package com.github.woshikid.utils;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Savepoint;
import java.sql.Types;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.sql.DataSource;

import org.apache.commons.lang3.StringUtils;

/**
 * 
 * @author kid
 *
 */
public class DBUtils {

	public static String jndi = null;
	private static int queryLimit = 1000;
	private static int queryTimeout = 30;
	private static final String colName = "thiscolumnisonlyusedforpaging_";
	public static boolean log = true;
	public static int cacheSize = 1000;
	public static boolean cacheEnable = true;
	private static int cacheLimit = 500;
	private static long cacheTime = 24 * 60 * 60 * 1000;
	private static Map<String, Map<String, Object>> cache = new LinkedHashMap<String, Map<String, Object>>(16, 0.75f, true);
	private static long lastClean = System.currentTimeMillis();
	
	private Connection conn = null;
	private int timeout = queryTimeout;
	
	public DBUtils() throws Exception {
		this(queryTimeout);
	}
	
	public DBUtils(int timeout) throws Exception {
		this(getConnection(), timeout);
	}
	
	public DBUtils(Connection conn) throws Exception {
		this(conn, queryTimeout);
	}
	
	public DBUtils(String jndi) throws Exception {
		this(jndi, queryTimeout);
	}
	
	public DBUtils(String jndi, int timeout) throws Exception {
		this(getConnection(jndi), timeout);
	}
	
	public DBUtils(String driver, String url, String user, String password) throws Exception {
		this(driver, url, user, password, queryTimeout);
	}
	
	public DBUtils(String driver, String url, String user, String password, int timeout) throws Exception {
		this(getConnection(driver, url, user, password), timeout);
	}
	
	public DBUtils(Connection conn, int timeout) throws Exception {
		this.timeout = timeout;
		this.conn = conn;
		conn.setAutoCommit(false);
	}
	
	public void commit() throws Exception {
		conn.commit();
	}
	
	public void rollback() throws Exception {
		conn.rollback();
	}
	
	public Savepoint setSavepoint() throws Exception {
		return conn.setSavepoint();
	}
	
	public void rollback(Savepoint sp) throws Exception {
		conn.rollback(sp);
	}
	
