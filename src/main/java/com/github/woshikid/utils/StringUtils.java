package com.github.woshikid.utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 
 * @author kid
 *
 */
public class StringUtils extends org.apache.commons.lang3.StringUtils {

	public static int gbkSize(Object input){
		try{
			return String.valueOf(input).getBytes("GBK").length;
		}catch(Exception e){
			return -1;
		}
	}
	
	public static int utf8Size(Object input){
		try{
			return String.valueOf(input).getBytes("UTF-8").length;
		}catch(Exception e){
			return -1;
		}
	}
	
	/**
	 * 得到符合规范的文件名
	 * @param input
	 * @return
	 */
	public static String fileName(Object input){
		return String.valueOf(input).replace("\\", "").replace("/", "").replace(":", "").replace("*", "").replace("?", "").replace("\"", "").replace("<", "").replace(">", "").replace("|", "");
	}
	
	/**
	 * change auto_case into autoCase
	 * @param input
	 * @return
	 */
	public static String autoCase(Object input){
		String text = String.valueOf(input);
		Matcher m = Pattern.compile("_([a-z])").matcher(text);
		while (m.find()) text = text.replace(m.group(), m.group(1).toUpperCase());
		return text;
	}
	
	/**
	 * change auto_case into AutoCase
	 * @param input
	 * @return
	 */
	public static String AutoCase(Object input){
		return capitalize(autoCase(input));
	}
	
	public static String byte2Hex(byte[] bytes) {
		if (bytes == null) return null;
		
		int bLen = bytes.length;
		StringBuilder sb = new StringBuilder(bLen * 2);
		for (int i = 0; i < bLen; i++) {
			int intbyte = bytes[i];
			while (intbyte < 0) {
				intbyte += 256;
			}
			
			if (intbyte < 16) sb.append("0");
			sb.append(Integer.toString(intbyte, 16));
		}
		
		return sb.toString();
	}
	
	public static byte[] hex2Byte(String hex) {
		if (hex == null) return null;
		
		int hLen = hex.length();
		if ((hLen & 1) != 0) throw new IllegalArgumentException();
		
		byte[] bytes = new byte[hLen / 2];
		for (int i = 0; i < hLen; i += 2) {
			String oneHex = hex.substring(i, i + 2);
			bytes[i / 2] = (byte)Integer.parseInt(oneHex, 16);
		}
		
		return bytes;
	}
	
	public static String toUnicode(String text){
		if (text == null) return null;
		
		StringBuffer unicode = new StringBuffer();
		for(int i = 0; i < text.length(); i++){
			try{
				unicode.append("\\u").append(byte2Hex(text.substring(i, i + 1).getBytes("UnicodeBigUnmarked")));
			}catch(Exception e){
				return null;
			}
		}
		return unicode.toString();
	}
	
	public static String fromUnicode(String unicode){
		if (unicode == null) return null;
		
		try{
			return new String(hex2Byte(unicode.replace("\\u", "")), "UnicodeBigUnmarked");
		}catch(Exception e){
			return null;
		}
	}
}
