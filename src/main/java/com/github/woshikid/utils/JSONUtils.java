package com.github.woshikid.utils;

import java.util.Date;
import java.util.List;
import java.util.Map;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializeConfig;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.alibaba.fastjson.serializer.SimpleDateFormatSerializer;

/**
 * json转换工具
 * @author kid
 *
 */
public class JSONUtils {

	/**
	 * 类型转换配置
	 */
	private static SerializeConfig mapping = new SerializeConfig();
	
	/**
	 * 配置默认日期格式
	 */
	static {
		mapping.put(Date.class, new SimpleDateFormatSerializer("yyyy-MM-dd HH:mm:ss"));
	}
	
	/**
	 * 将对象转换为json字符串
	 * 兼容List与Map
	 * @param object
	 * @return
	 */
	public static String toJSON(Object object) {
		return JSON.toJSONString(object, mapping);
	}
	
	/**
	 * 将对象转换为json字符串
	 * 兼容List与Map
	 * @param object
	 * @param prettyFormat
	 * @return
	 */
	public static String toJSON(Object object, boolean prettyFormat) {
		if (prettyFormat) {
			return JSON.toJSONString(object, mapping, SerializerFeature.PrettyFormat);
		} else {
			return JSON.toJSONString(object, mapping);
		}
	}
	
	/**
	 * 将对象转换为json字符串
	 * 兼容List与Map
	 * @param object
	 * @param dateFormat
	 * @return
	 */
	public static String toJSON(Object object, String dateFormat) {
		SerializeConfig mapping = new SerializeConfig();
		mapping.put(Date.class, new SimpleDateFormatSerializer(dateFormat));
		return JSON.toJSONString(object, mapping);
	}
	
	/**
	 * 将对象转换为json字符串
	 * 兼容List与Map
	 * @param object
	 * @param dateFormat
	 * @param prettyFormat
	 * @return
	 */
	public static String toJSON(Object object, String dateFormat, boolean prettyFormat) {
		SerializeConfig mapping = new SerializeConfig();
		mapping.put(Date.class, new SimpleDateFormatSerializer(dateFormat));
		
		if (prettyFormat) {
			return JSON.toJSONString(object, mapping, SerializerFeature.PrettyFormat);
		} else {
			return JSON.toJSONString(object, mapping);
		}
	}
	
	/**
	 * 将json字符串转换为Map或List
	 * @param json
	 * @return
	 */
	public static Object toObject(String json) {
		return JSON.parse(json);
	}
	
	/**
	 * 将json字符串转换为对象
	 * @param json
	 * @param targetClass
	 * @return
	 */
	public static <T> T toObject(String json, Class<T> targetClass) {
		return JSON.parseObject(json, targetClass);
	}
	
	/**
	 * 将map转换为对象
	 * @param map
	 * @param targetClass
	 * @return
	 */
	public static <T> T toObject(Map<?, ?> map, Class<T> targetClass) {
		String json = toJSON(map);
		return toObject(json, targetClass);
	}
	
	/**
	 * 将json字符串转换为Map
	 * @param json
	 * @return
	 */
	public static Map<String, Object> toMap(String json) {
		return JSON.parseObject(json);
	}
	
	/**
	 * 将对象转换为map
	 * @param object
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public static Map<String, Object> toMap(Object object) {
		return (Map<String, Object>)JSON.toJSON(object);
	}
	
	/**
	 * 将json字符串转换为List
	 * @param json
	 * @param targetClass
	 * @return
	 */
	public static <T> List<T> toList(String json, Class<T> targetClass) {
		return JSON.parseArray(json, targetClass);
	}
	
	/**
	 * 将json字符串转换为List
	 * @param json
	 * @return
	 */
	public static List<Object> toList(String json) {
		return JSON.parseArray(json);
	}
	
}