	public void close() {
		try{
			conn.close();
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	
	public static Connection getConnection() throws Exception {
		return getConnection(jndi);
	}
	
	public static Connection getConnection(String jndi) throws Exception {
		Context ctx = new InitialContext();
		DataSource ds = (DataSource)ctx.lookup(jndi);
		return ds.getConnection();
	}
	
	public static Connection getConnection(String driver, String url, String user, String password) throws Exception {
		Class.forName(driver);
		DriverManager.setLoginTimeout(queryTimeout);
		return DriverManager.getConnection(url, user, password);
	}
	
	private static List<Map<String, String>> resultSet2MapList(ResultSet rs) throws Exception {
		List<Map<String, String>> list = new ArrayList<Map<String, String>>();
		ResultSetMetaData rsmd = rs.getMetaData();
		while(rs.next()){
			if(list.size() >= queryLimit){
				rs.close();
				throw new Exception("too many results");
			}
			
			Map<String, String> map = new LinkedHashMap<String, String>();
			for(int i = 1;i <= rsmd.getColumnCount();i++){
				String name = rsmd.getColumnName(i).toLowerCase();
				if(name.equalsIgnoreCase(colName))continue;
				map.put(name, rs.getString(i));
			}
			list.add(map);
		}
		
		rs.close();
		return list;
	}
	
	private static List<Map<String, String>> query(String sql, Connection conn, int timeout, Object... param) throws Exception {
		try{
			long before = System.currentTimeMillis();
			PreparedStatement pstmt = conn.prepareStatement(sql);
			pstmt.setQueryTimeout(timeout);
			for(int i = 0;i < param.length;i++){
				pstmt.setObject(i + 1, param[i]);
			}
			ResultSet rs = pstmt.executeQuery();
			long after = System.currentTimeMillis();
			
			List<Map<String, String>> list = resultSet2MapList(rs);
			pstmt.close();
			
			if(log)System.out.println("DB: query time " + (after - before) + "ms, data count " + list.size() + ", sql: " + StringUtils.abbreviate(sql + param2String(param), 100));
			return list;
		}catch(Exception e){
			System.out.println(sql + param2String(param));
			throw e;
		}
	}
	
	public static List<Map<String, String>> query(String sql, Object... param) throws Exception {
		Connection conn = getConnection();
		try{
			return query(sql, conn, queryTimeout, param);
		}finally{
			conn.close();
		}
	}
	
	public List<Map<String, String>> transQuery(String sql, Object... param) throws Exception {
		return query(sql, conn, timeout, param);
	}
	
	public static List<Map<String, String>> pageOracle(int size, int page, String sql, Object... param) throws Exception {
		int start = size * (page - 1) + 1;
		int end = size * page;
		sql = "select * from (select rownum as " + colName + ", t.* from (" + sql + ") t) t where " + colName + " >= " + start + " and " + colName + " <= " + end;
		return query(sql, param);
	}
	
	public List<Map<String, String>> transPageOracle(int size, int page, String sql, Object... param) throws Exception {
		int start = size * (page - 1) + 1;
		int end = size * page;
		sql = "select * from (select rownum as " + colName + ", t.* from (" + sql + ") t) t where " + colName + " >= " + start + " and " + colName + " <= " + end;
		return transQuery(sql, param);
	}
	
	public static List<Map<String, String>> pageMySQL(int size, int page, String sql, Object... param) throws Exception {
		int start = size * (page - 1);
		sql = "select * from (" + sql + ") t limit " + start + "," + size;
		return query(sql, param);
	}
	
	public List<Map<String, String>> transPageMySQL(int size, int page, String sql, Object... param) throws Exception {
		int start = size * (page - 1);
		sql = "select * from (" + sql + ") t limit " + start + "," + size;
		return transQuery(sql, param);
	}
	
	public static long count(String sql, Object... param) throws Exception {
		sql = "select count(1) as counts from (" + sql + ") t";
		Map<String, String> map = query(sql, param).get(0);
		return Long.parseLong(map.get("counts"));
	}
	
	public long transCount(String sql, Object... param) throws Exception {
		sql = "select count(1) as counts from (" + sql + ") t";
		Map<String, String> map = transQuery(sql, param).get(0);
		return Long.parseLong(map.get("counts"));
	}
	
	private static int execute(String sql, Connection conn, int timeout, Object... param) throws Exception {
		try{
			long before = System.currentTimeMillis();
			PreparedStatement pstmt = conn.prepareStatement(sql);
			pstmt.setQueryTimeout(timeout);
			for(int i = 0;i < param.length;i++){
				pstmt.setObject(i + 1, param[i]);
			}
			int i = pstmt.executeUpdate();
			long after = System.currentTimeMillis();
			
			pstmt.close();
			
			if(log)System.out.println("DB: execute time " + (after - before) + "ms, sql: " + StringUtils.abbreviate(sql + param2String(param), 100));
			return i;
		}catch(Exception e){
			System.out.println(sql + param2String(param));
			throw e;
		}
	}
	
	public static int execute(String sql, Object... param) throws Exception {
		Connection conn = getConnection();
		try{
			return execute(sql, conn, queryTimeout, param);
		}finally{
			conn.close();
		}
	}
	
	public int transExecute(String sql, Object... param) throws Exception {
		return execute(sql, conn, timeout, param);
	}
	
	private static int update(String tableName, Map<String, Object> map, String where, Connection conn, int timeout, Object... param) throws Exception {
		if(map == null || map.isEmpty())throw new Exception("map is empty");
		Object[] newParam = new Object[map.size()];
		
		int i = 0;
		StringBuffer sql = new StringBuffer("update " + tableName + " set ");
		for(String key : map.keySet()){
			Object value = map.get(key);
			if(value instanceof StringBuffer){
				sql.append(key + "=").append((StringBuffer)value).append(",");
			}else{
				sql.append(key + "=?,");
				newParam[i++] = value;
			}
		}
		sql.deleteCharAt(sql.length() - 1).append(" where ").append(where);
		
		Object[] fullParam = new Object[i + param.length];
		System.arraycopy(newParam, 0, fullParam, 0, i);
		System.arraycopy(param, 0, fullParam, i, param.length);
		
		return execute(sql.toString(), conn, timeout, fullParam);
	}
	
	public static int update(String tableName, Map<String, Object> map, String where, Object... param) throws Exception {
		Connection conn = getConnection();
		try{
			return update(tableName, map, where, conn, queryTimeout, param);
		}finally{
			conn.close();
		}
	}
	
	public int transUpdate(String tableName, Map<String, Object> map, String where, Object... param) throws Exception {
		return update(tableName, map, where, conn, timeout, param);
	}
	
	private static int insert(String tableName, Map<String, Object> map, Connection conn, int timeout) throws Exception {
		if(map == null || map.isEmpty())throw new Exception("map is empty");
		Object[] newParam = new Object[map.size()];
		
		int i = 0;
		StringBuffer sql1 = new StringBuffer("insert into " + tableName + "(");
		StringBuffer sql2 = new StringBuffer(" values(");
		for(String key : map.keySet()){
			Object value = map.get(key);
			if(value instanceof StringBuffer){
				sql1.append(key + ",");
				sql2.append((StringBuffer)value).append(",");
			}else{
				sql1.append(key + ",");
				sql2.append("?,");
				newParam[i++] = value;
			}
		}
		sql1.deleteCharAt(sql1.length() - 1).append(")");
		sql2.deleteCharAt(sql2.length() - 1).append(")");
		
		Object[] fullParam = new Object[i];
		System.arraycopy(newParam, 0, fullParam, 0, i);
		
		return execute(sql1.append(sql2).toString(), conn, timeout, fullParam);
	}
	
	public static int insert(String tableName, Map<String, Object> map) throws Exception {
		Connection conn = getConnection();
		try{
			return insert(tableName, map, conn, queryTimeout);
		}finally{
			conn.close();
		}
	}
	
	public int transInsert(String tableName, Map<String, Object> map) throws Exception {
		return insert(tableName, map, conn, timeout);
	}
	
	private static Object[] call(String callName, Connection conn, int timeout, Object... param) throws Exception {
		try{
			callName = "{call " + callName + "(";
			for(int i = 0;i < param.length;i++){
				callName += "?,";
			}
			callName = callName.substring(0, callName.length() - 1);
			if(callName.endsWith("?")){
				callName += ")}";
			}else{
				callName += "}";
			}
			
			long before = System.currentTimeMillis();
			CallableStatement cstmt = conn.prepareCall(callName);
			cstmt.setQueryTimeout(timeout);
			for(int i = 0;i < param.length;i++){
				if(param[i] == null){
					cstmt.registerOutParameter(i + 1, Types.VARCHAR);
				}else{
					cstmt.setObject(i + 1, param[i]);
				}
			}
			cstmt.execute();
			long after = System.currentTimeMillis();
			
			for(int i = 0;i < param.length;i++){
				if(param[i] == null){
					param[i] = cstmt.getString(i + 1);
				}
			}
			cstmt.close();
			
			if(log)System.out.println("DB: call time " + (after - before) + "ms, sql: " + StringUtils.abbreviate(callName + param2String(param), 100));
			return param;
		}catch(Exception e){
			System.out.println(callName + param2String(param));
			throw e;
		}
	}
	
	public static Object[] call(String callName, Object... param) throws Exception {
		Connection conn = getConnection();
		try{
			return call(callName, conn, queryTimeout, param);
		}finally{
			conn.close();
		}
	}
	
	public Object[] transCall(String callName, Object... param) throws Exception {
		return call(callName, conn, timeout, param);
	}
	
	public synchronized static void cleanCache() {
		lastClean = System.currentTimeMillis();
		Iterator<Map.Entry<String, Map<String, Object>>> it = cache.entrySet().iterator();
		while(it.hasNext()){
			Map.Entry<String, Map<String, Object>> entry = it.next();
			Map<String, Object> cacheUnit = entry.getValue();
			long create = (Long)cacheUnit.get("create");
			long passed = System.currentTimeMillis() - create;
			if(passed >= cacheTime)it.remove();
		}
	}
	
	//param only support String Integer Long Float Double Date Timestamp
	@SuppressWarnings("unchecked")
	public static List<Map<String, String>> query(int fresh, String sql, Object... param) throws Exception {
		if(!cacheEnable)return query(sql, param);
		StringBuffer index = new StringBuffer(MessageDigestUtils.md5(sql));
		for(Object o : param){
			index.append(MessageDigestUtils.md5(String.valueOf(o)));
		}
		String key = MessageDigestUtils.md5(index.toString());
		
		Map<String, Object> cacheUnit;
		synchronized(DBUtils.class){
			cacheUnit = cache.get(key);
		}
		
		if(cacheUnit != null){
			long create = (Long)cacheUnit.get("create");
			long passed = System.currentTimeMillis() - create;
			if(passed < cacheTime && passed < fresh * 1000){
				if(log)System.out.println("DB: cache hit! cache size " + cache.size() + ", sql: " + StringUtils.abbreviate(sql + param2String(param), 100));
				return (List<Map<String, String>>)ObjectUtils.clone(cacheUnit.get("data"));
			}
		}
		
		List<Map<String, String>> data = query(sql, param);
		if(data.size() <= cacheLimit){
			synchronized(DBUtils.class){
				if(cacheUnit == null){
					cacheUnit = new HashMap<String, Object>();
					cache.put(key, cacheUnit);
				}
				cacheUnit.put("create", System.currentTimeMillis());
				cacheUnit.put("data", ObjectUtils.clone(data));
			}
		}
		
		if(cache.size() > cacheSize){
			synchronized(DBUtils.class){
				Iterator<Map.Entry<String, Map<String, Object>>> it = cache.entrySet().iterator();
				while(it.hasNext() && (cache.size() > (cacheSize * 0.9))){
					it.next();
					it.remove();
				}
			}
		}
		
		if((System.currentTimeMillis() - lastClean) > 3600000)cleanCache();
		return data;
	}
	
	private static String param2String(Object... param) {
		StringBuffer buff = new StringBuffer(" [");
		for(Object o : param){
			buff.append(String.valueOf(o)).append(", ");
		}
		buff.append("]");
		return buff.toString();
	}

}
