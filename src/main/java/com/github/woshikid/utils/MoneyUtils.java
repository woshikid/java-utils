package com.github.woshikid.utils;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 金钱类的计算工具
 * 一般处理BigDecimal
 * @author kid
 *
 */
public class MoneyUtils {

	/**
	 * 返回相应精度的数值
	 * 默认为四舍五入
	 * @param value 原数值
	 * @param scale 精度
	 * @return
	 */
	public static BigDecimal toBigDecimal(String value, int scale) {
		if (value == null) return null;
		return new BigDecimal(value).setScale(scale, RoundingMode.HALF_UP);
	}
	
	/**
	 * 返回精度为2的数值
	 * 默认为四舍五入
	 * @param value
	 * @return
	 */
	public static BigDecimal toBigDecimal(String value) {
		return toBigDecimal(value, 2);
	}
	
	/**
	 * 返回相应精度的数值
	 * 默认为四舍五入
	 * @param value 原数值
	 * @param scale 精度
	 * @return
	 */
	public static BigDecimal toBigDecimal(double value, int scale) {
		return toBigDecimal(String.valueOf(value), scale);
	}
	
	/**
	 * 返回精度为2的数值
	 * 默认为四舍五入
	 * @param value
	 * @return
	 */
	public static BigDecimal toBigDecimal(double value) {
		return toBigDecimal(String.valueOf(value), 2);
	}
	
	/**
	 * 返回相应精度的字符串
	 * @param value
	 * @param scale
	 * @return
	 */
	public static String toString(BigDecimal value, int scale) {
		if (value == null) return null;
		return value.setScale(scale, RoundingMode.HALF_UP).toPlainString();
	}
	
	/**
	 * 返回相应精度的字符串，并去除末尾的0
	 * @param value
	 * @param scale
	 * @return
	 */
	public static String toCleanString(BigDecimal value, int scale) {
		if (value == null) return null;
		return value.setScale(scale, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString();
	}
	
	/**
	 * 返回精度为2的字符串
	 * @param value
	 * @return
	 */
	public static String toString(BigDecimal value) {
		return toString(value, 2);
	}
	
	/**
	 * 返回精度为2的字符串，并去除末尾的0
	 * @param value
	 * @return
	 */
	public static String toCleanString(BigDecimal value) {
		return toCleanString(value, 2);
	}
	
	/**
	 * 返回相应精度的字符串
	 * @param value
	 * @param scale
	 * @return
	 */
	public static String toString(String value, int scale) {
		return toBigDecimal(value, scale).toPlainString();
	}
	
	/**
	 * 返回相应精度的字符串，并去除末尾的0
	 * @param value
	 * @param scale
	 * @return
	 */
	public static String toCleanString(String value, int scale) {
		return toBigDecimal(value, scale).stripTrailingZeros().toPlainString();
	}
	
	/**
	 * 返回精度为2的字符串
	 * @param value
	 * @return
	 */
	public static String toString(String value) {
		return toString(value, 2);
	}
	
	/**
	 * 返回精度为2的字符串，并去除末尾的0
	 * @param value
	 * @return
	 */
	public static String toCleanString(String value) {
		return toCleanString(value, 2);
	}
	
	/**
	 * 返回相应精度的字符串
	 * @param value
	 * @param scale
	 * @return
	 */
	public static String toString(double value, int scale) {
		return toString(String.valueOf(value), scale);
	}
	
	/**
	 * 返回相应精度的字符串，并去除末尾的0
	 * @param value
	 * @param scale
	 * @return
	 */
	public static String toCleanString(double value, int scale) {
		return toCleanString(String.valueOf(value), scale);
	}
	
	/**
	 * 返回精度为2的字符串
	 * @param value
	 * @return
	 */
	public static String toString(double value) {
		return toString(value, 2);
	}
	
	/**
	 * 返回精度为2的字符串，并去除末尾的0
	 * @param value
	 * @return
	 */
	public static String toCleanString(double value) {
		return toCleanString(value, 2);
	}
	
	/**
	 * 乘法，按照精度返回字符串
	 * @param value1
	 * @param value2
	 * @param scale
	 * @return
	 */
	public static String multiply(BigDecimal value1, BigDecimal value2, int scale) {
		return toString(value1.multiply(value2), scale);
	}
	
	/**
	 * 乘法，返回精度为2的字符串
	 * @param value1
	 * @param value2
	 * @return
	 */
	public static String multiply(BigDecimal value1, BigDecimal value2) {
		return multiply(value1, value2, 2);
	}
	
}
