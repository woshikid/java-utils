package com.github.woshikid.utils;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 改进之后的bean复制工具
 * @author kid
 *
 */
public class DTOUtils {

	/**
	 * 当前线程指定的日期格式
	 */
	private static final ThreadLocal<String> dateFormat = new ThreadLocal<>();
	
	/**
	 * 默认的日期时间格式
	 */
	private static String defaultDateFormat = DateUtils.DATETIME_FORMAT;
	
	/**
	 * 当前线程指定的小数精度
	 */
	private static final ThreadLocal<Integer> bigDecimalScale = new ThreadLocal<>();
	
	/**
	 * 得到当前线程指定的日期格式
	 * @return
	 */
	private static String getDateFormat() {
		//从当前线程变量中取得日期格式
		String format = dateFormat.get();
		//如果没有设置，则使用默认日期格式
		if (format == null) format = defaultDateFormat;
		return format;
	}
	
	/**
	 * 得到当前线程指定的小数精度
	 * @return
	 */
	private static Integer getBigDecimalScale() {
		return bigDecimalScale.get();
	}
	
	/**
	 * 设置当前线程指定的日期格式
	 * @param format
	 */
	public static void setDateFormat(String format) {
		dateFormat.set(format);
	}
	
	/**
	 * 设置当前线程指定的小数精度
	 * @param scale
	 */
	public static void setBigDecimalScale(int scale) {
		bigDecimalScale.set(scale);
	}
	
	/**
	 * 清除ThreadLocal格式信息
	 */
	public static void reset() {
		dateFormat.remove();
		bigDecimalScale.remove();
	}
	
