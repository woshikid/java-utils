package com.github.woshikid.utils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Map;

/**
 * 
 * @author kid
 *
 */
public class ObjectUtils {

	/**
	 * 序列化对象
	 * @param object
	 * @return
	 */
	public static byte[] serialize(Object object) {
		if (object == null) return new byte[0];
		
		try {
			ByteArrayOutputStream byteStream = new ByteArrayOutputStream(1024);
			ObjectOutputStream objectStream = new ObjectOutputStream(byteStream);
			objectStream.writeObject(object);
			objectStream.flush();
			
			return byteStream.toByteArray();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	/**
	 * 反序列化对象
	 * @param bytes
	 * @return
	 */
	public static Object deserialize(byte[] bytes) {
		if (bytes == null || bytes.length == 0) return null;
		
		try {
			ByteArrayInputStream byteStream = new ByteArrayInputStream(bytes);
			ObjectInputStream objectStream = new ObjectInputStream(byteStream);
			
			return objectStream.readObject();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	/**
	 * 深拷贝对象
	 * @param object
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public static <T> T clone(T object) {
		return (T)deserialize(serialize(object));
	}
	
	/**
	 * 根据指定名称查找对应属性，包括父类
	 * 如果找不到则返回null
	 * @param clazz
	 * @param fieldName
	 * @return
	 */
	public static Field getField(Class<?> clazz, String fieldName) {
		//可以加快查找速度
		String internedName = fieldName.intern();
		
		//循环查找该类的属性以及所有父类的属性
		while (clazz != null) {
			Field[] fields = clazz.getDeclaredFields();
			for (Field field : fields) {
				//你没看错，就是用等号的
				if (field.getName() == internedName) return field;
			}
			
			//如果没找到，则查找父类
			clazz = clazz.getSuperclass();
		}
		
		return null;
	}
	
	/**
	 * 得到类的所有属性，包括父类
	 * @param clazz
	 * @return
	 */
	public static Field[] getAllFields(Class<?> clazz) {
		Field[] allFields = clazz.getDeclaredFields();
		
		//循环处理该类的所有父类
		while ((clazz = clazz.getSuperclass()) != null) {
			Field[] newFields = clazz.getDeclaredFields();
			if (newFields.length == 0) continue;
			
			//复制插入父类的属性
			//数组中父类的属性在前，子类的属性在后
			//这样便于在循环中，子类的属性覆盖父类的属性
			Field[] fields = allFields;
			allFields = new Field[newFields.length + fields.length];
			System.arraycopy(newFields, 0, allFields, 0, newFields.length);
			System.arraycopy(fields, 0, allFields, newFields.length, fields.length);
		}
		
		return allFields;
	}
	
	/**
	 * 打印出对象的所有属性
	 * @param object
	 * @return
	 */
	public static String toString(Object object) {
		if (object == null) return "null";
		
		try {
			StringBuilder builder = new StringBuilder();
			
			//判断是否为数组
			if (object.getClass().isArray()) {
				builder.append('[');
				
				//循环输出所有的元素
				for (int i = 0; i < Array.getLength(object); i++) {
					builder.append(Array.get(object, i)).append(", ");
				}
				
				builder.append(']');
			//判断是否为集合类
			} else if(object instanceof Collection) {
				builder.append('[');
				
				//循环输出所有的元素
				for (Object value : (Collection<?>)object) {
					builder.append(value).append(", ");
				}
				
				builder.append(']');
			//判断是否为Map
			} else if(object instanceof Map) {
				builder.append('[');
				
				//循环输出所有的元素
				for (Map.Entry<?, ?> entry : ((Map<?, ?>)object).entrySet()) {
					builder.append(entry.getKey()).append('=');
					builder.append(entry.getValue()).append(", ");
				}
				
				builder.append(']');
			} else {
				//输出类名
				builder.append(object.getClass().getSimpleName());
				builder.append('[');
				
				//循环输出所有的属性
				Field[] fields = getAllFields(object.getClass());
				for (Field field : fields) {
					field.setAccessible(true);
					
					//得到属性值
					String name = field.getName();
					Object value = field.get(object);
					
					//如果为null则不输出
					if (value == null) continue;
					
					//输出单个属性
					builder.append(name).append('=');
					
					//判断是否为可处理类型
					if (value.getClass().isArray()) {
						builder.append(toString(value));
					} else if (value instanceof Collection) {
						builder.append(toString(value));
					} else if (value instanceof Map) {
						builder.append(toString(value));
					} else {
						builder.append(value);
					}
					
					builder.append(", ");
				}
				
				builder.append(']');
			}
			
			return builder.toString();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
}
