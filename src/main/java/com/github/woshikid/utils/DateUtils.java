package com.github.woshikid.utils;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

/**
 * 日期计算工具类
 * @author kid
 *
 */
public class DateUtils {

	//所有程序中统一规定的日期格式
	public static final String DATE_FORMAT = "yyyy-MM-dd";
	public static final String DATETIME_FORMAT = "yyyy-MM-dd HH:mm:ss";
	public static final String FULLTIME_FORMAT = "yyyy-MM-dd HH:mm:ss.SSS";
	public static final String TIMESTAMP_FORMAT = "yyyyMMddHHmmssSSS";

	//当前线程指定的宽容模式
	private static final ThreadLocal<Boolean> threadLenient = new ThreadLocal<>();
	
	/**
	 * 设置当前线程指定的宽容模式
	 * @param lenient
	 */
	public static void setLenient(boolean lenient) {
		threadLenient.set(lenient);
	}
	
	/**
	 * 得到当前线程指定的宽容模式
	 * 并清除数据，默认为false
	 * @return
	 */
	private static boolean getLenient() {
		Boolean lenient = threadLenient.get();
		
		if (lenient == null) {
			return false;
		} else {
			threadLenient.remove();
			return lenient;
		}
	}
	
	/**
	 * 得到当前日期
	 * @return
	 */
	public static String today() {
		return toDateString(new Date());
	}
	
	/**
	 * 得到当前时分秒
	 * @return
	 */
	public static String now() {
		return toDateTimeString(new Date());
	}
	
	/**
	 * 得到当前时分秒毫秒
	 * @return
	 */
	public static String nowMillis() {
		return toFullTimeString(new Date());
	}

	/**
	 * 得到当前时间戳
	 * @return
	 */
	public static String timestamp() {
		return toTimestampString(new Date());
	}
	