	/**
	 * 类型转换函数
	 * 如果无法转换则返回null
	 * @param source 包装后的类型
	 * @param targetClass
	 * @return
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static <T> T convert(Object source, Class<T> targetClass) {
		if (source == null) return null;
		
		if (targetClass.isInstance(source)) {
			return (T)source;
		}
		
		if (source.equals("")) return null;
		
		if (targetClass == boolean.class || targetClass == Boolean.class) {
			if (source instanceof Boolean) {
				return (T)source;
			} else if (source instanceof Number) {
				return (T)(Boolean)(((Number)source).intValue() != 0);
			} else if (source instanceof String) {
				return (T)Boolean.valueOf((String)source);
			}
		} else if (targetClass == char.class || targetClass == Character.class) {
			if (source instanceof Character) {
				return (T)source;
			} else if (source instanceof String) {
				if (((String)source).length() == 0) return null;
				return (T)Character.valueOf(((String)source).charAt(0));
			}
		} else if (targetClass == byte.class || targetClass == Byte.class) {
			if (source instanceof Number) {
				return (T)Byte.valueOf(((Number)source).byteValue());
			} else if (source instanceof String) {
				return (T)Byte.valueOf((String)source);
			}
		} else if (targetClass == short.class || targetClass == Short.class) {
			if (source instanceof Number) {
				return (T)Short.valueOf(((Number)source).shortValue());
			} else if (source instanceof String) {
				return (T)Short.valueOf((String)source);
			}
		} else if (targetClass == int.class || targetClass == Integer.class) {
			if (source instanceof Number) {
				return (T)Integer.valueOf(((Number)source).intValue());
			} else if (source instanceof String) {
				return (T)Integer.valueOf((String)source);
			}
		} else if (targetClass == long.class || targetClass == Long.class) {
			if (source instanceof Number) {
				return (T)Long.valueOf(((Number)source).longValue());
			} else if (source instanceof String) {
				return (T)Long.valueOf((String)source);
			} else if (source instanceof Date) {
				return (T)(Long)((Date)source).getTime();
			} else if (source instanceof Calendar) {
				return (T)(Long)((Calendar)source).getTimeInMillis();
			} else if (source instanceof LocalDate) {
				return (T)(Long)DateUtils.toInstant((LocalDate)source).toEpochMilli();
			} else if (source instanceof LocalDateTime) {
				return (T)(Long)DateUtils.toInstant((LocalDateTime)source).toEpochMilli();
			} else if (source instanceof Instant) {
				return (T)(Long)((Instant)source).toEpochMilli();
			}
		} else if (targetClass == float.class || targetClass == Float.class) {
			if (source instanceof Number) {
				return (T)Float.valueOf(((Number)source).floatValue());
			} else if (source instanceof String) {
				return (T)Float.valueOf((String)source);
			}
		} else if (targetClass == double.class || targetClass == Double.class) {
			if (source instanceof Number) {
				return (T)Double.valueOf(((Number)source).doubleValue());
			} else if (source instanceof String) {
				return (T)Double.valueOf((String)source);
			}
		} else if (targetClass == BigInteger.class) {
			if (source instanceof BigDecimal) {
				return (T)((BigDecimal)source).toBigInteger();
			} else if (source instanceof Number) {
				return (T)BigInteger.valueOf(((Number)source).longValue());
			} else if (source instanceof String) {
				return (T)new BigDecimal((String)source).toBigInteger();
			}
		} else if (targetClass == BigDecimal.class) {
			if (source instanceof Number || source instanceof String) {
				Integer scale = getBigDecimalScale();
				if (scale == null) {
					return (T)new BigDecimal(source.toString());
				} else {
					return (T)new BigDecimal(source.toString()).setScale(scale, RoundingMode.HALF_UP);
				}
			}
		} else if (targetClass == Date.class) {
			if (source instanceof Calendar) {
				return (T)((Calendar)source).getTime();
			} else if (source instanceof Long) {
				return (T)new Date((Long)source);
			} else if (source instanceof String) {
				return (T)DateUtils.toDate((String)source);
			} else if (source instanceof LocalDate) {
				return (T)DateUtils.toDate((LocalDate)source);
			} else if (source instanceof LocalDateTime) {
				return (T)DateUtils.toDate((LocalDateTime)source);
			} else if (source instanceof Instant) {
				return (T)Date.from((Instant)source);
			}
		} else if (targetClass == Calendar.class) {
			if (source instanceof Date) {
				return (T)DateUtils.toCalendar((Date)source);
			} else if (source instanceof Long) {
				return (T)DateUtils.toCalendar((Long)source);
			} else if (source instanceof String) {
				return (T)DateUtils.toCalendar((String)source);
			} else if (source instanceof LocalDate) {
				return (T)DateUtils.toCalendar((LocalDate)source);
			} else if (source instanceof LocalDateTime) {
				return (T)DateUtils.toCalendar((LocalDateTime)source);
			} else if (source instanceof Instant) {
				return (T)DateUtils.toCalendar((Instant)source);
			}
		} else if (targetClass == LocalDate.class) {
			if (source instanceof Long) {
				return (T)DateUtils.toLocalDate((Long)source);
			} else if (source instanceof Date) {
				return (T)DateUtils.toLocalDate((Date)source);
			} else if (source instanceof Calendar) {
				return (T)DateUtils.toLocalDate(((Calendar)source).getTime());
			} else if (source instanceof String) {
				return (T)DateUtils.toLocalDate((String)source);
			} else if (source instanceof LocalDateTime) {
				return (T)((LocalDateTime)source).toLocalDate();
			} else if (source instanceof Instant) {
				return (T)DateUtils.toLocalDate((Instant)source);
			}
		} else if (targetClass == LocalTime.class) {
			if (source instanceof Long) {
				return (T)DateUtils.toLocalTime((Long)source);
			} else if (source instanceof Date) {
				return (T)DateUtils.toLocalTime((Date)source);
			} else if (source instanceof Calendar) {
				return (T)DateUtils.toLocalTime(((Calendar)source).getTime());
			} else if (source instanceof String) {
				return (T)DateUtils.toLocalTime((String)source);
			} else if (source instanceof LocalDateTime) {
				return (T)((LocalDateTime)source).toLocalTime();
			} else if (source instanceof Instant) {
				return (T)DateUtils.toLocalTime((Instant)source);
			}
		} else if (targetClass == LocalDateTime.class) {
			if (source instanceof Long) {
				return (T)DateUtils.toLocalDateTime((Long)source);
			} else if (source instanceof Date) {
				return (T)DateUtils.toLocalDateTime((Date)source);
			} else if (source instanceof Calendar) {
				return (T)DateUtils.toLocalDateTime(((Calendar)source).getTime());
			} else if (source instanceof String) {
				return (T)DateUtils.toLocalDateTime((String)source);
			} else if (source instanceof LocalDate) {
				return (T)DateUtils.toLocalDateTime((LocalDate)source);
			} else if (source instanceof Instant) {
				return (T)DateUtils.toLocalDateTime((Instant)source);
			}
		} else if (targetClass == Instant.class) {
			if (source instanceof Long) {
				return (T)Instant.ofEpochMilli((Long)source);
			} else if (source instanceof Date) {
				return (T)((Date)source).toInstant();
			} else if (source instanceof Calendar) {
				return (T)((Calendar)source).getTime().toInstant();
			} else if (source instanceof String) {
				return (T)DateUtils.toInstant((String)source);
			} else if (source instanceof LocalDate) {
				return (T)DateUtils.toInstant((LocalDate)source);
			} else if (source instanceof LocalDateTime) {
				return (T)DateUtils.toInstant((LocalDateTime)source);
			}
		} else if (targetClass == String.class) {
			if (source instanceof Date) {
				return (T)DateUtils.toString((Date)source, getDateFormat());
			} else if (source instanceof Calendar) {
				return (T)DateUtils.toString(((Calendar)source).getTime(), getDateFormat());
			} else if (source instanceof LocalTime) {
				return (T)DateUtils.toTimeString((LocalTime)source);
			} else if (source instanceof LocalDateTime) {
				return (T)DateUtils.toString((LocalDateTime)source, getDateFormat());
			} else if (source instanceof Instant) {
				return (T)DateUtils.toString((Instant)source, getDateFormat());
			} else if (source instanceof BigDecimal) {
				Integer scale = getBigDecimalScale();
				if (scale == null) {
					return (T)((BigDecimal)source).toPlainString();
				} else {
					return (T)((BigDecimal)source).setScale(scale, RoundingMode.HALF_UP).toPlainString();
				}
			} else {
				return (T)source.toString();
			}
		} else if (Enum.class.isAssignableFrom(targetClass)) {
			if (source instanceof Enum) {
				return (T)Enum.valueOf((Class<Enum>)targetClass, ((Enum<?>)source).name());
			} else if (source instanceof String) {
				return (T)Enum.valueOf((Class<Enum>)targetClass, (String)source);
			}
		}
		
		return null;
	}
	
	/**
	 * 把一个bean的属性复制到另一个bean
	 * source兼容Map
	 * @param source
	 * @param target
	 * @return
	 */
	public static <T> T map(Object source, T target) {
		if (source instanceof Map) {
			return map((Map<?, ?>)source, target, true);
		} else {
			return map(source, target, true);
		}
	}
	
