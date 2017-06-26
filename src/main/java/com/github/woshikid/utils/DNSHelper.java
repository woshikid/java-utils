package com.github.woshikid.utils;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.net.InetAddress;
import java.util.Map;

/**
 * 
 * @author kid
 *
 */
public class DNSHelper {

	private static Map<String, Object> cacheMap = null;
	private static Constructor<?> entryConstructor = null;
	
	@SuppressWarnings("unchecked")
	private static void init() throws Exception {
		if (cacheMap != null) return;
		
		Field cacheField = InetAddress.class.getDeclaredField("addressCache");
		cacheField.setAccessible(true);
		Object addressCache = cacheField.get(null);
		Field cacheMapField = addressCache.getClass().getDeclaredField("cache");
		cacheMapField.setAccessible(true);
		cacheMap = (Map<String, Object>)cacheMapField.get(addressCache);
		
		entryConstructor = Class.forName("java.net.InetAddress$CacheEntry").getDeclaredConstructors()[0];
		entryConstructor.setAccessible(true);
	}
	
	public synchronized static void setHost(String host, String ip) throws Exception {
		init();
		
		Object cacheEntry = entryConstructor.newInstance(new InetAddress[]{InetAddress.getByName(ip)}, -1);
		cacheMap.put(host, cacheEntry);
	}
	
	public synchronized static void removeHost(String host) throws Exception {
		init();
		
		cacheMap.remove(host);
	}
}