	/**
	 * 根据String时间得到Date类型的时间
	 * @param date
	 * @return
	 */
	public static Date toDate(String date) {
		if (date == null) return null;
		date = date.trim();
		
		//根据字符串内容自动判断日期格式
		SimpleDateFormat format;
		if (date.indexOf("-") == -1) {
			format = new SimpleDateFormat(TIMESTAMP_FORMAT.substring(0, date.length()));
		} else {
			if (date.indexOf(" ") == -1) {
				format = new SimpleDateFormat(DATE_FORMAT);
			} else{
				if (date.indexOf(".") == -1) {
					format = new SimpleDateFormat(DATETIME_FORMAT);
				} else {
					format = new SimpleDateFormat(FULLTIME_FORMAT);
				}
			}
		}

		//设置宽容模式，本工具默认设置为关闭
		//不合法的日期将抛出异常
		boolean lenient = getLenient();
		format.setLenient(lenient);
		
		try {
			return format.parse(date);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	/**
	 * 根据long得到Calendar类型的时间
	 * @param date
	 * @return
	 */
	public static Calendar toCalendar(long millis) {
		Calendar calendar = Calendar.getInstance();
		calendar.setTimeInMillis(millis);
		return calendar;
	}
	
	/**
	 * 根据Date得到Calendar类型的时间
	 * @param date
	 * @return
	 */
	public static Calendar toCalendar(Date date) {
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(date);
		return calendar;
	}
	
	/**
	 * 根据String时间得到Calendar类型的时间
	 * @param date
	 * @return
	 */
	public static Calendar toCalendar(String date) {
		return toCalendar(toDate(date));
	}
	
	/**
	 * 根据给定的格式格式化日期
	 * @param date
	 * @param format 指定的日期格式
	 * @return
	 */
	public static String toString(Date date, String format) {
		if (date == null) return null;
		return new SimpleDateFormat(format).format(date);
	}
	
	/**
	 * 将日期格式化为纯日期格式
	 * @param date
	 * @return
	 */
	public static String toDateString(Date date) {
		return toString(date, DATE_FORMAT);
	}
	
	/**
	 * 将日期格式化为日期与时间格式
	 * @param date
	 * @return
	 */
	public static String toDateTimeString(Date date) {
		return toString(date, DATETIME_FORMAT);
	}
	
	/**
	 * 将日期格式化为带毫秒的日期格式
	 * @param date
	 * @return
	 */
	public static String toFullTimeString(Date date) {
		return toString(date, FULLTIME_FORMAT);
	}

	/**
	 * 将日期格式化为时间戳
	 * @param date
	 * @return
	 */
	public static String toTimestampString(Date date) {
		return toString(date, TIMESTAMP_FORMAT);
	}
	
	/**
	 * 清除日期里的时分秒
	 * @return
	 */
	public static Date clearTime(Date date) {
		Calendar calendar = toCalendar(date);
		calendar.set(Calendar.HOUR_OF_DAY, 0);
		calendar.set(Calendar.MINUTE, 0);
		calendar.set(Calendar.SECOND, 0);
		return calendar.getTime();
	}
	
	/**
	 * 清除日期里的时分秒
	 * @return
	 */
	public static String clearTime(String date) {
		return toDateString(clearTime(toDate(date)));
	}
	
	/**
	 * 得到两个日期的间隔秒数
	 * @param beginDate
	 * @param endDate
	 * @return
	 */
	public static int getSecondInterval(Date beginDate, Date endDate) {
		long diffMillis = endDate.getTime() - beginDate.getTime();
		return (int)(diffMillis / 1000);
	}
	
	/**
	 * 得到两个日期的间隔秒数
	 * @param beginDate
	 * @param endDate
	 * @return
	 */
	public static int getSecondInterval(String beginDate, String endDate) {
		boolean lenient = getLenient();
		
		setLenient(lenient);
		Date begin = toDate(beginDate);
		
		setLenient(lenient);
		Date end = toDate(endDate);
		
		return getSecondInterval(begin, end);
	}
	
	/**
	 * 得到两个日期的间隔分钟
	 * @param beginDate
	 * @param endDate
	 * @return
	 */
	public static int getMinuteInterval(Date beginDate, Date endDate) {
		return getSecondInterval(beginDate, endDate) / 60;
	}
	
	/**
	 * 得到两个日期的间隔分钟
	 * @param beginDate
	 * @param endDate
	 * @return
	 */
	public static int getMinuteInterval(String beginDate, String endDate) {
		return getSecondInterval(beginDate, endDate) / 60;
	}
	
	/**
	 * 得到两个日期的间隔小时
	 * @param beginDate
	 * @param endDate
	 * @return
	 */
	public static int getHourInterval(Date beginDate, Date endDate) {
		return getMinuteInterval(beginDate, endDate) / 60;
	}
	
	/**
	 * 得到两个日期的间隔小时
	 * @param beginDate
	 * @param endDate
	 * @return
	 */
	public static int getHourInterval(String beginDate, String endDate) {
		return getMinuteInterval(beginDate, endDate) / 60;
	}
	
	/**
	 * 得到两个日期的间隔
	 * 不算时分秒，只算日期
	 * @param beginDate
	 * @param endDate
	 * @return
	 */
	public static int getDateIntervalFloored(Date beginDate, Date endDate) {
		return getHourInterval(clearTime(beginDate), clearTime(endDate)) / 24;
	}
	
	/**
	 * 得到两个日期的间隔
	 * 不算时分秒，只算日期
	 * @param beginDate
	 * @param endDate
	 * @return
	 */
	public static int getDateIntervalFloored(String beginDate, String endDate) {
		boolean lenient = getLenient();
		
		setLenient(lenient);
		Date begin = toDate(beginDate);
		
		setLenient(lenient);
		Date end = toDate(endDate);
		
		return getDateIntervalFloored(begin, end);
	}
	
	/**
	 * 得到两个日期的间隔
	 * @param beginDate
	 * @param endDate
	 * @return
	 */
	public static int getDateInterval(Date beginDate, Date endDate) {
		return getHourInterval(beginDate, endDate) / 24;
	}
	
	/**
	 * 得到两个日期的间隔
	 * @param beginDate
	 * @param endDate
	 * @return
	 */
	public static int getDateInterval(String beginDate, String endDate) {
		return getHourInterval(beginDate, endDate) / 24;
	}
	
	/**
	 * 得到两个日期的间隔月数
	 * @param beginDate
	 * @param endDate
	 * @return
	 */
	public static int getMonthInterval(Date beginDate, Date endDate) {
		Calendar begin = toCalendar(beginDate);
		Calendar end = toCalendar(endDate);
		
		int diffYears = end.get(Calendar.YEAR) - begin.get(Calendar.YEAR);
		int diffMonths = diffYears * 12 + end.get(Calendar.MONTH) - begin.get(Calendar.MONTH);
		
		//根据日期调整结果
		if (end.after(begin) && end.get(Calendar.DATE) < begin.get(Calendar.DATE)) diffMonths--;
		if (begin.after(end) && begin.get(Calendar.DATE) < end.get(Calendar.DATE)) diffMonths++;
		
		return diffMonths;
	}
	
	/**
	 * 得到两个日期的间隔月数
	 * @param beginDate
	 * @param endDate
	 * @return
	 */
	public static int getMonthInterval(String beginDate, String endDate) {
		boolean lenient = getLenient();
		
		setLenient(lenient);
		Date begin = toDate(beginDate);
		
		setLenient(lenient);
		Date end = toDate(endDate);
		
		return getMonthInterval(begin, end);
	}
	
	/**
	 * 得到两个日期的间隔年数
	 * @param beginDate
	 * @param endDate
	 * @return
	 */
	public static int getYearInterval(Date beginDate, Date endDate) {
		return getMonthInterval(beginDate, endDate) / 12;
	}
	
	/**
	 * 得到两个日期的间隔年数
	 * @param beginDate
	 * @param endDate
	 * @return
	 */
	public static int getYearInterval(String beginDate, String endDate) {
		return getMonthInterval(beginDate, endDate) / 12;
	}
	
	/**
	 * 获取日期的秒数
	 * @param date
	 * @return
	 */
	public static int getSecond(Date date) {
		Calendar calendar = toCalendar(date);
		return calendar.get(Calendar.SECOND);
	}
	
	/**
	 * 获取日期的秒数
	 * @param date
	 * @return
	 */
	public static int getSecond(String date) {
		return getSecond(toDate(date));
	}
	
	/**
	 * 获取日期从零点开始的秒数
	 * @param date
	 * @return
	 */
	public static int getSecondOfZero(Date date) {
		Calendar calendar = toCalendar(date);
		int hour = calendar.get(Calendar.HOUR_OF_DAY);
		int minute = calendar.get(Calendar.MINUTE);
		int second = calendar.get(Calendar.SECOND);
		return hour * 3600 + minute * 60 + second;
	}
	
	/**
	 * 获取日期从零点开始的秒数
	 * @param date
	 * @return
	 */
	public static int getSecondOfZero(String date) {
		return getSecondOfZero(toDate(date));
	}
	
	/**
	 * 将日期设为指定的秒数
	 * @param date
	 * @param second
	 * @return
	 */
	public static Date setSecond(Date date, int second) {
		Calendar calendar = toCalendar(date);
		calendar.set(Calendar.SECOND, second);
		return calendar.getTime();
	}
	
	/**
	 * 将日期设为指定的秒数
	 * @param date
	 * @param second
	 * @return
	 */
	public static String setSecond(String date, int second) {
		return toDateString(setSecond(toDate(date), second));
	}
	
	/**
	 * 将日期设为从零点开始的秒数
	 * @param date
	 * @param second
	 * @return
	 */
	public static Date setSecondOfZero(Date date, int second) {
		date = clearTime(date);
		return addSecond(date, second);
	}
	
	/**
	 * 将日期设为从零点开始的秒数
	 * @param date
	 * @param second
	 * @return
	 */
	public static String setSecondOfZero(String date, int second) {
		return toDateString(setSecondOfZero(toDate(date), second));
	}
	
	/**
	 * 得到指定秒数后的日期
	 * @param date
	 * @param inteval
	 * @return
	 */
	public static Date addSecond(Date date, int inteval) {
		Calendar calendar = toCalendar(date);
		calendar.add(Calendar.SECOND, inteval);
		return calendar.getTime();
	}
	
	/**
	 * 得到指定秒数后的日期
	 * @param date
	 * @param inteval
	 * @return
	 */
	public static String addSecond(String date, int inteval) {
		return toDateString(addSecond(toDate(date), inteval));
	}
	
	/**
	 * 获取日期的分钟
	 * @param date
	 * @return
	 */
	public static int getMinute(Date date) {
		Calendar calendar = toCalendar(date);
		return calendar.get(Calendar.MINUTE);
	}
	
	/**
	 * 获取日期的分钟
	 * @param date
	 * @return
	 */
	public static int getMinute(String date) {
		return getMinute(toDate(date));
	}
	
	/**
	 * 获取日期从零点开始的分钟
	 * @param date
	 * @return
	 */
	public static int getMinuteOfZero(Date date) {
		return getSecondOfZero(date) / 60;
	}
	
	/**
	 * 获取日期从零点开始的分钟
	 * @param date
	 * @return
	 */
	public static int getMinuteOfZero(String date) {
		return getSecondOfZero(date) / 60;
	}
	
	/**
	 * 将日期设为指定的分钟
	 * @param date
	 * @param minute
	 * @return
	 */
	public static Date setMinute(Date date, int minute) {
		Calendar calendar = toCalendar(date);
		calendar.set(Calendar.MINUTE, minute);
		return calendar.getTime();
	}
	
	/**
	 * 将日期设为指定的分钟
	 * @param date
	 * @param minute
	 * @return
	 */
	public static String setMinute(String date, int minute) {
		return toDateString(setMinute(toDate(date), minute));
	}
	
	/**
	 * 将日期设为从零点开始的分钟
	 * @param date
	 * @param minute
	 * @return
	 */
	public static Date setMinuteOfZero(Date date, int minute) {
		date = clearTime(date);
		return addMinute(date, minute);
	}
	
	/**
	 * 将日期设为从零点开始的分钟
	 * @param date
	 * @param minute
	 * @return
	 */
	public static String setMinuteOfZero(String date, int minute) {
		return toDateString(setMinuteOfZero(toDate(date), minute));
	}
	
	/**
	 * 得到指定分钟后的日期
	 * @param date
	 * @param inteval
	 * @return
	 */
	public static Date addMinute(Date date, int inteval) {
		Calendar calendar = toCalendar(date);
		calendar.add(Calendar.MINUTE, inteval);
		return calendar.getTime();
	}
	
	/**
	 * 得到指定分钟后的日期
	 * @param date
	 * @param inteval
	 * @return
	 */
	public static String addMinute(String date, int inteval) {
		return toDateString(addMinute(toDate(date), inteval));
	}
	
	/**
	 * 获取日期的小时
	 * @param date
	 * @return
	 */
	public static int getHour(Date date) {
		Calendar calendar = toCalendar(date);
		return calendar.get(Calendar.HOUR_OF_DAY);
	}
	
	/**
	 * 获取日期的小时
	 * @param date
	 * @return
	 */
	public static int getHour(String date) {
		return getHour(toDate(date));
	}
	
	/**
	 * 将日期设为指定的小时
	 * @param date
	 * @param hour
	 * @return
	 */
	public static Date setHour(Date date, int hour) {
		Calendar calendar = toCalendar(date);
		calendar.set(Calendar.HOUR_OF_DAY, hour);
		return calendar.getTime();
	}
	
	/**
	 * 将日期设为指定的小时
	 * @param date
	 * @param hour
	 * @return
	 */
	public static String setHour(String date, int hour) {
		return toDateString(setHour(toDate(date), hour));
	}
	
	/**
	 * 得到指定小时后的日期
	 * @param date
	 * @param inteval
	 * @return
	 */
	public static Date addHour(Date date, int inteval) {
		Calendar calendar = toCalendar(date);
		calendar.add(Calendar.HOUR_OF_DAY, inteval);
		return calendar.getTime();
	}
	
	/**
	 * 得到指定小时后的日期
	 * @param date
	 * @param inteval
	 * @return
	 */
	public static String addHour(String date, int inteval) {
		return toDateString(addHour(toDate(date), inteval));
	}
	
	/**
	 * 获取日期的日
	 * @param date
	 * @return
	 */
	public static int getDate(Date date) {
		Calendar calendar = toCalendar(date);
		return calendar.get(Calendar.DATE);
	}
	
	/**
	 * 获取日期的日
	 * @param date
	 * @return
	 */
	public static int getDate(String date) {
		return getDate(toDate(date));
	}
	
	/**
	 * 将日期设为指定的日
	 * @param date
	 * @param day
	 * @return
	 */
	public static Date setDate(Date date, int day) {
		Calendar calendar = toCalendar(date);
		calendar.set(Calendar.DATE, day);
		return calendar.getTime();
	}
	
	/**
	 * 将日期设为指定的日
	 * @param date
	 * @param day
	 * @return
	 */
	public static String setDate(String date, int day) {
		return toDateString(setDate(toDate(date), day));
	}
	
	/**
	 * 得到指定天数后的日期
	 * @param date
	 * @param inteval
	 * @return
	 */
	public static Date addDate(Date date, int inteval) {
		Calendar calendar = toCalendar(date);
		calendar.add(Calendar.DATE, inteval);
		return calendar.getTime();
	}
	
	/**
	 * 得到指定天数后的日期
	 * @param date
	 * @param inteval
	 * @return
	 */
	public static String addDate(String date, int inteval) {
		return toDateString(addDate(toDate(date), inteval));
	}
	
	/**
	 * 获取日期的月份(1月份为0)
	 * @param date
	 * @return
	 */
	public static int getMonth(Date date) {
		Calendar calendar = toCalendar(date);
		return calendar.get(Calendar.MONTH);
	}
	
	/**
	 * 获取日期的月份(1月份为0)
	 * @param date
	 * @return
	 */
	public static int getMonth(String date) {
		return getMonth(toDate(date));
	}
	
	/**
	 * 将日期设为指定的月
	 * @param date
	 * @param month 1月份为0
	 * @return
	 */
	public static Date setMonth(Date date, int month) {
		Calendar calendar = toCalendar(date);
		calendar.set(Calendar.MONTH, month);
		return calendar.getTime();
	}
	
	/**
	 * 将日期设为指定的月
	 * @param date
	 * @param month 1月份为0
	 * @return
	 */
	public static String setMonth(String date, int month) {
		return toDateString(setMonth(toDate(date), month));
	}
	
	/**
	 * 得到指定月数之后的日期
	 * @param date
	 * @param inteval
	 * @return
	 */
	public static Date addMonth(Date date, int inteval) {
		Calendar calendar = toCalendar(date);
		calendar.add(Calendar.MONTH, inteval);
		return calendar.getTime();
	}
	
	/**
	 * 得到指定月数之后的日期
	 * @param date
	 * @param inteval
	 * @return
	 */
	public static String addMonth(String date, int inteval) {
		return toDateString(addMonth(toDate(date), inteval));
	}
	
	/**
	 * 获取日期的年份
	 * @param date
	 * @return
	 */
	public static int getYear(Date date) {
		Calendar calendar = toCalendar(date);
		return calendar.get(Calendar.YEAR);
	}
	
	/**
	 * 获取日期的年份
	 * @param date
	 * @return
	 */
	public static int getYear(String date) {
		return getYear(toDate(date));
	}
	
	/**
	 * 将日期设为指定的年
	 * @param date
	 * @param year
	 * @return
	 */
	public static Date setYear(Date date, int year) {
		Calendar calendar = toCalendar(date);
		calendar.set(Calendar.YEAR, year);
		return calendar.getTime();
	}
	
	/**
	 * 将日期设为指定的年
	 * @param date
	 * @param year
	 * @return
	 */
	public static String setYear(String date, int year) {
		return toDateString(setYear(toDate(date), year));
	}
	
	/**
	 * 得到指定年数之后的日期
	 * @param date
	 * @param inteval
	 * @return
	 */
	public static Date addYear(Date date, int inteval) {
		Calendar calendar = toCalendar(date);
		calendar.add(Calendar.YEAR, inteval);
		return calendar.getTime();
	}
	
	/**
	 * 得到指定年数之后的日期
	 * @param date
	 * @param inteval
	 * @return
	 */
	public static String addYear(String date, int inteval) {
		return toDateString(addYear(toDate(date), inteval));
	}
	
	/**
	 * 判断日期是否为当前月的最后一天
	 * @param date
	 * @return
	 */
	public static boolean isLastDayOfMonth(Date date) {
		return getMonth(addDate(date, 1)) != getMonth(date);
	}
	
	/**
	 * 判断日期是否为当前月的最后一天
	 * @param date
	 * @return
	 */
	public static boolean isLastDayOfMonth(String date) {
		return isLastDayOfMonth(toDate(date));
	}
	
	/**
	 * 设置日期为当前月的最后一天
	 * @param date
	 * @return
	 */
	public static Date setLastDayOfMonth(Date date) {
		date = setDate(date, 1);
		date = addMonth(date, 1);
		return addDate(date, -1);
	}
	
	/**
	 * 设置日期为当前月的最后一天
	 * @param date
	 * @return
	 */
	public static String setLastDayOfMonth(String date) {
		return toDateString(setLastDayOfMonth(toDate(date)));
	}
	
}