	/**
	 * 把一个bean的属性复制到另一个bean
	 * @param source
	 * @param target
	 * @param reset 是否清除ThreadLocal格式信息
	 * @return
	 */
	private static <T> T map(Object source, T target, boolean reset) {
		try {
			if (source == null) return target;
			if (target == null) return target;
			
			Field[] fields = ObjectUtils.getAllFields(target.getClass());
			for (Field field : fields) {
				if (Modifier.isFinal(field.getModifiers())) continue;
				
				Field sourceField = ObjectUtils.getField(source.getClass(), field.getName());
				if (sourceField == null) continue;
				
				sourceField.setAccessible(true);
				Object value = sourceField.get(source);
				value = convert(value, field.getType());
				
				if (value != null) {
					field.setAccessible(true);
					field.set(target, value);
				}
			}
			
			return target;
		} catch (Exception e) {
			throw new RuntimeException(e);
		} finally {
			if (reset) reset();
		}
	}
	
	/**
	 * 从map中组装bean
	 * @param source
	 * @param target
	 * @param reset 是否清除ThreadLocal格式信息
	 * @return
	 */
	private static <T> T map(Map<?, ?> source, T target, boolean reset) {
		try {
			if (source == null) return target;
			if (target == null) return target;
			
			Field[] fields = ObjectUtils.getAllFields(target.getClass());
			for (Field field : fields) {
				if (Modifier.isFinal(field.getModifiers())) continue;
				
				Object value = source.get(field.getName());
				value = convert(value, field.getType());
				
				if (value != null) {
					field.setAccessible(true);
					field.set(target, value);
				}
			}
			
			return target;
		} catch (Exception e) {
			throw new RuntimeException(e);
		} finally {
			if (reset) reset();
		}
	}
	
