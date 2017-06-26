package com.github.woshikid.utils;

/**
 * 
 * @author kid
 *
 */
public class SQLUtils {

	/**
	 * like sql for oracle for PreparedStatement
	 * @param value
	 * @return
	 */
	public static String likeOraclePrepared(Object value) {
		return String.valueOf(value).replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_").replace("％", "\\％");
	}
	
	/**
	 * like sql for oracle
	 * @param value
	 * @return
	 */
	private static String likeOracle(Object value) {
		return String.valueOf(value).replace("'", "''").replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_").replace("％", "\\％");
	}
	
	/**
	 * like sql for oracle
	 * @param value
	 * @return
	 */
	public static String likeOracleL(Object value) {
		return "'%" + likeOracle(value) + "' escape '\\'";
	}
	
	/**
	 * like sql for oracle
	 * @param value
	 * @return
	 */
	public static String likeOracleR(Object value) {
		return "'" + likeOracle(value) + "%' escape '\\'";
	}
	
	/**
	 * like sql for oracle
	 * @param value
	 * @return
	 */
	public static String likeOracleA(Object value) {
		return "'%" + likeOracle(value) + "%' escape '\\'";
	}
	
	/**
	 * like sql for mysql for PreparedStatement
	 * @param value
	 * @return
	 */
	public static String likeMySQLPrepared(Object value) {
		return String.valueOf(value).replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
	}
	
	/**
	 * like sql for mysql
	 * @param value
	 * @return
	 */
	private static String likeMySQL(Object value) {
		return String.valueOf(value).replace("'", "''").replace("\\", "\\\\\\\\").replace("%", "\\\\%").replace("_", "\\\\_");
	}
	
	/**
	 * like sql for mysql
	 * @param value
	 * @return
	 */
	public static String likeMySQLL(Object value) {
		return "'%" + likeMySQL(value) + "'";
	}
	
	/**
	 * like sql for mysql
	 * @param value
	 * @return
	 */
	public static String likeMySQLR(Object value) {
		return "'" + likeMySQL(value) + "%'";
	}
	
	/**
	 * like sql for mysql
	 * @param value
	 * @return
	 */
	public static String likeMySQLA(Object value) {
		return "'%" + likeMySQL(value) + "%'";
	}
	
	/**
	 * mysql escape
	 * @param value
	 * @return
	 */
	public static String mysql(Object value) {
		return String.valueOf(value).replace("\\", "\\\\");
	}
	
	/**
	 * sql escape
	 * @param value
	 * @return
	 */
	public static String sql(Object value) {
		return "'" + String.valueOf(value).replace("'", "''") + "'";
	}
	
	/**
	 * sql in (unsafe)
	 * @param value
	 * @return
	 */
	public static String in(Object value) {
		return "('" + String.valueOf(value).replace("'", "''").replace(",", "','") + "')";
	}
	
}
