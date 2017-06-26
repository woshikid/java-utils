package com.github.woshikid.utils;

import java.util.Date;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;

/**
 * 身份证/ID相关工具类
 * @author kid
 *
 */
public class IDUtils {

	//身份证验证系数
	private static final int[] IDPowers = new int[] {7, 9, 10, 5, 8, 4, 2, 1, 6, 3, 7, 9, 10, 5, 8, 4, 2};
	
	//身份证验证码
	private static final String[] IDValidations = new String[] {"1", "0", "X", "9", "8", "7", "6", "5", "4", "3", "2"};
	
	/**
	 * 判断身份证号是否有效
	 * @param IDNo
	 * @return
	 */
	public static boolean isValid(String IDNo) {
		if (StringUtils.isEmpty(IDNo)) return false;
		if (!IDNo.matches("^[1-9]\\d{5}(19|20)\\d{9}[\\dxX]$")) return false;
		
		try {
			getBirthday(IDNo);
		} catch (Exception e) {
			return false;
		}
		
		int sum = 0;
		for (int i = 0; i < 17; i++) {
			int num = Integer.valueOf(IDNo.substring(i, i + 1));
			sum += num * IDPowers[i];
		}
		
		String validation = IDValidations[sum % 11];
		return IDNo.endsWith(validation);
	}
	
	/**
	 * 从身份证中取得生日信息
	 * @param IDNo
	 * @return
	 */
	public static String getBirthdayString(String IDNo) {
		return IDNo.substring(6, 14);
	}
	
	/**
	 * 从身份证中取得生日信息
	 * @param IDNo
	 * @return
	 */
	public static Date getBirthday(String IDNo) {
		return DateUtils.toDate(getBirthdayString(IDNo));
	}
	
	/**
	 * 从身份证中取得年龄
	 * @param IDNo
	 * @return
	 */
	public static int getAge(String IDNo) {
		return DateUtils.getYearInterval(getBirthday(IDNo), new Date());
	}

	/**
	 * 根据身份证判断是否为男性
	 * @param IDNo
	 * @return
	 */
	public static boolean isMale(String IDNo) {
		return !isFemale(IDNo);
	}
	
	/**
	 * 根据身份证判断是否为女性
	 * @param IDNo
	 * @return
	 */
	public static boolean isFemale(String IDNo) {
		String flag = IDNo.substring(16, 17);
		return (Integer.valueOf(flag) % 2) == 0;
	}

	/**
	 * 生成指定长度的sn
	 * 不足的部分用0前置填充
	 * 超出部分截取末尾
	 * @param sn 原始序列号
	 * @param length 指定长度
	 * @return
	 */
	public static String getSN(String sn, int length) {
		sn = StringUtils.leftPad(sn, length, '0');
		return StringUtils.right(sn, length);
	}

	/**
	 * 生成随机uuid
	 * @return
	 */
	public static String uuid() {
		return UUID.randomUUID().toString().replace("-", "");
	}
	
}