	/**
	 * 根据给定的类复制bean
	 * source兼容Map
	 * @param source
	 * @param targetClass
	 * @return
	 */
	public static <T> T map(Object source, Class<T> targetClass) {
		return map(source, targetClass, true);
	}
	
	/**
	 * 根据给定的类复制bean
	 * source兼容Map
	 * @param source
	 * @param targetClass
	 * @param reset 是否清除ThreadLocal格式信息
	 * @return
	 */
	private static <T> T map(Object source, Class<T> targetClass, boolean reset) {
		try {
			T target = targetClass.getDeclaredConstructor().newInstance();
			
			if (source instanceof Map) {
				map((Map<?, ?>)source, target, reset);
			} else {
				map(source, target, reset);
			}
			
			return target;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	/**
	 * 根据指定的类复制整个bean数组
	 * source兼容Map
	 * @param sourceList
	 * @param targetClass
	 * @return
	 */
	public static <T> List<T> map(List<?> sourceList, Class<T> targetClass) {
		try {
			List<T> list = new ArrayList<>(sourceList.size());
			
			for (Object source : sourceList) {
				list.add(map(source, targetClass, false));
			}
			
			return list;
		} finally {
			reset();
		}
	}
	
	/**
	 * 将bean转换成map
	 * @param source
	 * @return
	 */
	public static Map<String, Object> map(Object source) {
		return map(source, false, true);
	}
	
	/**
	 * 将bean转换成map
	 * @param source
	 * @param toString 是否将所有属性转换成字符串
	 * @return
	 */
	public static Map<String, Object> map(Object source, boolean toString) {
		return map(source, toString, true);
	}
	
	/**
	 * 将bean转换成map
	 * @param source
	 * @param toString 是否将所有属性转换成字符串
	 * @param reset 是否清除ThreadLocal格式信息
	 * @return
	 */
	private static Map<String, Object> map(Object source, boolean toString, boolean reset) {
		try {
			if (source == null) return null;
			
			Map<String, Object> map = new HashMap<>();
			Field[] fields = ObjectUtils.getAllFields(source.getClass());
			
			for (Field field : fields) {
				field.setAccessible(true);
				Object value = field.get(source);
				if (toString) value = convert(value, String.class);
				if (value != null) map.put(field.getName(), value);
			}
			
			return map;
		} catch (Exception e) {
			throw new RuntimeException(e);
		} finally {
			if (reset) reset();
		}
	}
	
	/**
	 * 将bean数组转换成map数组
	 * @param sourceList
	 * @return
	 */
	public static List<Map<String, Object>> map(List<?> sourceList) {
		return map(sourceList, false);
	}
	
	/**
	 * 将bean数组转换成map数组
	 * @param sourceList
	 * @param toString 是否将所有属性转换成字符串
	 * @return
	 */
	public static List<Map<String, Object>> map(List<?> sourceList, boolean toString) {
		try {
			List<Map<String, Object>> list = new ArrayList<>(sourceList.size());
			
			for (Object source : sourceList) {
				list.add(map(source, toString, false));
			}
			
			return list;
		} finally {
			reset();
		}
	}
	
	/**
	 * 检查参数是否为null
	 * @param objects
	 * @return 有null返回false
	 */
	public static boolean checkNull(Object... objects) {
		for (Object object : objects) {
			if (object == null) return false;
		}
		
		return true;
	}
	
	/**
	 * 检查bean中的属性是否为null
	 * @param object
	 * @return 有null返回false
	 */
	public static boolean checkNullFields(Object object) {
		return checkNullFields(object, false);
	}
	
	/**
	 * 检查bean中的属性是否为null
	 * @param object
	 * @param superclass 是否检查父类
	 * @return 有null返回false
	 */
	public static boolean checkNullFields(Object object, boolean superclass) {
		try {
			Field[] fields;
			if (superclass) {
				fields = ObjectUtils.getAllFields(object.getClass());
			} else {
				fields = object.getClass().getDeclaredFields();
			}
			
			for (Field field : fields) {
				field.setAccessible(true);
				Object value = field.get(object);
				if (value == null) return false;
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		
		return true;
	}
	
	/**
	 * 检查参数是否为null
	 * 如果为null则抛出异常
	 * @param objects
	 */
	public static void assertNotNull(Object... objects) {
		if (!checkNull(objects)) throw new IllegalArgumentException();
	}
	
	/**
	 * 检查bean中的属性是否为null
	 * 如果为null则抛出异常
	 * @param object
	 */
	public static void assertNoNullFields(Object object) {
		if (!checkNullFields(object, false)) throw new IllegalArgumentException();
	}
	
	/**
	 * 检查bean中的属性是否为null
	 * 如果为null则抛出异常
	 * @param object
	 * @param superclass 是否检查父类
	 */
	public static void assertNoNullFields(Object object, boolean superclass) {
		if (!checkNullFields(object, superclass)) throw new IllegalArgumentException();
	}
	
	/**
	 * 将下划线风格替换为驼峰风格
	 * @param name
	 * @return
	 */
	public static String underscoreToCamelCase(String name) {
		Matcher matcher = Pattern.compile("_[a-z]").matcher(name);
		StringBuilder builder = new StringBuilder(name);
		
		for (int i = 0; matcher.find(); i++) {
			builder.replace(matcher.start() - i, matcher.end() - i, matcher.group().substring(1).toUpperCase());
		}
		
		if (Character.isUpperCase(builder.charAt(0))) {
			builder.replace(0, 1, String.valueOf(Character.toLowerCase(builder.charAt(0))));
		}
		
		return builder.toString();
	}
	
	/**
	 * 将map中的key转换为驼峰风格
	 * @param map
	 * @return
	 */
	public static <V> Map<String, V> camelCaseMap(Map<String, V> map) {
		Map<String, V> camelHumpMap = new HashMap<>();
		
		Iterator<Map.Entry<String, V>> iterator = map.entrySet().iterator();
		while (iterator.hasNext()) {
			Map.Entry<String, V> entry = iterator.next();
			String key = entry.getKey();
			String camelHumpKey = underscoreToCamelCase(key.toLowerCase());
			
			if (!key.equals(camelHumpKey)) {
				camelHumpMap.put(camelHumpKey, entry.getValue());
				iterator.remove();
			}
		}
		
		map.putAll(camelHumpMap);
		return map;
	}
	
	/**
	 * 将数组中map的key转换为驼峰风格
	 * @param mapList
	 * @return
	 */
	public static <V> List<Map<String, V>> camelCaseMap(List<Map<String, V>> mapList) {
		for (Map<String, V> map : mapList) {
			camelCaseMap(map);
		}
		
		return mapList;
	}
	
}
