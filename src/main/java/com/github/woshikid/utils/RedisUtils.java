package com.github.woshikid.utils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

import redis.clients.jedis.BinaryClient;
import redis.clients.jedis.BitOP;
import redis.clients.jedis.BitPosParams;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisCluster;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.JedisSentinelPool;
import redis.clients.jedis.ScanParams;
import redis.clients.jedis.ScanResult;
import redis.clients.jedis.SortingParams;
import redis.clients.jedis.Tuple;
import redis.clients.jedis.ZParams;
import redis.clients.jedis.ZParams.Aggregate;
import redis.clients.jedis.params.sortedset.ZAddParams;
import redis.clients.jedis.params.sortedset.ZIncrByParams;
import redis.clients.util.JedisClusterCRC16;

/**
 * Redis工具类
 * 配置文件为/redis.properties
 * enable=true
 * pool.maxTotal=10
 * pool.maxIdle=10
 * pool.maxWaitMillis=10000
 * pool.testOnBorrow=true
 * #cluster=127.0.0.1:7000,127.0.0.1:7001,127.0.0.1:7002
 * #maxRedirections=10
 * #sentinels=127.0.0.1:26379,127.0.0.1:36379,127.0.0.1:46379
 * #masterName=redis_master
 * host=127.0.0.1
 * port=6379
 * timeout=2000
 * database=0
 * password=
 * 
 * 
 * 未实现的命令：
 * AUTH
 * ECHO
 * QUIT
 * SELECT
 * MIGRATE
 * OBJECT(但增加idle方法)
 * WAIT
 * BITFIELD
 * HSTRLEN
 * FLUSHALL
 * FLUSHDB
 * INFO
 * ROLE
 * SAVE
 * SHUTDOWN
 * SLAVEOF
 * CLIENT控制相关命令
 * COMMAND相关命令
 * CONFIG相关命令
 * DEBUG相关命令
 * CLUSTER相关命令
 * SCRIPT相关命令
 * GEO相关命令
 * PUBSUB相关命令
 * 事务管道相关命令(通过getJedis自己实现)
 * 
 * @author kid
 *
 */
public class RedisUtils {

	private static JedisCluster jedisCluster = null;
	
	private static JedisSentinelPool jedisSentinelPool = null;
	
	private static JedisPool jedisPool = null;
	
