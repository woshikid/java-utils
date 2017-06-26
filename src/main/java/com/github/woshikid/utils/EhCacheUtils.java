package com.github.woshikid.utils;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;

/**
 * 
 * @author kid
 *
 */
public class EhCacheUtils {

	public static Object get(String cacheName, Object key) {
		CacheManager cacheManager = CacheManager.getInstance();
		Cache cache = cacheManager.getCache(cacheName);
		if (cache == null) return null;
		return get(cache, key);
	}
	
	public static Object get(Ehcache cache, Object key) {
		Element element = cache.get(key);
		if (element == null) return null;
		return element.getObjectValue();
	}
	
	public static void set(String cacheName, Object key, Object value) {
		CacheManager cacheManager = CacheManager.getInstance();
		Ehcache cache = cacheManager.addCacheIfAbsent(cacheName);
		set(cache, key, value);
	}
	
	public static void set(Ehcache cache, Object key, Object value) {
		cache.put(new Element(key, value));
	}
	
	public static Object setIfAbsent(String cacheName, Object key, Object value) {
		CacheManager cacheManager = CacheManager.getInstance();
		Ehcache cache = cacheManager.addCacheIfAbsent(cacheName);
		return setIfAbsent(cache, key, value);
	}
	
	public static Object setIfAbsent(Ehcache cache, Object key, Object value) {
		Element element = cache.putIfAbsent(new Element(key, value));
		if (element == null) return null;
		return element.getObjectValue();
	}
	
	public static boolean remove(String cacheName, Object key) {
		CacheManager cacheManager = CacheManager.getInstance();
		Cache cache = cacheManager.getCache(cacheName);
		if (cache == null) return false;
		return remove(cache, key);
	}
	
	public static boolean remove(Ehcache cache, Object key) {
		return cache.remove(key);
	}
	
	public static void removeAll(String cacheName) {
		CacheManager cacheManager = CacheManager.getInstance();
		Cache cache = cacheManager.getCache(cacheName);
		if (cache == null) return;
		removeAll(cache);
	}
	
	public static void removeAll(Ehcache cache) {
		cache.removeAll();
	}
	
	public static Object update(String cacheName, Object key, Object value) {
		CacheManager cacheManager = CacheManager.getInstance();
		Cache cache = cacheManager.getCache(cacheName);
		if (cache == null) return null;
		return update(cache, key, value);
	}
	
	public static Object update(Ehcache cache, Object key, Object value) {
		Element element = cache.replace(new Element(key, value));
		if (element == null) return null;
		return element.getObjectValue();
	}
	
}