	static {
		Properties config = new Properties();
		
		try (InputStream in = RedisUtils.class.getResourceAsStream("/redis.properties")) {
			config.load(in);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		
		String enable = config.getProperty("enable");
		if ("true".equals(enable)) {
			//连接池设置
			int maxTotal = Integer.parseInt(config.getProperty("pool.maxTotal"));
			int maxIdle = Integer.parseInt(config.getProperty("pool.maxIdle"));
			int maxWaitMillis = Integer.parseInt(config.getProperty("pool.maxWaitMillis"));
			boolean testOnBorrow = Boolean.parseBoolean(config.getProperty("pool.testOnBorrow"));
			
			//集群设置
			String cluster = config.getProperty("cluster");
			String maxRedirections = config.getProperty("maxRedirections");
			
			//哨兵设置
			String sentinels = config.getProperty("sentinels");
			String masterName = config.getProperty("masterName");
			
			//单机设置
			String host = config.getProperty("host");
			String port = config.getProperty("port");
			
			//客户端设置
			int timeout = Integer.parseInt(config.getProperty("timeout"));
			int database = Integer.parseInt(config.getProperty("database"));
			String password = config.getProperty("password");
			if ("".equals(password)) password = null;
			
			JedisPoolConfig poolConfig = new JedisPoolConfig();
			poolConfig.setMaxTotal(maxTotal);
			poolConfig.setMaxIdle(maxIdle);
			poolConfig.setMaxWaitMillis(maxWaitMillis);
			poolConfig.setTestOnBorrow(testOnBorrow);
			
			if (cluster != null) {
				int intMaxRedirections = Integer.parseInt(maxRedirections);
				
				Set<HostAndPort> clusterNodes = new HashSet<>();
				for (String node : cluster.split(",")) {
					String nodeHost = node.split(":")[0];
					int nodePort = Integer.parseInt(node.split(":")[1]);
					clusterNodes.add(new HostAndPort(nodeHost, nodePort));
				}
				
				jedisCluster = new JedisCluster(clusterNodes, timeout, intMaxRedirections, poolConfig);
			} else if (sentinels != null) {
				Set<String> sentinelSet = new HashSet<>();
				for (String sentinel : sentinels.split(",")) {
					sentinelSet.add(sentinel);
				}
				
				jedisSentinelPool = new JedisSentinelPool(masterName, sentinelSet, poolConfig, timeout, password, database);
			} else if (host != null) {
				int intPort = Integer.parseInt(port);
				
				jedisPool = new JedisPool(poolConfig, host, intPort, timeout, password, database);
			}
		}
	}
	
	/**
	 * 得到key对应集群中的slot
	 * @param key
	 * @return
	 */
	public static int getSlot(Object key) {
		return JedisClusterCRC16.getSlot(toBytes(key));
	}
	
	/**
	 * 得到JedisCluster实例来进行redis操作
	 * 内部自带连接池，再也不用finally去close了
	 * 直接调用对应的方法即可
	 * @return
	 */
	public static JedisCluster getJedisCluster() {
		if (jedisCluster != null) {
			return jedisCluster;
		} else {
			throw new RuntimeException("jedis cluster not ready");
		}
	}
	
	/**
	 * 得到Jedis实例用来执行watch,multi,pipelined等未实现的命令
	 * 用完一定记得用
	 * finally {
	 *     jedis.close();
	 * }
	 * 将实例返回连接池
	 * @return
	 */
	public static Jedis getJedis() {
		if (jedisSentinelPool != null) {
			return jedisSentinelPool.getResource();
		} else if (jedisPool != null) {
			return jedisPool.getResource();
		} else {
			throw new RuntimeException("jedis not ready");
		}
	}
	
	/**
	 * 将对象转成字节数组保存
	 * redis值不能为null
	 * @param object
	 * @return
	 */
	private static byte[] toBytes(Object object) {
		if (object == null) return null;
		
		if (object instanceof String) {
			return ((String)object).getBytes(StandardCharsets.UTF_8);
		} else if (object instanceof byte[]) {
			return (byte[])object;
		} else if (object instanceof Boolean) {
			return object.toString().getBytes(StandardCharsets.UTF_8);
		} else if (object instanceof Number) {
			return object.toString().getBytes(StandardCharsets.UTF_8);
		} else {
			return ObjectUtils.serialize(object);
		}
	}
	
	/**
	 * 将多个对象转换成字节数组
	 * @param objects
	 * @return
	 */
	private static byte[][] toBytes(Object... objects) {
		if (objects == null) return null;
		
		byte[][] bytes = new byte[objects.length][];
		for (int i = 0; i < objects.length; i++) {
			bytes[i] = toBytes(objects[i]);
		}
		return bytes;
	}
	
	/**
	 * 将Map内容转换为字节数组
	 * @param map
	 * @return
	 */
	private static Map<byte[], byte[]> toBytes(Map<?, ?> map) {
		if (map == null) return null;
		
		Map<byte[], byte[]> byteMap = new HashMap<>(map.size());
		for (Map.Entry<?, ?> entry : map.entrySet()) {
			byteMap.put(toBytes(entry.getKey()), toBytes(entry.getValue()));
		}
		return byteMap;
	}
	
	/**
	 * 将Map内容转换为字节数组
	 * @param map
	 * @return
	 */
	private static Map<byte[], Double> toBytesZ(Map<?, Double> map) {
		if (map == null) return null;
		
		Map<byte[], Double> byteMap = new HashMap<>(map.size());
		for (Map.Entry<?, Double> entry : map.entrySet()) {
			byteMap.put(toBytes(entry.getKey()), entry.getValue());
		}
		return byteMap;
	}
	
	/**
	 * 将字节数组转成字符串返回
	 * @param bytes
	 * @return
	 */
	private static String toString(byte[] bytes) {
		if (bytes == null) return null;
		return new String(bytes, StandardCharsets.UTF_8);
	}
	
	/**
	 * 将字节数组的数组转成字符串数组返回
	 * @param byteList
	 * @return
	 */
	private static List<String> toString(List<byte[]> byteList) {
		if (byteList == null) return null;
		
		return byteList.parallelStream().map(bytes -> toString(bytes)).collect(Collectors.toList());
	}
	
	/**
	 * 将字节数组的集合转成字符串集合返回
	 * @param byteSet
	 * @return
	 */
	private static Set<String> toString(Set<byte[]> byteSet) {
		if (byteSet == null) return null;
		
		return byteSet.parallelStream().map(bytes -> toString(bytes)).collect(Collectors.toSet());
	}
	
	/**
	 * 将字节数组的Entry转换成字符串的Entry返回
	 * @param byteEntry
	 * @return
	 */
	private static Map.Entry<String, String> toString(Map.Entry<byte[], byte[]> byteEntry) {
		if (byteEntry == null) return null;
		return new AbstractMap.SimpleEntry<>(toString(byteEntry.getKey()), toString(byteEntry.getValue()));
	}
	
	/**
	 * 将字节数组的Map转成字符串Map返回
	 * @param byteMap
	 * @return
	 */
	private static Map<String, String> toString(Map<byte[], byte[]> byteMap) {
		if (byteMap == null) return null;
		
		Map<String, String> map = new HashMap<>(byteMap.size());
		for (Map.Entry<byte[], byte[]> entry : byteMap.entrySet()) {
			map.put(toString(entry.getKey()), toString(entry.getValue()));
		}
		return map;
	}
	
	/**
	 * 将字节数组的迭代信息转成字符串迭代信息返回
	 * @param byteResult
	 * @return
	 */
	private static ScanResult<String> toString(ScanResult<byte[]> byteResult) {
		if (byteResult == null) return null;
		
		String cursor = toString(byteResult.getCursorAsBytes());
		List<String> results = toString(byteResult.getResult());
		return new ScanResult<>(cursor, results);
	}
	
	/**
	 * 将字节数组的Map迭代信息转成字符串Map迭代信息返回
	 * @param byteResult
	 * @return
	 */
	private static ScanResult<Map.Entry<String, String>> toStringH(ScanResult<Map.Entry<byte[], byte[]>> byteResult) {
		if (byteResult == null) return null;
		
		String cursor = toString(byteResult.getCursorAsBytes());
		List<Map.Entry<byte[], byte[]>> byteList = byteResult.getResult();
		
		List<Map.Entry<String, String>> list = new ArrayList<>(byteList.size());
		for (Map.Entry<byte[], byte[]> bytes : byteList) {
			list.add(toString(bytes));
		}
		
		return new ScanResult<>(cursor, list);
	}
	
	/**
	 * 将位置字符串转换成数组位置枚举
	 * @param where before|after
	 * @return
	 */
	private static BinaryClient.LIST_POSITION toListPosition(String where) {
		if ("BEFORE".equalsIgnoreCase(where)) {
			return BinaryClient.LIST_POSITION.BEFORE;
		} else if ("AFTER".equalsIgnoreCase(where)) {
			return BinaryClient.LIST_POSITION.AFTER;
		} else {
			throw new IllegalArgumentException("unknow position:" + where);
		}
	}
	
	/**
	 * 得到迭代参数
	 * @param match
	 * @param count
	 * @return
	 */
	private static ScanParams toScanParams(String match, Integer count) {
		ScanParams params = new ScanParams();
		if (match != null) params.match(match);
		if (count != null) params.count(count);
		return params;
	}
	
	/**
	 * 得到排序参数
	 * @param by
	 * @param ascdesc
	 * @param alpha
	 * @param offset
	 * @param count
	 * @param get
	 * @return
	 */
	private static SortingParams toSortingParams(String by, String ascdesc, String alpha, Integer offset, Integer count, String[] get) {
		SortingParams params = new SortingParams();
		if (by != null) params.by(by);
		if ("asc".equalsIgnoreCase(ascdesc)) params.asc();
		if ("desc".equalsIgnoreCase(ascdesc)) params.desc();
		if ("alpha".equalsIgnoreCase(alpha)) params.alpha();
		if (offset != null && count != null) params.limit(offset, count);
		if (get != null) params.get(get);
		return params;
	}
	
	/**
	 * 得到比特查找参数
	 * @param start
	 * @param end
	 * @return
	 */
	private static BitPosParams toBitPosParams(long start, Long end) {
		if (end == null) {
			return new BitPosParams(start);
		} else {
			return new BitPosParams(start, end);
		}
	}
	
	/**
	 * 将比特操作字符串转换成比特操作枚举
	 * @param bitop
	 * @return
	 */
	private static BitOP toBitOP(String bitop) {
		if ("AND".equalsIgnoreCase(bitop)) {
			return BitOP.AND;
		} else if ("OR".equalsIgnoreCase(bitop)) {
			return BitOP.OR;
		} else if ("XOR".equalsIgnoreCase(bitop)) {
			return BitOP.XOR;
		} else if ("NOT".equalsIgnoreCase(bitop)) {
			return BitOP.NOT;
		} else {
			throw new IllegalArgumentException("unknow bitop:" + bitop);
		}
	}
	
	/**
	 * 得到zset插入参数
	 * @param nxxx
	 * @param ch
	 * @return
	 */
	private static ZAddParams toZAddParams(String nxxx, String ch) {
		ZAddParams params = ZAddParams.zAddParams();
		if ("nx".equalsIgnoreCase(nxxx)) params.nx();
		if ("xx".equalsIgnoreCase(nxxx)) params.xx();
		if ("ch".equalsIgnoreCase(ch)) params.ch();
		return params;
	}
	
	/**
	 * 得到zset自增参数
	 * @param nxxx
	 * @return
	 */
	private static ZIncrByParams toZIncrByParams(String nxxx) {
		ZIncrByParams params = ZIncrByParams.zIncrByParams();
		if ("nx".equalsIgnoreCase(nxxx)) params.nx();
		if ("xx".equalsIgnoreCase(nxxx)) params.xx();
		return params;
	}
	
	/**
	 * 得到zset交并集参数
	 * @param weights
	 * @param aggregate
	 * @return
	 */
	private static ZParams toZParams(double[] weights, String aggregate) {
		ZParams params = new ZParams();
		if (weights != null) params.weightsByDouble(weights);
		if ("SUM".equalsIgnoreCase(aggregate)) params.aggregate(Aggregate.SUM);
		if ("MIN".equalsIgnoreCase(aggregate)) params.aggregate(Aggregate.MIN);
		if ("MAX".equalsIgnoreCase(aggregate)) params.aggregate(Aggregate.MAX);
		return params;
	}
	
	/********** 通用命令 **********/
	
	/**
	 * 测试redis连通性
	 * @return 网络延时(纳秒)
	 */
	public static long ping() {
		try (Jedis jedis = getJedis()) {
			long now = System.nanoTime();
			jedis.ping();
			return System.nanoTime() - now;
		}
	}
	
	/**
	 * 得到服务器时间
	 * [0]时间戳(秒)
	 * [1]微秒
	 * @return
	 */
	public static List<String> time() {
		try (Jedis jedis = getJedis()) {
			return jedis.time();
		}
	}
	
	/**
	 * 得到当前库里key的数量
	 * @return
	 */
	public static long dbSize() {
		try (Jedis jedis = getJedis()) {
			return jedis.dbSize();
		}
	}
	
	/**
	 * 得到服务器上次成功保存的时间戳(秒)
	 * @return
	 */
	public static long lastsave() {
		try (Jedis jedis = getJedis()) {
			return jedis.lastsave();
		}
	}
	
	/**
	 * 启动异步aof文件重写任务
	 */
	public static void bgrewriteaof() {
		try (Jedis jedis = getJedis()) {
			jedis.bgrewriteaof();
		}
	}
	
	/**
	 * 启动异步文件备份任务
	 */
	public static void bgsave() {
		try (Jedis jedis = getJedis()) {
			jedis.bgsave();
		}
	}
	
	/**
	 * 删除指定的key
	 * @param key
	 * @return 成功返回true，key不存在返回false
	 */
	public static boolean del(Object key) {
		try (Jedis jedis = getJedis()) {
			return jedis.del(toBytes(key)) == 1L;
		}
	}
	
	/**
	 * 删除指定的多个key
	 * @param keys
	 * @return 被实际删除的key个数
	 */
	public static long del(Object... keys) {
		try (Jedis jedis = getJedis()) {
			return jedis.del(toBytes(keys));
		}
	}
	
	/**
	 * 检查某个key是否在缓存中存在，如果存在返回true，否则返回false
	 * @param key
	 * @return
	 */
	public static boolean exists(Object key) {
		try (Jedis jedis = getJedis()) {
			return jedis.exists(toBytes(key));
		}
	}
	
	/**
	 * 检查多个key是否在缓存中存在，返回存在的key的总个数
	 * 如果一个key在参数中重复多次，则会被统计多次
	 * @param keys
	 * @return
	 */
	public static long exists(Object... keys) {
		try (Jedis jedis = getJedis()) {
			return jedis.exists(toBytes(keys));
		}
	}
	
	/**
	 * 为key设置一个特定的过期时间，单位为秒。
	 * 过期时间一到，redis将会从缓存中删除掉该key。
	 * 即使是有过期时间的key，redis也会在持久化时将其写到硬盘中
	 * 并把相对过期时间改为绝对的Unix过期时间。
	 * set之类完全替换值内容的操作会清除过期时间
	 * incr, lpush等修改值内容的操作将不会影响过期时间
	 * @param key
	 * @param seconds
	 * @return
	 */
	public static boolean expire(Object key, int seconds) {
		try (Jedis jedis = getJedis()) {
			return jedis.expire(toBytes(key), seconds) == 1L;
		}
	}
	
	/**
	 * 与{@link #expire(Object, int)}不一样，expireAt设置的时间不是能存活多久
	 * 而是固定的UNIX时间（从1970年开始算起），<b>单位为秒</b>
	 * @param key
	 * @param unixTime
	 * @return
	 */
	public static boolean expireAt(Object key, long unixTime) {
		try (Jedis jedis = getJedis()) {
			return jedis.expireAt(toBytes(key), unixTime) == 1L;
		}
	}
	
	/**
	 * 机制同{@link #expire(Object, int)}一样，只是时间单位改为毫秒。
	 * @param key
	 * @param milliseconds
	 * @return
	 */
	public static boolean pexpire(Object key, long milliseconds) {
		try (Jedis jedis = getJedis()) {
			return jedis.pexpire(toBytes(key), milliseconds) == 1L;
		}
	}
	
	/**
	 * 同{@link #expireAt(Object, long)}机制相同，但单位为毫秒。
	 * @param key
	 * @param millisecondsTimestamp
	 * @return
	 */
	public static boolean pexpireAt(Object key, long millisecondsTimestamp) {
		try (Jedis jedis = getJedis()) {
			return jedis.pexpireAt(toBytes(key), millisecondsTimestamp) == 1L;
		}
	}
	
	/**
	 * 返回一个key还能活多久，单位为秒
	 * 如果该key本来并没有设置过期时间，则返回-1，如果该key不存在，则返回-2
	 * @param key
	 * @return
	 */
	public static long ttl(Object key) {
		try (Jedis jedis = getJedis()) {
			return jedis.ttl(toBytes(key));
		}
	}
	
	/**
	 * 返回一个key还能活多久，单位为毫秒
	 * @param key
	 * @return
	 */
	public static long pttl(Object key) {
		try (Jedis jedis = getJedis()) {
			return jedis.pttl(toBytes(key));
		}
	}
	
	/**
	 * 如果一个key设置了过期时间，则取消其过期时间，使其永久存在。
	 * @param key
	 * @return 取消过期时间成功与否(不存在或原来没有过期)
	 */
	public static boolean persist(Object key) {
		try (Jedis jedis = getJedis()) {
			return jedis.persist(toBytes(key)) == 1L;
		}
	}
	
	/**
	 * 得到一个key空闲的秒数(没有读也没有写)
	 * 目前空闲时间的精度为10秒
	 * 如果key不存在则返回null
	 * @param key
	 * @return
	 */
	public static Long idle(Object key) {
		try (Jedis jedis = getJedis()) {
			return jedis.objectIdletime(toBytes(key));
		}
	}
	
	/**
	 * 返回某个key所存储的数据类型，返回的数据类型有可能是:
	 * "none", "string", "list", "set", "zset", "hash"
	 * "none"代表key不存在
	 * @param key
	 * @return
	 */
	public static String type(Object key) {
		try (Jedis jedis = getJedis()) {
			return jedis.type(toBytes(key));
		}
	}
	
	/**
	 * 将指定的key转移到另一个库
	 * 如果另一个库中key已存在则不做操作
	 * 该命令通常用作原生的锁实现
	 * @param key
	 * @param dbIndex
	 * @return 是否转移成功
	 */
	public static boolean move(Object key, int dbIndex) {
		try (Jedis jedis = getJedis()) {
			return jedis.move(toBytes(key), dbIndex) == 1L;
		}
	}
	
	/**
	 * 将一个key更名为另一个key
	 * 如果原key不存在则抛异常
	 * 如果目标key已存在则覆盖
	 * @param oldkey
	 * @param newkey
	 */
	public static void rename(Object oldkey, Object newkey) {
		try (Jedis jedis = getJedis()) {
			jedis.rename(toBytes(oldkey), toBytes(newkey));
		}
	}
	
	/**
	 * 仅当目标key不存在时，将一个key更名
	 * 如果原key不存在则抛异常
	 * 如果目标key已存在不做操作
	 * @param oldkey
	 * @param newkey
	 * @return 是否更名成功
	 */
	public static boolean renamenx(Object oldkey, Object newkey) {
		try (Jedis jedis = getJedis()) {
			return jedis.renamenx(toBytes(oldkey), toBytes(newkey)) == 1L;
		}
	}
	
	/**
	 * dump出指定key对应的value
	 * 以RDB格式序列化
	 * @param key
	 * @return
	 */
	public static byte[] dump(Object key) {
		try (Jedis jedis = getJedis()) {
			return jedis.dump(toBytes(key));
		}
	}
	
	/**
	 * 将序列化的值还原回指定的key
	 * @param key
	 * @param ttl 毫秒的过期时间，为0表示永不过期
	 * @param serializedValue
	 */
	public static void restore(Object key, int ttl, byte[] serializedValue) {
		try (Jedis jedis = getJedis()) {
			jedis.restore(toBytes(key), ttl, serializedValue);
		}
	}
	
	/**
	 * 得到库中随机的key
	 * 库为空则返回null
	 * @return
	 */
	public static byte[] randomKeyBytes() {
		try (Jedis jedis = getJedis()) {
			return jedis.randomBinaryKey();
		}
	}
	
	/**
	 * 得到库中随机的key
	 * 库为空则返回null
	 * @return
	 */
	public static String randomKey() {
		return toString(randomKeyBytes());
	}
	
	/**
	 * 同{@link #keys(String)}原理
	 * @param pattern
	 * @return
	 */
	public static Set<byte[]> keysBytes(String pattern) {
		try (Jedis jedis = getJedis()) {
			return jedis.keys(toBytes(pattern));
		}
	}
	
	/**
	 * 得到库中所有的key
	 * 不要在生产环境执行此命令
	 * @param pattern 支持*?[ae][a-e][^e]转义为\
	 * @return
	 */
	public static Set<String> keys(String pattern) {
		return toString(keysBytes(pattern));
	}
	
	/**
	 * 同{@link #scan(String)}原理
	 * @param cursor
	 * @return
	 */
	public static ScanResult<byte[]> scanBytes(String cursor) {
		try (Jedis jedis = getJedis()) {
			return jedis.scan(toBytes(cursor));
		}
	}
	
	/**
	 * 迭代得到库中所有的key
	 * 保证在完整迭代周期内始终存在的元素一定会返回
	 * 保证在完整迭代周期内始终不存在的元素一定不会返回
	 * 其他情况的元素返回性不能保证，有可能不返回，有可能重复返回多次
	 * 一次返回的元素个数不定，有可能为0
	 * @param cursor 游标以0开始，以0结束
	 * @return
	 */
	public static ScanResult<String> scan(String cursor) {
		return toString(scanBytes(cursor));
	}
	
	/**
	 * 同{@link #scan(String, String, Integer)}原理
	 * @param cursor
	 * @param match
	 * @param count
	 * @return
	 */
	public static ScanResult<byte[]> scanBytes(String cursor, String match, Integer count) {
		try (Jedis jedis = getJedis()) {
			return jedis.scan(toBytes(cursor), toScanParams(match, count));
		}
	}
	
	/**
	 * 迭代得到库中所有的key
	 * 保证在完整迭代周期内始终存在的元素一定会返回
	 * 保证在完整迭代周期内始终不存在的元素一定不会返回
	 * 其他情况的元素返回性不能保证，有可能不返回，有可能重复返回多次
	 * 一次返回的元素个数不定，有可能为0
	 * @param cursor 游标以0开始，以0结束
	 * @param match 支持*?[ae][a-e][^e]转义为\
	 * @param count 希望一次返回的元素数量，但并不保证
	 * @return
	 */
	public static ScanResult<String> scan(String cursor, String match, Integer count) {
		return toString(scanBytes(cursor, match, count));
	}
	
	/**
	 * 同{@link #sort(Object)}原理
	 * @param key
	 * @return
	 */
	public static List<byte[]> sortBytes(Object key) {
		try (Jedis jedis = getJedis()) {
			return jedis.sort(toBytes(key));
		}
	}
	
	/**
	 * 返回list，set，zset排序之后的元素列表
	 * 默认是把元素值转换成double进行排序
	 * 如果元素不能转换成double则抛出异常
	 * @param key
	 * @return
	 */
	public static List<String> sort(Object key) {
		return toString(sortBytes(key));
	}
	
	/**
	 * 将list，set，zset排序之后插入新的list
	 * 默认是把元素值转换成double进行排序
	 * 如果元素不能转换成double则抛出异常
	 * 如果dstkey已存在，则覆盖
	 * @param key
	 * @param dstkey
	 * @return 插入的元素数量
	 */
	public static long sort(Object key, Object dstkey) {
		try (Jedis jedis = getJedis()) {
			return jedis.sort(toBytes(key), toBytes(dstkey));
		}
	}
	
	/**
	 * 同{@link #sort(Object, String, String, String, Integer, Integer, String...)}原理
	 * @param key
	 * @param by
	 * @param ascdesc
	 * @param alpha
	 * @param offset
	 * @param count
	 * @param get
	 * @return
	 */
	public static List<byte[]> sortBytes(Object key, String by, String ascdesc, String alpha, Integer offset, Integer count, String... get) {
		try (Jedis jedis = getJedis()) {
			return jedis.sort(toBytes(key), toSortingParams(by, ascdesc, alpha, offset, count, get));
		}
	}
	
	/**
	 * 返回list，set，zset排序之后的元素列表
	 * @param key
	 * @param by 引用外部权重排序：weight_*
	 * @param ascdesc 只能取值asc|desc|null表示排序方式
	 * @param alpha 只能取值alpha|null表示是否按照字典排序
	 * @param offset 类似SELECT LIMIT offset, count in SQL
	 * @param count 类似SELECT LIMIT offset, count in SQL
	 * @param get 获取外部值：GET object_* GET #
	 * @return
	 */
	public static List<String> sort(Object key, String by, String ascdesc, String alpha, Integer offset, Integer count, String... get) {
		return toString(sortBytes(key, by, ascdesc, alpha, offset, count, get));
	}
	
	/**
	 * 将list，set，zset排序之后插入新的list
	 * 如果dstkey已存在，则覆盖
	 * @param key
	 * @param dstkey
	 * @param by 引用外部权重排序：weight_*
	 * @param ascdesc 只能取值asc|desc|null表示排序方式
	 * @param alpha 只能取值alpha|null表示是否按照字典排序
	 * @param offset 类似SELECT LIMIT offset, count in SQL
	 * @param count 类似SELECT LIMIT offset, count in SQL
	 * @param get 获取外部值：GET object_* GET #
	 * @return 插入的元素数量
	 */
	public static long sort(Object key, Object dstkey, String by, String ascdesc, String alpha, Integer offset, Integer count, String... get) {
		try (Jedis jedis = getJedis()) {
			return jedis.sort(toBytes(key), toSortingParams(by, ascdesc, alpha, offset, count, get), toBytes(dstkey));
		}
	}
	
	/********** string命令 **********/
	
	/**
	 * 存储数据到缓存中，若key已存在则覆盖，value的长度不能超过512MB
	 * 设置成功之后，原有的过期时间不再有效
	 * @param key
	 * @param value
	 * @return 操作结果
	 */
	public static boolean set(Object key, Object value) {
		try (Jedis jedis = getJedis()) {
			return jedis.set(toBytes(key), toBytes(value)) != null;
		}
	}
	
	/**
	 * 存储数据到缓存中，并决定当Key存在时是否覆盖
	 * 设置成功之后，原有的过期时间不再有效
	 * @param key
	 * @param value
	 * @param nxxx 只能取NX或者XX，如果取NX，则只有当key不存在时才进行set，如果取XX，则只有当key已经存在时才进行set
	 * @return
	 */
	public static boolean set(Object key, Object value, String nxxx) {
		try (Jedis jedis = getJedis()) {
			return jedis.set(toBytes(key), toBytes(value), toBytes(nxxx)) != null;
		}
	}
	
	/**
	 * 存储数据到缓存中，并制定过期时间和当Key存在时是否覆盖。
	 * @param key
	 * @param value
	 * @param nxxx 只能取NX或者XX，如果取NX，则只有当key不存在时才进行set，如果取XX，则只有当key已经存在时才进行set
	 * @param expx 值只能取EX或者PX，代表数据过期时间的单位，EX代表秒，PX代表毫秒
	 * @param time 过期时间，单位是expx所代表的单位
	 * @return
	 */
	public static boolean set(Object key, Object value, String nxxx, String expx, long time) {
		try (Jedis jedis = getJedis()) {
			return jedis.set(toBytes(key), toBytes(value), toBytes(nxxx), toBytes(expx), time) != null;
		}
	}
	
	/**
	 * 从缓存中根据key取得其值，如果key不存在则返回null
	 * @param key
	 * @return
	 */
	public static byte[] getBytes(Object key) {
		try (Jedis jedis = getJedis()) {
			return jedis.get(toBytes(key));
		}
	}
	
	/**
	 * 从缓存中根据key取得其值，如果key不存在则返回null
	 * @param key
	 * @return
	 */
	public static String get(Object key) {
		return toString(getBytes(key));
	}
	
	/**
	 * 一次性设置多个key的值
	 * @param keysvalues
	 */
	public static void mset(Object... keysvalues) {
		try (Jedis jedis = getJedis()) {
			jedis.mset(toBytes(keysvalues));
		}
	}
	
	/**
	 * 当所有key都不存在时
	 * 一次性设置多个key的值
	 * 如果有一个key存在，则全部不操作
	 * @param keysvalues
	 * @return 是否进行了set操作
	 */
	public static boolean msetnx(Object... keysvalues) {
		try (Jedis jedis = getJedis()) {
			return jedis.msetnx(toBytes(keysvalues)) == 1L;
		}
	}
	
	/**
	 * 一次性取得多个key的值
	 * @param keys
	 * @return
	 */
	public static List<byte[]> mgetBytes(Object... keys) {
		try (Jedis jedis = getJedis()) {
			return jedis.mget(toBytes(keys));
		}
	}
	
	/**
	 * 一次性取得多个key的值
	 * @param keys
	 * @return
	 */
	public static List<String> mget(Object... keys) {
		return toString(mgetBytes(keys));
	}
	
	/**
	 * 仅当key不存在时才设置value值
	 * 如果key已经存在，则不做修改
	 * @param key
	 * @param value
	 * @return 是否设置成功
	 */
	public static boolean setnx(Object key, Object value) {
		try (Jedis jedis = getJedis()) {
			return jedis.setnx(toBytes(key), toBytes(value)) == 1L;
		}
	}
	
	/**
	 * 对key设置值并同时设置超时时间，单位秒
	 * @param key
	 * @param seconds
	 * @param value
	 */
	public static void setex(Object key, int seconds, Object value) {
		try (Jedis jedis = getJedis()) {
			jedis.setex(toBytes(key), seconds, toBytes(value));
		}
	}
	
	/**
	 * 对key设置值并同时设置超时时间，单位毫秒
	 * @param key
	 * @param milliseconds
	 * @param value
	 */
	public static void psetex(Object key, long milliseconds, Object value) {
		try (Jedis jedis = getJedis()) {
			jedis.psetex(toBytes(key), milliseconds, toBytes(value));
		}
	}
	
	/**
	 * 对key的值设置为value并返回原来的值
	 * 如果key存在但是对应的类型不是string则抛异常
	 * @param key
	 * @param value
	 * @return
	 */
	public static byte[] getSetBytes(Object key, Object value) {
		try (Jedis jedis = getJedis()) {
			return jedis.getSet(toBytes(key), toBytes(value));
		}
	}
	
	/**
	 * 对key的值设置为value并返回原来的值
	 * 如果key存在但是对应的类型不是string则抛异常
	 * @param key
	 * @param value
	 * @return
	 */
	public static String getSet(Object key, Object value) {
		return toString(getSetBytes(key, value));
	}
	
	/**
	 * 将指定key的值减少某个值
	 * 如果key不存在，则默认原值为0
	 * 如果value不是string或不能转成integer则抛异常
	 * @param key
	 * @param integer
	 * @return 返回减少后的新值
	 */
	public static long decrBy(Object key, long integer) {
		try (Jedis jedis = getJedis()) {
			return jedis.decrBy(toBytes(key), integer);
		}
	}
	
	/**
	 * 将指定Key的值减少1
	 * 如果key不存在，则默认原值为0
	 * 如果value不是string或不能转成integer则抛异常
	 * @param key
	 * @return 返回减少后的新值
	 */
	public static long decr(Object key) {
		try (Jedis jedis = getJedis()) {
			return jedis.decr(toBytes(key));
		}
	}
	
	/**
	 * 将指定的key的值增加指定的值
	 * 如果key不存在，则默认原值为0
	 * 如果value不是string或不能转成integer则抛异常
	 * @param key
	 * @param integer
	 * @return 返回增加后的新值
	 */
	public static long incrBy(Object key, long integer) {
		try (Jedis jedis = getJedis()) {
			return jedis.incrBy(toBytes(key), integer);
		}
	}
	
	/**
	 * 将指定的key的值增加指定的值(浮点数)
	 * 如果key不存在，则默认原值为0
	 * 如果value不是string或不能转成float则抛异常
	 * @param key
	 * @param value 支持科学计数法(包括原值也支持)
	 * @return 返回增加后的新值
	 */
	public static double incrByFloat(Object key, double value) {
		try (Jedis jedis = getJedis()) {
			return jedis.incrByFloat(toBytes(key), value);
		}
	}
	
	/**
	 * 将指定的key的值增加1
	 * 如果key不存在，则默认原值为0
	 * 如果value不是string或不能转成integer则抛异常
	 * @param key
	 * @return 返回增加后的新值
	 */
	public static long incr(Object key) {
		try (Jedis jedis = getJedis()) {
			return jedis.incr(toBytes(key));
		}
	}
	
	/**
	 * 将value追加到原有字符串的末尾
	 * 如果key不存在，则默认原值为空字符串
	 * @param key
	 * @param value
	 * @return 新string的长度
	 */
	public static long append(Object key, Object value) {
		try (Jedis jedis = getJedis()) {
			return jedis.append(toBytes(key), toBytes(value));
		}
	}
	
	/**
	 * 得到key对应的string的长度
	 * 如果key不存在则返回0
	 * @param key
	 * @return
	 */
	public static long strlen(Object key) {
		try (Jedis jedis = getJedis()) {
			return jedis.strlen(toBytes(key));
		}
	}
	
	/**
	 * 覆盖key对应的string的一部分，从指定的offset处开始，覆盖value的长度
	 * 如果offset超过string的长度，则自动增长string直到offset
	 * 如果key不存在则创建新的string
	 * @param key
	 * @param offset
	 * @param value
	 * @return 新string的长度
	 */
	public static long setrange(Object key, long offset, Object value) {
		try (Jedis jedis = getJedis()) {
			return jedis.setrange(toBytes(key), offset, toBytes(value));
		}
	}
	
	/**
	 * 获得start - end之间的子字符串(包含end)
	 * 若偏移量为负数，代表从末尾开始计算，例如-1代表倒数第一个，-2代表倒数第二个
	 * @param key
	 * @param startOffset
	 * @param endOffset
	 * @return
	 */
	public static byte[] getrangeBytes(Object key, long startOffset, long endOffset) {
		try (Jedis jedis = getJedis()) {
			return jedis.getrange(toBytes(key), startOffset, endOffset);
		}
	}
	
	/**
	 * 获得start - end之间的子字符串(包含end)
	 * 若偏移量为负数，代表从末尾开始计算，例如-1代表倒数第一个，-2代表倒数第二个
	 * @return
	 */
	public static String getrange(Object key, long startOffset, long endOffset) {
		return toString(getrangeBytes(key, startOffset, endOffset));
	}
	
	/**
	 * 已经改名为{@link #getrange(Object, long, long)}}
	 * @param key
	 * @param start
	 * @param end
	 * @return
	 */
	@Deprecated
	public static byte[] substrBytes(Object key, int start, int end) {
		try (Jedis jedis = getJedis()) {
			return jedis.substr(toBytes(key), start, end);
		}
	}
	
	/**
	 * 已经改名为{@link #getrange(Object, long, long)}}
	 * @param key
	 * @param start
	 * @param end
	 * @return
	 */
	@Deprecated
	public static String substr(Object key, int start, int end) {
		return toString(substrBytes(key, start, end));
	}
	
	/**
	 * 设置或者清除指定key的value上的某个位置的比特位
	 * 如果该key原先不存在，则新创建一个key
	 * 其value将会自动分配内存，直到可以放下指定位置的bit值
	 * bitmaps最大限制为512MB，默认生成的比特位为0
	 * @param key
	 * @param offset
	 * @param value true代表1，false代表0
	 * @return 原来位置的bit值，如果是1，则返回true，否则返回false
	 */
	public static boolean setbit(Object key, long offset, boolean value) {
		try (Jedis jedis = getJedis()) {
			return jedis.setbit(toBytes(key), offset, value);
		}
	}
	
	/**
	 * 同{@link #setbit(Object, long, boolean)}机制相同
	 * @param key
	 * @param offset
	 * @param value 只能是"1"或者"0"
	 * @return
	 */
	public static boolean setbit(Object key, long offset, String value) {
		try (Jedis jedis = getJedis()) {
			return jedis.setbit(toBytes(key), offset, toBytes(value));
		}
	}
	
	/**
	 * 取得指定key的value上的某个位置的比特位
	 * 如果offset超过value长度，则认为value后面默认为0(返回0)
	 * 如果该key原先不存在，则认为数据为0(返回0)
	 * @param key
	 * @param offset
	 * @return true代表1，false代表0
	 */
	public static boolean getbit(Object key, long offset) {
		try (Jedis jedis = getJedis()) {
			return jedis.getbit(toBytes(key), offset);
		}
	}
	
	/**
	 * 取得指定key的value值中比特位为1的个数
	 * 通常用来作为实时统计
	 * @param key
	 * @return
	 */
	public static long bitcount(Object key) {
		try (Jedis jedis = getJedis()) {
			return jedis.bitcount(toBytes(key));
		}
	}
	
	/**
	 * 获得指定key中start - end之间(包含end)比特位为1的个数
	 * 若偏移量为负数，代表从末尾开始计算，例如-1代表倒数第一个，-2代表倒数第二个
	 * @param key
	 * @param start
	 * @param end
	 * @return
	 */
	public static long bitcount(Object key, long start, long end) {
		try (Jedis jedis = getJedis()) {
			return jedis.bitcount(toBytes(key), start, end);
		}
	}
	
	/**
	 * 将key对应的字符串做比特位级别的操作
	 * 将操作结果存入新的key
	 * 如果字符串长短不一，则默认短字符串末尾补0
	 * @param bitop and|or|xor|not
	 * @param destKey
	 * @param srcKeys
	 * @return 结果字符串的长度
	 */
	public static long bitop(String bitop, Object destKey, Object... srcKeys) {
		try (Jedis jedis = getJedis()) {
			return jedis.bitop(toBitOP(bitop), toBytes(destKey), toBytes(srcKeys));
		}
	}
	
	/**
	 * 得到key对应的string中第一个设置为value的bit的位置
	 * 如果查找bit为1的位置，没有找到将返回-1
	 * 如果查找bit为0的位置，没有找到将返回string末尾+1的位置
	 * @param key
	 * @param value 1为true，0为false
	 * @return
	 */
	public static long bitpos(Object key, boolean value) {
		try (Jedis jedis = getJedis()) {
			return jedis.bitpos(toBytes(key), value);
		}
	}
	
	/**
	 * 得到key对应的string中第一个设置为value的bit的位置
	 * 如果查找bit为1的位置，没有找到将返回-1
	 * 如果查找bit为0的位置，没有找到时
	 * 如果end为null将返回string末尾+1的位置
	 * 否则返回-1
	 * @param key
	 * @param value 1为true，0为false
	 * @param start
	 * @param end
	 * @return
	 */
	public static long bitpos(Object key, boolean value, long start, Long end) {
		try (Jedis jedis = getJedis()) {
			return jedis.bitpos(toBytes(key), value, toBitPosParams(start, end));
		}
	}
	
	/********** hash命令 **********/
	
	/**
	 * 设置hash表里field字段的值为value
	 * 如果key不存在，则创建一个新的hash表
	 * 如果key原来的值不是hash则抛异常
	 * @param key
	 * @param field
	 * @param value
	 * @return 新增为true，更新为false
	 */
	public static boolean hset(Object key, Object field, Object value) {
		try (Jedis jedis = getJedis()) {
			return jedis.hset(toBytes(key), toBytes(field), toBytes(value)) == 1L;
		}
	}
	
	/**
	 * 如果该key对应的值是一个Hash表，则返回对应字段的值。
	 * 如果不存在该字段，或者key不存在，则返回null
	 * @param key
	 * @param field
	 * @return
	 */
	public static byte[] hgetBytes(Object key, Object field) {
		try (Jedis jedis = getJedis()) {
			return jedis.hget(toBytes(key), toBytes(field));
		}
	}
	
	/**
	 * 如果该key对应的值是一个Hash表，则返回对应字段的值。
	 * 如果不存在该字段，或者key不存在，则返回null
	 * @param key
	 * @param field
	 * @return
	 */
	public static String hget(Object key, Object field) {
		return toString(hgetBytes(key, field));
	}
	
	/**
	 * 设置hash表里field字段的值为value
	 * 如果key不存在，则创建一个新的hash表
	 * 如果field存在，则不做操作
	 * @param key
	 * @param field
	 * @param value
	 * @return 是否设置成功
	 */
	public static boolean hsetnx(Object key, Object field, Object value) {
		try (Jedis jedis = getJedis()) {
			return jedis.hsetnx(toBytes(key), toBytes(field), toBytes(value)) == 1L;
		}
	}
	
	/**
	 * 同时设置hash表里多个field字段
	 * 如果已存在则直接覆盖
	 * @param key
	 * @param hash
	 */
	public static void hmset(Object key, Map<?, ?> hash) {
		try (Jedis jedis = getJedis()) {
			jedis.hmset(toBytes(key), toBytes(hash));
		}
	}
	
	/**
	 * 同时取得hash表里多个field字段
	 * 如果field不存在则返回为null
	 * 如果key不存在则返回都为null
	 * @param key
	 * @param fields
	 * @return
	 */
	public static List<byte[]> hmgetBytes(Object key, Object... fields) {
		try (Jedis jedis = getJedis()) {
			return jedis.hmget(toBytes(key), toBytes(fields));
		}
	}
	
	/**
	 * 同时取得hash表里多个field字段
	 * 如果field不存在则返回为null
	 * 如果key不存在则返回都为null
	 * @param key
	 * @param fields
	 * @return
	 */
	public static List<String> hmget(Object key, Object... fields) {
		return toString(hmgetBytes(key, fields));
	}
	
	/**
	 * 对hash中指定字段的值增加指定的值
	 * 如果field不存在，则默认原值为0
	 * 如果value不是string或不能转成integer则抛异常
	 * @param key
	 * @param field
	 * @param value
	 * @return 返回增加后的新值
	 */
	public static long hincrBy(Object key, Object field, long value) {
		try (Jedis jedis = getJedis()) {
			return jedis.hincrBy(toBytes(key), toBytes(field), value);
		}
	}
	
	/**
	 * 对hash中指定字段的值增加指定的值(浮点数)
	 * 如果field不存在，则默认原值为0
	 * 如果value不是string或不能转成float则抛异常
	 * @param key
	 * @param field
	 * @param value 支持科学计数法(包括原值也支持)
	 * @return 返回增加后的新值
	 */
	public static double hincrByFloat(Object key, Object field, double value) {
		try (Jedis jedis = getJedis()) {
			return jedis.hincrByFloat(toBytes(key), toBytes(field), value);
		}
	}
	
	/**
	 * 判断hash中指定字段是否存在
	 * @param key
	 * @param field
	 * @return
	 */
	public static boolean hexists(Object key, Object field) {
		try (Jedis jedis = getJedis()) {
			return jedis.hexists(toBytes(key), toBytes(field));
		}
	}
	
	/**
	 * 删除hash中指定字段
	 * @param key
	 * @param fields
	 * @return 实际被删除的field数量
	 */
	public static long hdel(Object key, Object... fields) {
		try (Jedis jedis = getJedis()) {
			return jedis.hdel(toBytes(key), toBytes(fields));
		}
	}
	
	/**
	 * 得到hash中字段的数量
	 * key不存在时返回0
	 * @param key
	 * @return
	 */
	public static long hlen(Object key) {
		try (Jedis jedis = getJedis()) {
			return jedis.hlen(toBytes(key));
		}
	}
	
	/**
	 * 得到hash中的所有字段
	 * @param key
	 * @return
	 */
	public static Set<byte[]> hkeysBytes(Object key) {
		try (Jedis jedis = getJedis()) {
			return jedis.hkeys(toBytes(key));
		}
	}
	
	/**
	 * 得到hash中的所有字段
	 * @param key
	 * @return
	 */
	public static Set<String> hkeys(Object key) {
		return toString(hkeysBytes(key));
	}
	
	/**
	 * 得到hash中的所有字段的值
	 * @param key
	 * @return
	 */
	public static List<byte[]> hvalsBytes(Object key) {
		try (Jedis jedis = getJedis()) {
			return jedis.hvals(toBytes(key));
		}
	}
	
	/**
	 * 得到hash中的所有字段的值
	 * @param key
	 * @return
	 */
	public static List<String> hvals(Object key) {
		return toString(hvalsBytes(key));
	}
	
	/**
	 * 返回key指定的hash中所有的字段和值
	 * @param key
	 * @return
	 */
	public static Map<byte[], byte[]> hgetAllBytes(Object key) {
		try (Jedis jedis = getJedis()) {
			return jedis.hgetAll(toBytes(key));
		}
	}
	
	/**
	 * 返回key指定的hash中所有的字段和值
	 * @param key
	 * @return
	 */
	public static Map<String, String> hgetAll(Object key) {
		return toString(hgetAllBytes(key));
	}
	
	/**
	 * 同{@link #hscan(Object, String)}原理
	 * @param cursor
	 * @return
	 */
	public static ScanResult<Map.Entry<byte[], byte[]>> hscanBytes(Object key, String cursor) {
		try (Jedis jedis = getJedis()) {
			return jedis.hscan(toBytes(key), toBytes(cursor));
		}
	}
	
	/**
	 * 迭代得到hash中所有的元素与其值
	 * 保证在完整迭代周期内始终存在的元素一定会返回
	 * 保证在完整迭代周期内始终不存在的元素一定不会返回
	 * 其他情况的元素返回性不能保证，有可能不返回，有可能重复返回多次
	 * 一次返回的元素个数不定，有可能为0
	 * @param key
	 * @param cursor 游标以0开始，以0结束
	 * @return
	 */
	public static ScanResult<Map.Entry<String, String>> hscan(Object key, String cursor) {
		return toStringH(hscanBytes(key, cursor));
	}
	
	/**
	 * 同{@link #hscan(Object, String, String, Integer)}原理
	 * @param cursor
	 * @param match
	 * @param count
	 * @return
	 */
	public static ScanResult<Map.Entry<byte[], byte[]>> hscanBytes(Object key, String cursor, String match, Integer count) {
		try (Jedis jedis = getJedis()) {
			return jedis.hscan(toBytes(key), toBytes(cursor), toScanParams(match, count));
		}
	}
	
	/**
	 * 迭代得到hash中所有的元素与其值
	 * 保证在完整迭代周期内始终存在的元素一定会返回
	 * 保证在完整迭代周期内始终不存在的元素一定不会返回
	 * 其他情况的元素返回性不能保证，有可能不返回，有可能重复返回多次
	 * 一次返回的元素个数不定，有可能为0
	 * @param key
	 * @param cursor 游标以0开始，以0结束
	 * @param match 支持*?[ae][a-e][^e]转义为\
	 * @param count 希望一次返回的元素数量，但并不保证
	 * @return
	 */
	public static ScanResult<Map.Entry<String, String>> hscan(Object key, String cursor, String match, Integer count) {
		return toStringH(hscanBytes(key, cursor, match, count));
	}
	
	/********** list命令 **********/
	
	/**
	 * 向key指定的list尾部插入新值
	 * 如果key不存在，则创建一个新的list
	 * 如果key原来的值不是list则抛异常
	 * @param key
	 * @param values
	 * @return 插入后list的长度
	 */
	public static long rpush(Object key, Object... values) {
		try (Jedis jedis = getJedis()) {
			return jedis.rpush(toBytes(key), toBytes(values));
		}
	}
	
	/**
	 * 向key指定的list头部插入新值
	 * 如果key不存在，则创建一个新的list
	 * 如果key原来的值不是list则抛异常
	 * 注意参数的顺序与结果的顺序是相反的
	 * @param key
	 * @param values
	 * @return 插入后list的长度
	 */
	public static long lpush(Object key, Object... values) {
		try (Jedis jedis = getJedis()) {
			return jedis.lpush(toBytes(key), toBytes(values));
		}
	}
	
	/**
	 * 只有当key存在时才向list尾部插入新值
	 * @param key
	 * @param values
	 * @return 插入后list的长度
	 */
	public static long rpushx(Object key, Object... values) {
		try (Jedis jedis = getJedis()) {
			return jedis.rpushx(toBytes(key), toBytes(values));
		}
	}
	
	/**
	 * 只有当key存在时才向list头部插入新值
	 * 注意参数的顺序与结果的顺序是相反的
	 * @param key
	 * @param values
	 * @return
	 */
	public static long lpushx(Object key, Object... values) {
		try (Jedis jedis = getJedis()) {
			return jedis.lpushx(toBytes(key), toBytes(values));
		}
	}
	
	/**
	 * 得到key指定的list的长度
	 * @param key
	 * @return
	 */
	public static long llen(Object key) {
		try (Jedis jedis = getJedis()) {
			return jedis.llen(toBytes(key));
		}
	}
	
	/**
	 * 获得key指定的list下标在start - end之间的元素(包含end)
	 * 若索引为负数，代表从末尾开始计算，例如-1代表倒数第一个，-2代表倒数第二个
	 * @param key
	 * @param start
	 * @param end
	 * @return
	 */
	public static List<byte[]> lrangeBytes(Object key, long start, long end) {
		try (Jedis jedis = getJedis()) {
			return jedis.lrange(toBytes(key), start, end);
		}
	}
	
	/**
	 * 获得key指定的list下标在start - end之间的元素(包含end)
	 * 若索引为负数，代表从末尾开始计算，例如-1代表倒数第一个，-2代表倒数第二个
	 * @param key
	 * @param start
	 * @param end
	 * @return
	 */
	public static List<String> lrange(Object key, long start, long end) {
		return toString(lrangeBytes(key, start, end));
	}
	
	/**
	 * 修剪key指定的list，只保留下标在start - end之间的元素(包含end)
	 * 若索引为负数，代表从末尾开始计算，例如-1代表倒数第一个，-2代表倒数第二个
	 * 如果范围内的元素为0，则整个key会被删除
	 * 通常用以下命令组合来维护一个定长列表
	 * lpush key value
	 * ltrim key 0 99
	 * @param key
	 * @param start
	 * @param end
	 */
	public static void ltrim(Object key, long start, long end) {
		try (Jedis jedis = getJedis()) {
			jedis.ltrim(toBytes(key), start, end);
		}
	}
	
	/**
	 * 得到下标为index的list元素
	 * 若索引为负数，代表从末尾开始计算，例如-1代表倒数第一个，-2代表倒数第二个
	 * 索引不存在则返回null
	 * @param key
	 * @param index
	 * @return
	 */
	public static byte[] lindexBytes(Object key, long index) {
		try (Jedis jedis = getJedis()) {
			return jedis.lindex(toBytes(key), index);
		}
	}
	
	/**
	 * 得到list下标为index的元素
	 * 若索引为负数，代表从末尾开始计算，例如-1代表倒数第一个，-2代表倒数第二个
	 * 索引不存在则返回null
	 * @param key
	 * @param index
	 * @return
	 */
	public static String lindex(Object key, long index) {
		return toString(lindexBytes(key, index));
	}
	
	/**
	 * 设置list下标为index的元素为value
	 * 若索引为负数，代表从末尾开始计算，例如-1代表倒数第一个，-2代表倒数第二个
	 * 索引不存在则抛异常
	 * @param key
	 * @param index
	 * @param value
	 */
	public static void lset(Object key, long index, Object value) {
		try (Jedis jedis = getJedis()) {
			jedis.lset(toBytes(key), index, toBytes(value));
		}
	}
	
	/**
	 * 在list中指定的值元素的前面或后面插入一个新值
	 * 只会对找到的第一个元素前后进行插入操作
	 * 如果key不存在或元素没找到则不进行操作
	 * @param key
	 * @param where before|after
	 * @param pivot 要搜索的元素值
	 * @param value 要插入的新值
	 * @return 插入后list的长度
	 * 如果0则表示key不存在
	 * 如果-1则表示元素没找到
	 */
	public static long linsert(Object key, String where, Object pivot, Object value) {
		try (Jedis jedis = getJedis()) {
			return jedis.linsert(toBytes(key), toListPosition(where), toBytes(pivot), toBytes(value));
		}
	}
	
	/**
	 * 删除list中count个指定的值元素
	 * count &gt; 0 从头部开始删除
	 * count &lt; 0 从尾部开始删除
	 * count = 0 删除所有匹配值
	 * 数组长度为0时会删除整个key
	 * @param key
	 * @param count
	 * @param value 要删除的值
	 * @return 实际删除的元素数量
	 */
	public static long lrem(Object key, long count, Object value) {
		try (Jedis jedis = getJedis()) {
			return jedis.lrem(toBytes(key), count, toBytes(value));
		}
	}
	
	/**
	 * 移除并返回list的第一个元素
	 * 数组长度为0时会删除整个key
	 * key不存在时返回null
	 * @param key
	 * @return
	 */
	public static byte[] lpopBytes(Object key) {
		try (Jedis jedis = getJedis()) {
			return jedis.lpop(toBytes(key));
		}
	}
	
	/**
	 * 移除并返回list的第一个元素
	 * 数组长度为0时会删除整个key
	 * key不存在时返回null
	 * @param key
	 * @return
	 */
	public static String lpop(Object key) {
		return toString(lpopBytes(key));
	}
	
	/**
	 * 移除并返回list的最后一个元素
	 * 数组长度为0时会删除整个key
	 * key不存在时返回null
	 * @param key
	 * @return
	 */
	public static byte[] rpopBytes(Object key) {
		try (Jedis jedis = getJedis()) {
			return jedis.rpop(toBytes(key));
		}
	}
	
	/**
	 * 移除并返回list的最后一个元素
	 * 数组长度为0时会删除整个key
	 * key不存在时返回null
	 * @param key
	 * @return
	 */
	public static String rpop(Object key) {
		return toString(rpopBytes(key));
	}
	
	/**
	 * 同{@link #rpoplpush(Object, Object)}}原理
	 * @param srckey
	 * @param dstkey
	 * @return 被转移的元素
	 */
	public static byte[] rpoplpushBytes(Object srckey, Object dstkey) {
		try (Jedis jedis = getJedis()) {
			return jedis.rpoplpush(toBytes(srckey), toBytes(dstkey));
		}
	}
	
	/**
	 * 从第一个list的尾部移除元素并插入到第二个list的头部
	 * 如果第一个list不存在则不进行操作并返回null
	 * 如果第二个list不存在则创建该key
	 * 如果两个list相同，则表示将尾部的元素移到头部
	 * 常用于循环列表的实现或原子级别的列表元素转移
	 * @param srckey
	 * @param dstkey
	 * @return 被转移的元素
	 */
	public static String rpoplpush(Object srckey, Object dstkey) {
		return toString(rpoplpushBytes(srckey, dstkey));
	}
	
	/**
	 * 同{@link #blpop(int, Object...)}原理
	 * @param keys
	 * @return
	 */
	public static List<byte[]> blpopBytes(int timeout, Object... keys) {
		try (Jedis jedis = getJedis()) {
			return jedis.blpop(timeout, toBytes(keys));
		}
	}
	
	/**
	 * 阻塞版本的{@link #lpop(Object)}
	 * 接收多个key作为参数
	 * 非阻塞时：根据参数的顺序查找非空的列表
	 * 将找到的第一个非空列表的列头元素移除，同时返回该列表的key与移除的元素
	 * 阻塞时：所有参数指向的列表都不存在时，连接将阻塞
	 * 当参数指向的列表中有新元素插入时，将列表头元素移除，同时返回该列表的key与移除的元素
	 * 当到达超时时间还没有元素插入时，返回null
	 * 当超时时间为0时，表示永久阻塞
	 * 当有多个连接同时阻塞在一个key，最先阻塞的连接将获得返回
	 * 当阻塞的多个key同时有新元素插入时，最先插入数据的key将作为被操作的列表
	 * 当在一个事务中对列表做多个插入或删除时，事务完成后该列表才被此命令操作
	 * 当在事务中尝试运行此命令，当列表都为空时，连接将不会被阻塞，效果等同于立即超时并返回null
	 * @param keys
	 * @return 按照参数顺序找到的第一个非空列表的key与列头元素
	 */
	public static List<String> blpop(int timeout, Object... keys) {
		return toString(blpopBytes(timeout, keys));
	}
	
	/**
	 * 同{@link #brpop(int, Object...)}原理
	 * @param timeout
	 * @param keys
	 * @return
	 */
	public static List<byte[]> brpopBytes(int timeout, Object... keys) {
		try (Jedis jedis = getJedis()) {
			return jedis.brpop(timeout, toBytes(keys));
		}
	}
	
	/**
	 * 阻塞版本的{@link #rpop(Object)}
	 * 接收多个key作为参数
	 * 非阻塞时：根据参数的顺序查找非空的列表
	 * 将找到的第一个非空列表的列尾元素移除，同时返回该列表的key与移除的元素
	 * 阻塞时：所有参数指向的列表都不存在时，连接将阻塞
	 * 当参数指向的列表中有新元素插入时，将列表尾元素移除，同时返回该列表的key与移除的元素
	 * 当到达超时时间还没有元素插入时，返回null
	 * 当超时时间为0时，表示永久阻塞
	 * 当有多个连接同时阻塞在一个key，最先阻塞的连接将获得返回
	 * 当阻塞的多个key同时有新元素插入时，最先插入数据的key将作为被操作的列表
	 * 当在一个事务中对列表做多个插入或删除时，事务完成后该列表才被此命令操作
	 * 当在事务中尝试运行此命令，当列表都为空时，连接将不会被阻塞，效果等同于立即超时并返回null
	 * @param timeout
	 * @param keys
	 * @return 按照参数顺序找到的第一个非空列表的key与列头元素
	 */
	public static List<String> brpop(int timeout, Object... keys) {
		return toString(brpopBytes(timeout, keys));
	}
	
	/**
	 * 同{@link #brpoplpush(Object, Object, int)}原理
	 * @param source
	 * @param destination
	 * @param timeout
	 * @return
	 */
	public static byte[] brpoplpushBytes(Object source, Object destination, int timeout) {
		try (Jedis jedis = getJedis()) {
			return jedis.brpoplpush(toBytes(source), toBytes(destination), timeout);
		}
	}
	
	/**
	 * 阻塞版本的{@link #rpoplpush(Object, Object)}
	 * 当第一个列表为空时阻塞直到有元素插入或超时
	 * 当超时时间为0时表示永久阻塞
	 * 当在事务中执行此命令时，连接将不会被阻塞，效果等同于立即超时
	 * @param source
	 * @param destination
	 * @param timeout
	 * @return 被转移的元素或超时返回null
	 */
	public static String brpoplpush(Object source, Object destination, int timeout) {
		return toString(brpoplpushBytes(source, destination, timeout));
	}
	
	/********** set命令 **********/
	
	/**
	 * 向key指定的set中增加元素
	 * set中的元素不会重复
	 * 如果key不存在则新建
	 * @param key
	 * @param members
	 * @return 实际加入set的元素个数
	 */
	public static long sadd(Object key, Object... members) {
		try (Jedis jedis = getJedis()) {
			return jedis.sadd(toBytes(key), toBytes(members));
		}
	}
	
	/**
	 * 将元素从一个set转移到另一个set
	 * 如果元素不存在则不做操作
	 * 如果另一个set中元素已经存在，则仅仅从第一个set中删除
	 * @param srckey
	 * @param dstkey
	 * @param member
	 * @return 转移是否成功
	 */
	public static boolean smove(Object srckey, Object dstkey, Object member) {
		try (Jedis jedis = getJedis()) {
			return jedis.smove(toBytes(srckey), toBytes(dstkey), toBytes(member)) == 1L;
		}
	}
	
	/**
	 * 删除set里的指定元素
	 * 不存在的元素将被忽略
	 * @param key
	 * @param members
	 * @return 实际被删除的元素数量
	 */
	public static long srem(Object key, Object... members) {
		try (Jedis jedis = getJedis()) {
			return jedis.srem(toBytes(key), toBytes(members));
		}
	}
	
	/**
	 * 同#{@link #spop(Object)}原理
	 * @param key
	 * @return
	 */
	public static byte[] spopBytes(Object key) {
		try (Jedis jedis = getJedis()) {
			return jedis.spop(toBytes(key));
		}
	}
	
	/**
	 * 移除并返回set里的一个随机元素
	 * 如果key不存在则返回null
	 * @param key
	 * @return 被移除的元素
	 */
	public static String spop(Object key) {
		return toString(spopBytes(key));
	}
	
	/**
	 * 同{@link #spop(Object, long)}原理
	 * @param key
	 * @param count
	 * @return
	 */
	public static Set<byte[]> spopBytes(Object key, long count) {
		try (Jedis jedis = getJedis()) {
			return jedis.spop(toBytes(key), count);
		}
	}
	
	/**
	 * The count argument is not available yet
	 * 移除并返回set里的多个随机元素
	 * 如果key不存在则返回空set
	 * @param key
	 * @param count 要返回的元素个数
	 * @return
	 */
	public static Set<String> spop(Object key, long count) {
		return toString(spopBytes(key, count));
	}
	
	/**
	 * 得到set中元素的个数
	 * 如果key不存在则返回0
	 * @param key
	 * @return
	 */
	public static long scard(Object key) {
		try (Jedis jedis = getJedis()) {
			return jedis.scard(toBytes(key));
		}
	}
	
	/**
	 * 得到第一个集合减去其他集合的差集
	 * @param keys
	 * @return
	 */
	public static Set<byte[]> sdiffBytes(Object... keys) {
		try (Jedis jedis = getJedis()) {
			return jedis.sdiff(toBytes(keys));
		}
	}
	
	/**
	 * 得到第一个集合减去其他集合的差集
	 * @param keys
	 * @return
	 */
	public static Set<String> sdiff(Object... keys) {
		return toString(sdiffBytes(keys));
	}
	
	/**
	 * 将第一个集合减去其他集合的差集存入新的集合
	 * 如果目标key已存在则被覆盖
	 * @param dstkey
	 * @param keys
	 * @return 新集合元素的数量
	 */
	public static long sdiffstore(Object dstkey, Object... keys) {
		try (Jedis jedis = getJedis()) {
			return jedis.sdiffstore(toBytes(dstkey), toBytes(keys));
		}
	}
	
	/**
	 * 得到多个集合的交集
	 * @param keys
	 * @return
	 */
	public static Set<byte[]> sinterBytes(Object... keys) {
		try (Jedis jedis = getJedis()) {
			return jedis.sinter(toBytes(keys));
		}
	}
	
	/**
	 * 得到多个集合的交集
	 * @param keys
	 * @return
	 */
	public static Set<String> sinter(Object... keys) {
		return toString(sinterBytes(keys));
	}
	
	/**
	 * 将多个集合的交集存入新的集合
	 * 如果目标key已存在则被覆盖
	 * @param dstkey
	 * @param keys
	 * @return
	 */
	public static long sinterstore(Object dstkey, Object... keys) {
		try (Jedis jedis = getJedis()) {
			return jedis.sinterstore(toBytes(dstkey), toBytes(keys));
		}
	}
	
	/**
	 * 得到多个集合的并集
	 * @param keys
	 * @return
	 */
	public static Set<byte[]> sunionBytes(Object... keys) {
		try (Jedis jedis = getJedis()) {
			return jedis.sunion(toBytes(keys));
		}
	}
	
	/**
	 * 得到多个集合的并集
	 * @param keys
	 * @return
	 */
	public static Set<String> sunion(Object... keys) {
		return toString(sunionBytes(keys));
	}
	
	/**
	 * 将多个集合的并集存入新的集合
	 * 如果目标key已存在则被覆盖
	 * @param dstkey
	 * @param keys
	 * @return
	 */
	public static long sunionstore(Object dstkey, Object... keys) {
		try (Jedis jedis = getJedis()) {
			return jedis.sunionstore(toBytes(dstkey), toBytes(keys));
		}
	}
	
	/**
	 * 判断指定set中是否存在某元素
	 * @param key
	 * @param member
	 * @return
	 */
	public static boolean sismember(Object key, Object member) {
		try (Jedis jedis = getJedis()) {
			return jedis.sismember(toBytes(key), toBytes(member));
		}
	}
	
	/**
	 * 同{@link #srandmember(Object)}原理
	 * @param key
	 * @return
	 */
	public static byte[] srandmemberBytes(Object key) {
		try (Jedis jedis = getJedis()) {
			return jedis.srandmember(toBytes(key));
		}
	}
	
	/**
	 * 获取set里的一个随机元素
	 * 如果key不存在则返回null
	 * @param key
	 * @return
	 */
	public static String srandmember(Object key) {
		return toString(srandmemberBytes(key));
	}
	
	/**
	 * 同{@link #srandmemberBytes(Object, int)}原理
	 * @param key
	 * @param count
	 * @return
	 */
	public static List<byte[]> srandmemberBytes(Object key, int count) {
		try (Jedis jedis = getJedis()) {
			return jedis.srandmember(toBytes(key), count);
		}
	}
	
	/**
	 * 获取set里的多个随机元素
	 * count &gt; 0 返回不重复的元素，结果集有可能小于count
	 * count &lt; 0 返回有可能重复的count个元素
	 * 如果key不存在则返回空set
	 * @param key
	 * @param count
	 * @return
	 */
	public static List<String> srandmember(Object key, int count) {
		return toString(srandmemberBytes(key, count));
	}
	
	/**
	 * 同{@link #smembers(Object)}原理
	 * @return
	 */
	public static Set<byte[]> smembersBytes(Object key) {
		try (Jedis jedis = getJedis()) {
			return jedis.smembers(toBytes(key));
		}
	}
	
	/**
	 * 得到指定set里的所有元素
	 * @param key
	 * @return
	 */
	public static Set<String> smembers(Object key) {
		return toString(smembersBytes(key));
	}
	
	/**
	 * 同{@link #sscan(Object, String)}原理
	 * @param cursor
	 * @return
	 */
	public static ScanResult<byte[]> sscanBytes(Object key, String cursor) {
		try (Jedis jedis = getJedis()) {
			return jedis.sscan(toBytes(key), toBytes(cursor));
		}
	}
	
	/**
	 * 迭代得到set中的所有元素
	 * 保证在完整迭代周期内始终存在的元素一定会返回
	 * 保证在完整迭代周期内始终不存在的元素一定不会返回
	 * 其他情况的元素返回性不能保证，有可能不返回，有可能重复返回多次
	 * 一次返回的元素个数不定，有可能为0
	 * @param key
	 * @param cursor 游标以0开始，以0结束
	 * @return
	 */
	public static ScanResult<String> sscan(Object key, String cursor) {
		return toString(sscanBytes(key, cursor));
	}
	
	/**
	 * 同{@link #sscan(Object, String, String, Integer)}原理
	 * @param cursor
	 * @param match
	 * @param count
	 * @return
	 */
	public static ScanResult<byte[]> sscanBytes(Object key, String cursor, String match, Integer count) {
		try (Jedis jedis = getJedis()) {
			return jedis.sscan(toBytes(key), toBytes(cursor), toScanParams(match, count));
		}
	}
	
	/**
	 * 迭代得到set中的所有元素
	 * 保证在完整迭代周期内始终存在的元素一定会返回
	 * 保证在完整迭代周期内始终不存在的元素一定不会返回
	 * 其他情况的元素返回性不能保证，有可能不返回，有可能重复返回多次
	 * 一次返回的元素个数不定，有可能为0
	 * @param key
	 * @param cursor 游标以0开始，以0结束
	 * @param match 支持*?[ae][a-e][^e]转义为\
	 * @param count 希望一次返回的元素数量，但并不保证
	 * @return
	 */
	public static ScanResult<String> sscan(Object key, String cursor, String match, Integer count) {
		return toString(sscanBytes(key, cursor, match, count));
	}
	
	/********** zset命令 **********/
	
	/**
	 * 向key指定的有序集合中添加元素与其分数
	 * 如果元素已存在，则更新其分数
	 * 如果key不存在则新增有序集合
	 * 集合中的元素以分数升序排列
	 * 如果分数相同，则以字典排序
	 * @param key
	 * @param score
	 * @param member
	 * @return 是否插入成功
	 */
	public static boolean zadd(Object key, double score, Object member) {
		try (Jedis jedis = getJedis()) {
			return jedis.zadd(toBytes(key), score, toBytes(member)) == 1L;
		}
	}
	
	/**
	 * 向key指定的有序集合中添加元素与其分数
	 * 如果元素已存在，则更新其分数
	 * 如果key不存在则新增有序集合
	 * 集合中的元素以分数升序排列
	 * 如果分数相同，则以字典排序
	 * @param key
	 * @param score
	 * @param member
	 * @param nxxx 只能取NX或者XX，如果取NX，则只有当元素不存在时进行插入，不做更新。如果取XX，则只有当元素已经存在时进行更新，不做插入
	 * @param ch 只能取ch或null，如果取ch，则返回结果表示是否改动，包括插入与更新。如果元素与分数都相同，则不算作改动。
	 * @return 是否插入成功
	 */
	public static boolean zadd(Object key, double score, Object member, String nxxx, String ch) {
		try (Jedis jedis = getJedis()) {
			return jedis.zadd(toBytes(key), score, toBytes(member), toZAddParams(nxxx, ch)) == 1L;
		}
	}
	
	/**
	 * 向key指定的有序集合中添加元素与其分数
	 * 如果元素已存在，则更新其分数
	 * 如果key不存在则新增有序集合
	 * 集合中的元素以分数升序排列
	 * 如果分数相同，则以字典排序
	 * @param key
	 * @param scoreMembers
	 * @return 实际添加的元素数量
	 */
	public static long zadd(Object key, Map<?, Double> scoreMembers) {
		try (Jedis jedis = getJedis()) {
			return jedis.zadd(toBytes(key), toBytesZ(scoreMembers));
		}
	}
	
	/**
	 * 向key指定的有序集合中添加元素与其分数
	 * 如果元素已存在，则更新其分数
	 * 如果key不存在则新增有序集合
	 * 集合中的元素以分数升序排列
	 * 如果分数相同，则以字典排序
	 * @param key
	 * @param scoreMembers
	 * @param nxxx 只能取NX或者XX，如果取NX，则只有当元素不存在时进行插入，不做更新。如果取XX，则只有当元素已经存在时进行更新，不做插入
	 * @param ch 只能取ch或null，如果取ch，则返回结果表示实际改动的元素数量，包括插入与更新。如果元素与分数都相同，则不算作改动。
	 * @return 实际添加的元素数量
	 */
	public static long zadd(Object key, Map<?, Double> scoreMembers, String nxxx, String ch) {
		try (Jedis jedis = getJedis()) {
			return jedis.zadd(toBytes(key), toBytesZ(scoreMembers), toZAddParams(nxxx, ch));
		}
	}
	
	/**
	 * 对zset中指定元素的分数增加指定的值
	 * 如果元素不存在，则默认原值为0
	 * 增加的分数可以为负数
	 * @param key
	 * @param score
	 * @param member
	 * @return 返回增加后的新分数
	 */
	public static double zincrby(Object key, double score, Object member) {
		try (Jedis jedis = getJedis()) {
			return jedis.zincrby(toBytes(key), score, toBytes(member));
		}
	}
	
	/**
	 * 对zset中指定元素的分数增加指定的值
	 * 如果元素不存在，则默认原值为0
	 * 增加的分数可以为负数
	 * @param key
	 * @param score
	 * @param member
	 * @param nxxx 只能取NX或者XX，如果取NX，则只有当元素不存在时进行插入，不做更新。如果取XX，则只有当元素已经存在时进行更新，不做插入
	 * @return 返回增加后的新分数，如果不符合新增或更新条件，则返回null
	 */
	public static Double zincrby(Object key, double score, Object member, String nxxx) {
		try (Jedis jedis = getJedis()) {
			return jedis.zincrby(toBytes(key), score, toBytes(member), toZIncrByParams(nxxx));
		}
	}
	
	/**
	 * 得到zset中元素的个数
	 * 如果key不存在，则返回0
	 * @param key
	 * @return
	 */
	public static long zcard(Object key) {
		try (Jedis jedis = getJedis()) {
			return jedis.zcard(toBytes(key));
		}
	}
	
	/**
	 * 得到zset中分数介于min与max之间的元素个数(包含min与max)
	 * @param key
	 * @param min
	 * @param max
	 * @return
	 */
	public static long zcount(Object key, double min, double max) {
		try (Jedis jedis = getJedis()) {
			return jedis.zcount(toBytes(key), min, max);
		}
	}
	
	/**
	 * 得到zset中分数介于min与max之间的元素个数
	 * @param key
	 * @param min 必须以(或[开头，[表示包含，(表示不包含。+inf -inf 表示正负无穷大
	 * @param max 必须以(或[开头，[表示包含，(表示不包含。+inf -inf 表示正负无穷大
	 * @return
	 */
	public static long zcount(Object key, String min, String max) {
		try (Jedis jedis = getJedis()) {
			return jedis.zcount(toBytes(key), toBytes(min), toBytes(max));
		}
	}
	
	/**
	 * 当元素的分数都相同时，返回按照字典排序介于min与max之间的元素个数
	 * 当元素的分数不全都相同时，返回结果不可预料
	 * @param key
	 * @param min 必须以(或[开头，[表示包含，(表示不包含。+-表示正负无穷大
	 * @param max 必须以(或[开头，[表示包含，(表示不包含。+-表示正负无穷大
	 * @return
	 */
	public static long zlexcount(Object key, String min, String max) {
		try (Jedis jedis = getJedis()) {
			return jedis.zlexcount(toBytes(key), toBytes(min), toBytes(max));
		}
	}
	
	/**
	 * 返回zset中元素对应的分数
	 * 如果元素不存在或key不存在则返回null
	 * @param key
	 * @param member
	 * @return
	 */
	public static Double zscore(Object key, Object member) {
		try (Jedis jedis = getJedis()) {
			return jedis.zscore(toBytes(key), toBytes(member));
		}
	}
	
	/**
	 * 得到元素在zset中的排名
	 * 集合中的元素以分数升序排列
	 * 如果分数相同，则以字典排序
	 * 返回的排名从0开始
	 * 如果元素不存在或key不存在则返回null
	 * @param key
	 * @param member
	 * @return 排名或null
	 */
	public static Long zrank(Object key, Object member) {
		try (Jedis jedis = getJedis()) {
			return jedis.zrank(toBytes(key), toBytes(member));
		}
	}
	
	/**
	 * 得到元素在zset中的降序排名
	 * 如果元素不存在或key不存在则返回null
	 * @param key
	 * @param member
	 * @return 排名或null
	 */
	public static Long zrevrank(Object key, Object member) {
		try (Jedis jedis = getJedis()) {
			return jedis.zrevrank(toBytes(key), toBytes(member));
		}
	}
	
	/**
	 * 删除zset里的指定元素
	 * 不存在的元素将被忽略
	 * @param key
	 * @param members
	 * @return 实际被删除的元素数量
	 */
	public static long zrem(Object key, Object... members) {
		try (Jedis jedis = getJedis()) {
			return jedis.zrem(toBytes(key), toBytes(members));
		}
	}
	
	/**
	 * 删除经过排序的zset中位置在start - end之间的元素(包含end)
	 * 若位置为负数，代表从末尾开始计算，例如-1代表倒数第一个，-2代表倒数第二个
	 * 集合中的元素以分数升序排列
	 * 如果分数相同，则以字典排序
	 * @param key
	 * @param start
	 * @param end
	 * @return 实际被删除的元素数量
	 */
	public static long zremrangeByRank(Object key, long start, long end) {
		try (Jedis jedis = getJedis()) {
			return jedis.zremrangeByRank(toBytes(key), start, end);
		}
	}
	
	/**
	 * 当元素的分数都相同时，删除按照字典排序介于min与max之间的元素
	 * 当元素的分数不全都相同时，结果不可预料
	 * @param key
	 * @param min 必须以(或[开头，[表示包含，(表示不包含。+-表示正负无穷大
	 * @param max 必须以(或[开头，[表示包含，(表示不包含。+-表示正负无穷大
	 * @return 实际被删除的元素数量
	 */
	public static long zremrangeByLex(Object key, String min, String max) {
		try (Jedis jedis = getJedis()) {
			return jedis.zremrangeByLex(toBytes(key), toBytes(min), toBytes(max));
		}
	}
	
	/**
	 * 删除zset中分数介于min与max之间的元素(包含min与max)
	 * @param key
	 * @param min
	 * @param max
	 * @return 实际被删除的元素数量
	 */
	public static long zremrangeByScore(Object key, double min, double max) {
		try (Jedis jedis = getJedis()) {
			return jedis.zremrangeByScore(toBytes(key), min, max);
		}
	}
	
	/**
	 * 删除zset中分数介于min与max之间的元素
	 * @param key
	 * @param min 必须以(或[开头，[表示包含，(表示不包含。+inf -inf 表示正负无穷大
	 * @param max 必须以(或[开头，[表示包含，(表示不包含。+inf -inf 表示正负无穷大
	 * @return
	 */
	public static long zremrangeByScore(Object key, String min, String max) {
		try (Jedis jedis = getJedis()) {
			return jedis.zremrangeByScore(toBytes(key), toBytes(min), toBytes(max));
		}
	}
	
	/**
	 * 同{@link #zrange(Object, long, long)}原理
	 * @param key
	 * @param start
	 * @param end
	 * @return
	 */
	public static Set<byte[]> zrangeBytes(Object key, long start, long end) {
		try (Jedis jedis = getJedis()) {
			return jedis.zrange(toBytes(key), start, end);
		}
	}
	
	/**
	 * 返回经过排序的zset中位置在start - end之间的元素(包含end)
	 * 若位置为负数，代表从末尾开始计算，例如-1代表倒数第一个，-2代表倒数第二个
	 * 集合中的元素以分数升序排列
	 * 如果分数相同，则以字典排序
	 * @param key
	 * @param start
	 * @param end
	 * @return
	 */
	public static Set<String> zrange(Object key, long start, long end) {
		return toString(zrangeBytes(key, start, end));
	}
	
	/**
	 * 同{@link #zrevrange(Object, long, long)}原理
	 * @param key
	 * @param start
	 * @param end
	 * @return
	 */
	public static Set<byte[]> zrevrangeBytes(Object key, long start, long end) {
		try (Jedis jedis = getJedis()) {
			return jedis.zrevrange(toBytes(key), start, end);
		}
	}
	
	/**
	 * 返回经过降序排列的zset中位置在start - end之间的元素(包含end)
	 * 若位置为负数，代表从末尾开始计算，例如-1代表倒数第一个，-2代表倒数第二个
	 * @param key
	 * @param start
	 * @param end
	 * @return
	 */
	public static Set<String> zrevrange(Object key, long start, long end) {
		return toString(zrevrangeBytes(key, start, end));
	}
	
	/**
	 * 返回经过排序的zset中位置在start - end之间的元素(包含end)
	 * 若位置为负数，代表从末尾开始计算，例如-1代表倒数第一个，-2代表倒数第二个
	 * 集合中的元素以分数升序排列
	 * 如果分数相同，则以字典排序
	 * @param key
	 * @param start
	 * @param end
	 * @return 元素与分数的集合
	 */
	public static Set<Tuple> zrangeWithScores(Object key, long start, long end) {
		try (Jedis jedis = getJedis()) {
			return jedis.zrangeWithScores(toBytes(key), start, end);
		}
	}
	
	/**
	 * 返回经过降序排列的zset中位置在start - end之间的元素(包含end)
	 * 若位置为负数，代表从末尾开始计算，例如-1代表倒数第一个，-2代表倒数第二个
	 * @param key
	 * @param start
	 * @param end
	 * @return 元素与分数的集合
	 */
	public static Set<Tuple> zrevrangeWithScores(Object key, long start, long end) {
		try (Jedis jedis = getJedis()) {
			return jedis.zrevrangeWithScores(toBytes(key), start, end);
		}
	}
	
	/**
	 * 同{@link #zrangeByLex(Object, String, String)}原理
	 * @param key
	 * @param min
	 * @param max
	 * @return
	 */
	public static Set<byte[]> zrangeByLexBytes(Object key, String min, String max) {
		try (Jedis jedis = getJedis()) {
			return jedis.zrangeByLex(toBytes(key), toBytes(min), toBytes(max));
		}
	}
	
	/**
	 * 当元素的分数都相同时，返回按照字典排序介于min与max之间的元素
	 * 当元素的分数不全都相同时，返回结果不可预料
	 * @param key
	 * @param min 必须以(或[开头，[表示包含，(表示不包含。+-表示正负无穷大
	 * @param max 必须以(或[开头，[表示包含，(表示不包含。+-表示正负无穷大
	 * @return
	 */
	public static Set<String> zrangeByLex(Object key, String min, String max) {
		return toString(zrangeByLexBytes(key, min, max));
	}
	
	/**
	 * 同{@link #zrangeByLex(Object, String, String, int, int)}原理
	 * @param key
	 * @param min
	 * @param max
	 * @param offset
	 * @param count
	 * @return
	 */
	public static Set<byte[]> zrangeByLexBytes(Object key, String min, String max, int offset, int count) {
		try (Jedis jedis = getJedis()) {
			return jedis.zrangeByLex(toBytes(key), toBytes(min), toBytes(max), offset, count);
		}
	}
	
	/**
	 * 当元素的分数都相同时，返回按照字典排序介于min与max之间的元素
	 * 当元素的分数不全都相同时，返回结果不可预料
	 * @param key
	 * @param min 必须以(或[开头，[表示包含，(表示不包含。+-表示正负无穷大
	 * @param max 必须以(或[开头，[表示包含，(表示不包含。+-表示正负无穷大
	 * @param offset 类似SELECT LIMIT offset, count in SQL
	 * @param count 类似SELECT LIMIT offset, count in SQL
	 * @return
	 */
	public static Set<String> zrangeByLex(Object key, String min, String max, int offset, int count) {
		return toString(zrangeByLexBytes(key, min, max, offset, count));
	}
	
	/**
	 * 同{@link #zrevrangeByLex(Object, String, String)}原理
	 * @param key
	 * @param max
	 * @param min
	 * @return
	 */
	public static Set<byte[]> zrevrangeByLexBytes(Object key, String max, String min) {
		try (Jedis jedis = getJedis()) {
			return jedis.zrevrangeByLex(toBytes(key), toBytes(max), toBytes(min));
		}
	}
	
	/**
	 * 当元素的分数都相同时，返回按照字典降序排列介于max与min之间的元素
	 * 当元素的分数不全都相同时，返回结果不可预料
	 * @param key
	 * @param max 必须以(或[开头，[表示包含，(表示不包含。+-表示正负无穷大
	 * @param min 必须以(或[开头，[表示包含，(表示不包含。+-表示正负无穷大
	 * @return
	 */
	public static Set<String> zrevrangeByLex(Object key, String max, String min) {
		return toString(zrevrangeByLexBytes(key, max, min));
	}
	
	/**
	 * 同{@link #zrevrangeByLex(Object, String, String, int, int)}原理
	 * @param key
	 * @param max
	 * @param min
	 * @param offset
	 * @param count
	 * @return
	 */
	public static Set<byte[]> zrevrangeByLexBytes(Object key, String max, String min, int offset, int count) {
		try (Jedis jedis = getJedis()) {
			return jedis.zrevrangeByLex(toBytes(key), toBytes(max), toBytes(min), offset, count);
		}
	}
	
	/**
	 * 当元素的分数都相同时，返回按照字典降序排列介于max与min之间的元素
	 * 当元素的分数不全都相同时，返回结果不可预料
	 * @param key
	 * @param max 必须以(或[开头，[表示包含，(表示不包含。+-表示正负无穷大
	 * @param min 必须以(或[开头，[表示包含，(表示不包含。+-表示正负无穷大
	 * @param offset 类似SELECT LIMIT offset, count in SQL
	 * @param count 类似SELECT LIMIT offset, count in SQL
	 * @return
	 */
	public static Set<String> zrevrangeByLex(Object key, String max, String min, int offset, int count) {
		return toString(zrevrangeByLexBytes(key, max, min, offset, count));
	}
	
	/**
	 * 同{@link #zrangeByScore(Object, double, double)}原理
	 * @param key
	 * @param min
	 * @param max
	 * @return
	 */
	public static Set<byte[]> zrangeByScoreBytes(Object key, double min, double max) {
		try (Jedis jedis = getJedis()) {
			return jedis.zrangeByScore(toBytes(key), min, max);
		}
	}
	
	/**
	 * 返回zset中分数介于min与max之间的元素(包含min与max)
	 * @param key
	 * @param min
	 * @param max
	 * @return
	 */
	public static Set<String> zrangeByScore(Object key, double min, double max) {
		return toString(zrangeByScoreBytes(key, min, max));
	}
	
	/**
	 * 同{@link #zrangeByScore(Object, double, double, int, int)}原理
	 * @param key
	 * @param min
	 * @param max
	 * @param offset
	 * @param count
	 * @return
	 */
	public static Set<byte[]> zrangeByScoreBytes(Object key, double min, double max, int offset, int count) {
		try (Jedis jedis = getJedis()) {
			return jedis.zrangeByScore(toBytes(key), min, max, offset, count);
		}
	}
	
	/**
	 * 返回zset中分数介于min与max之间的元素(包含min与max)
	 * @param key
	 * @param min
	 * @param max
	 * @param offset 类似SELECT LIMIT offset, count in SQL
	 * @param count 类似SELECT LIMIT offset, count in SQL
	 * @return
	 */
	public static Set<String> zrangeByScore(Object key, double min, double max, int offset, int count) {
		return toString(zrangeByScoreBytes(key, min, max, offset, count));
	}
	
	/**
	 * 同{@link #zrangeByScore(Object, String, String)}原理
	 * @param key
	 * @param min
	 * @param max
	 * @return
	 */
	public static Set<byte[]> zrangeByScoreBytes(Object key, String min, String max) {
		try (Jedis jedis = getJedis()) {
			return jedis.zrangeByScore(toBytes(key), toBytes(min), toBytes(max));
		}
	}
	
	/**
	 * 返回zset中分数介于min与max之间的元素
	 * @param key
	 * @param min 必须以(或[开头，[表示包含，(表示不包含。+inf -inf 表示正负无穷大
	 * @param max 必须以(或[开头，[表示包含，(表示不包含。+inf -inf 表示正负无穷大
	 * @return
	 */
	public static Set<String> zrangeByScore(Object key, String min, String max) {
		return toString(zrangeByScoreBytes(key, min, max));
	}
	
	/**
	 * 同{@link #zrangeByScore(Object, String, String, int, int)}原理
	 * @param key
	 * @param min
	 * @param max
	 * @param offset
	 * @param count
	 * @return
	 */
	public static Set<byte[]> zrangeByScoreBytes(Object key, String min, String max, int offset, int count) {
		try (Jedis jedis = getJedis()) {
			return jedis.zrangeByScore(toBytes(key), toBytes(min), toBytes(max), offset, count);
		}
	}
	
	/**
	 * 返回zset中分数介于min与max之间的元素
	 * @param key
	 * @param min 必须以(或[开头，[表示包含，(表示不包含。+inf -inf 表示正负无穷大
	 * @param max 必须以(或[开头，[表示包含，(表示不包含。+inf -inf 表示正负无穷大
	 * @param offset 类似SELECT LIMIT offset, count in SQL
	 * @param count 类似SELECT LIMIT offset, count in SQL
	 * @return
	 */
	public static Set<String> zrangeByScore(Object key, String min, String max, int offset, int count) {
		return toString(zrangeByScoreBytes(key, min, max, offset, count));
	}
	
	/**
	 * 同{@link #zrevrangeByScore(Object, double, double)}原理
	 * @param key
	 * @param max
	 * @param min
	 * @return
	 */
	public static Set<byte[]> zrevrangeByScoreBytes(Object key, double max, double min) {
		try (Jedis jedis = getJedis()) {
			return jedis.zrevrangeByScore(toBytes(key), max, min);
		}
	}
	
	/**
	 * 返回zset中分数介于max与min之间的元素(包含max与min)
	 * @param key
	 * @param max
	 * @param min
	 * @return
	 */
	public static Set<String> zrevrangeByScore(Object key, double max, double min) {
		return toString(zrevrangeByScoreBytes(key, max, min));
	}
	
	/**
	 * 同{@link #zrevrangeByScore(Object, double, double, int, int)}原理
	 * @param key
	 * @param max
	 * @param min
	 * @param offset
	 * @param count
	 * @return
	 */
	public static Set<byte[]> zrevrangeByScoreBytes(Object key, double max, double min, int offset, int count) {
		try (Jedis jedis = getJedis()) {
			return jedis.zrevrangeByScore(toBytes(key), max, min, offset, count);
		}
	}
	
	/**
	 * 返回zset中分数介于max与min之间的元素(包含max与min)
	 * @param key
	 * @param max
	 * @param min
	 * @param offset 类似SELECT LIMIT offset, count in SQL
	 * @param count 类似SELECT LIMIT offset, count in SQL
	 * @return
	 */
	public static Set<String> zrevrangeByScore(Object key, double max, double min, int offset, int count) {
		return toString(zrevrangeByScoreBytes(key, max, min, offset, count));
	}
	
	/**
	 * 同{@link #zrevrangeByScore(Object, String, String)}原理
	 * @param key
	 * @param max
	 * @param min
	 * @return
	 */
	public static Set<byte[]> zrevrangeByScoreBytes(Object key, String max, String min) {
		try (Jedis jedis = getJedis()) {
			return jedis.zrevrangeByScore(toBytes(key), toBytes(max), toBytes(min));
		}
	}
	
	/**
	 * 返回zset中分数介于max与min之间的元素
	 * @param key
	 * @param max 必须以(或[开头，[表示包含，(表示不包含。+inf -inf 表示正负无穷大
	 * @param min 必须以(或[开头，[表示包含，(表示不包含。+inf -inf 表示正负无穷大
	 * @return
	 */
	public static Set<String> zrevrangeByScore(Object key, String max, String min) {
		return toString(zrevrangeByScoreBytes(key, max, min));
	}
	
	/**
	 * 同{@link #zrevrangeByScore(Object, String, String, int, int)}原理
	 * @param key
	 * @param max
	 * @param min
	 * @param offset
	 * @param count
	 * @return
	 */
	public static Set<byte[]> zrevrangeByScoreBytes(Object key, String max, String min, int offset, int count) {
		try (Jedis jedis = getJedis()) {
			return jedis.zrevrangeByScore(toBytes(key), toBytes(max), toBytes(min), offset, count);
		}
	}
	
	/**
	 * 返回zset中分数介于max与min之间的元素
	 * @param key
	 * @param max 必须以(或[开头，[表示包含，(表示不包含。+inf -inf 表示正负无穷大
	 * @param min 必须以(或[开头，[表示包含，(表示不包含。+inf -inf 表示正负无穷大
	 * @param offset 类似SELECT LIMIT offset, count in SQL
	 * @param count 类似SELECT LIMIT offset, count in SQL
	 * @return
	 */
	public static Set<String> zrevrangeByScore(Object key, String max, String min, int offset, int count) {
		return toString(zrevrangeByScoreBytes(key, max, min, offset, count));
	}
	
	/**
	 * 返回zset中分数介于min与max之间的元素(包含min与max)
	 * @param key
	 * @param min
	 * @param max
	 * @return 元素与分数的集合
	 */
	public static Set<Tuple> zrangeByScoreWithScores(Object key, double min, double max) {
		try (Jedis jedis = getJedis()) {
			return jedis.zrangeByScoreWithScores(toBytes(key), min, max);
		}
	}
	
	/**
	 * 返回zset中分数介于min与max之间的元素(包含min与max)
	 * @param key
	 * @param min
	 * @param max
	 * @param offset 类似SELECT LIMIT offset, count in SQL
	 * @param count 类似SELECT LIMIT offset, count in SQL
	 * @return
	 */
	public static Set<Tuple> zrangeByScoreWithScores(Object key, double min, double max, int offset, int count) {
		try (Jedis jedis = getJedis()) {
			return jedis.zrangeByScoreWithScores(toBytes(key), min, max, offset, count);
		}
	}
	
	/**
	 * 返回zset中分数介于min与max之间的元素
	 * @param key
	 * @param min 必须以(或[开头，[表示包含，(表示不包含。+inf -inf 表示正负无穷大
	 * @param max 必须以(或[开头，[表示包含，(表示不包含。+inf -inf 表示正负无穷大
	 * @return
	 */
	public static Set<Tuple> zrangeByScoreWithScores(Object key, String min, String max) {
		try (Jedis jedis = getJedis()) {
			return jedis.zrangeByScoreWithScores(toBytes(key), toBytes(min), toBytes(max));
		}
	}
	
	/**
	 * 返回zset中分数介于min与max之间的元素
	 * @param key
	 * @param min 必须以(或[开头，[表示包含，(表示不包含。+inf -inf 表示正负无穷大
	 * @param max 必须以(或[开头，[表示包含，(表示不包含。+inf -inf 表示正负无穷大
	 * @param offset 类似SELECT LIMIT offset, count in SQL
	 * @param count 类似SELECT LIMIT offset, count in SQL
	 * @return
	 */
	public static Set<Tuple> zrangeByScoreWithScores(Object key, String min, String max, int offset, int count) {
		try (Jedis jedis = getJedis()) {
			return jedis.zrangeByScoreWithScores(toBytes(key), toBytes(min), toBytes(max), offset, count);
		}
	}
	
	/**
	 * 返回zset中分数介于max与min之间的元素(包含max与min)
	 * @param key
	 * @param max
	 * @param min
	 * @return
	 */
	public static Set<Tuple> zrevrangeByScoreWithScores(Object key, double max, double min) {
		try (Jedis jedis = getJedis()) {
			return jedis.zrevrangeByScoreWithScores(toBytes(key), max, min);
		}
	}
	
	/**
	 * 返回zset中分数介于max与min之间的元素(包含max与min)
	 * @param key
	 * @param max
	 * @param min
	 * @param offset 类似SELECT LIMIT offset, count in SQL
	 * @param count 类似SELECT LIMIT offset, count in SQL
	 * @return
	 */
	public static Set<Tuple> zrevrangeByScoreWithScores(Object key, double max, double min, int offset, int count) {
		try (Jedis jedis = getJedis()) {
			return jedis.zrevrangeByScoreWithScores(toBytes(key), max, min, offset, count);
		}
	}
	
	/**
	 * 返回zset中分数介于max与min之间的元素
	 * @param key
	 * @param max 必须以(或[开头，[表示包含，(表示不包含。+inf -inf 表示正负无穷大
	 * @param min 必须以(或[开头，[表示包含，(表示不包含。+inf -inf 表示正负无穷大
	 * @return
	 */
	public static Set<Tuple> zrevrangeByScoreWithScores(Object key, String max, String min) {
		try (Jedis jedis = getJedis()) {
			return jedis.zrevrangeByScoreWithScores(toBytes(key), toBytes(max), toBytes(min));
		}
	}
	
	/**
	 * 返回zset中分数介于max与min之间的元素
	 * @param key
	 * @param max 必须以(或[开头，[表示包含，(表示不包含。+inf -inf 表示正负无穷大
	 * @param min 必须以(或[开头，[表示包含，(表示不包含。+inf -inf 表示正负无穷大
	 * @param offset 类似SELECT LIMIT offset, count in SQL
	 * @param count 类似SELECT LIMIT offset, count in SQL
	 * @return
	 */
	public static Set<Tuple> zrevrangeByScoreWithScores(Object key, String max, String min, int offset, int count) {
		try (Jedis jedis = getJedis()) {
			return jedis.zrevrangeByScoreWithScores(toBytes(key), toBytes(max), toBytes(min), offset, count);
		}
	}
	
	/**
	 * 将多个zset的交集存入新的集合
	 * 结果集的分数为各个集合中分数相加
	 * 如果目标key已存在则被覆盖
	 * @param dstkey
	 * @param sets
	 * @return 新集合的元素个数
	 */
	public static long zinterstore(Object dstkey, Object... sets) {
		try (Jedis jedis = getJedis()) {
			return jedis.zinterstore(toBytes(dstkey), toBytes(sets));
		}
	}
	
	/**
	 * 将多个zset的交集存入新的集合
	 * 结果集的分数为各个集合中分数相加
	 * 如果目标key已存在则被覆盖
	 * @param dstkey
	 * @param weights 计算分数时每个原始zset的权重
	 * @param aggregate 计算分数的方式 SUM|MIN|MAX
	 * @param sets
	 * @return 新集合的元素个数
	 */
	public static long zinterstore(Object dstkey, double[] weights, String aggregate, Object... sets) {
		try (Jedis jedis = getJedis()) {
			return jedis.zinterstore(toBytes(dstkey), toZParams(weights, aggregate), toBytes(sets));
		}
	}
	
	/**
	 * 将多个zset的并集存入新的集合
	 * 结果集的分数为各个集合中分数相加
	 * 如果目标key已存在则被覆盖
	 * @param dstkey
	 * @param sets
	 * @return 新集合的元素个数
	 */
	public static long zunionstore(Object dstkey, Object... sets) {
		try (Jedis jedis = getJedis()) {
			return jedis.zunionstore(toBytes(dstkey), toBytes(sets));
		}
	}
	
	/**
	 * 将多个zset的并集存入新的集合
	 * 结果集的分数为各个集合中分数相加
	 * 如果目标key已存在则被覆盖
	 * @param dstkey
	 * @param weights 计算分数时每个原始zset的权重
	 * @param aggregate 计算分数的方式 SUM|MIN|MAX
	 * @param sets
	 * @return 新集合的元素个数
	 */
	public static long zunionstore(Object dstkey, double[] weights, String aggregate, Object... sets) {
		try (Jedis jedis = getJedis()) {
			return jedis.zunionstore(toBytes(dstkey), toZParams(weights, aggregate), toBytes(sets));
		}
	}
	
	/**
	 * 迭代得到zset中所有的元素与其分数
	 * 保证在完整迭代周期内始终存在的元素一定会返回
	 * 保证在完整迭代周期内始终不存在的元素一定不会返回
	 * 其他情况的元素返回性不能保证，有可能不返回，有可能重复返回多次
	 * 一次返回的元素个数不定，有可能为0
	 * @param key
	 * @param cursor 游标以0开始，以0结束
	 * @return
	 */
	public static ScanResult<Tuple> zscan(Object key, String cursor) {
		try (Jedis jedis = getJedis()) {
			return jedis.zscan(toBytes(key), toBytes(cursor));
		}
	}
	
	/**
	 * 迭代得到zset中所有的元素与其分数
	 * 保证在完整迭代周期内始终存在的元素一定会返回
	 * 保证在完整迭代周期内始终不存在的元素一定不会返回
	 * 其他情况的元素返回性不能保证，有可能不返回，有可能重复返回多次
	 * 一次返回的元素个数不定，有可能为0
	 * @param key
	 * @param cursor 游标以0开始，以0结束
	 * @param match 支持*?[ae][a-e][^e]转义为\
	 * @param count 希望一次返回的元素数量，但并不保证
	 * @return
	 */
	public static ScanResult<Tuple> zscan(Object key, String cursor, String match, Integer count) {
		try (Jedis jedis = getJedis()) {
			return jedis.zscan(toBytes(key), toBytes(cursor), toScanParams(match, count));
		}
	}
	
	/**
	 * 向HyperLogLog统计添加一个元素
	 * 相当于sadd命令向集合中添加统计元素
	 * @param key
	 * @param elements
	 * @return 统计数量是否有变化
	 */
	public static boolean pfadd(Object key, Object... elements) {
		try (Jedis jedis = getJedis()) {
			return jedis.pfadd(toBytes(key), toBytes(elements)) == 1L;
		}
	}
	
	/**
	 * 得到HyperLogLog统计结果
	 * 相当于scard命令得到集合的统计数量
	 * @param key
	 * @return
	 */
	public static long pfcount(Object key) {
		try (Jedis jedis = getJedis()) {
			return jedis.pfcount(toBytes(key));
		}
	}
	
	/**
	 * 得到多个HyperLogLog并集之后的统计结果
	 * 相当于sunion与scard命令合并得到集合的统计数量
	 * @param keys
	 * @return
	 */
	public static long pfcount(Object... keys) {
		try (Jedis jedis = getJedis()) {
			return jedis.pfcount(toBytes(keys));
		}
	}
	
	/**
	 * 将多个HyperLogLog的并集存入新的HyperLogLog
	 * @param destkey
	 * @param sourcekeys
	 */
	public static void pfmerge(Object destkey, Object... sourcekeys) {
		try (Jedis jedis = getJedis()) {
			jedis.pfmerge(toBytes(destkey), toBytes(sourcekeys));
		}
	}
	
}
