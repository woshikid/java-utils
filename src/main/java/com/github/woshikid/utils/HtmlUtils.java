package com.github.woshikid.utils;

import java.util.Map;

/**
 * 
 * @author kid
 *
 */
public class HtmlUtils {

	/**
	 * js escape
	 * @param value
	 * @return
	 */
	public static String js(Object value) {
		if (value == null) return "";
		return String.valueOf(value).replace("\\", "\\\\").replace("'", "\\'").replace("\"", "\\\"").replace("\r", "\\r").replace("\n", "\\n");
	}
	
	/**
	 * html escape, used in &lt;input name="&lt;%=HtmlUtils.value(?) %&gt;"&gt;
	 * @param value
	 * @return
	 */
	public static String value(Object value) {
		if (value == null) return "";
		return String.valueOf(value).replace("&", "&amp;").replace("\"", "&quot;").replace("<", "&lt;").replace(">", "&gt;").replace("'", "&#39;").replace(" ", "&#32;").replace("\r", "").replace("\n", "");
	}
	
	/**
	 * html escape, used in &lt;textarea&gt;&lt;%=HtmlUtils.content(?) %&gt;&lt;/textarea&gt;
	 * @param value
	 * @return
	 */
	public static String content(Object value) {
		if (value == null) return "";
		return String.valueOf(value).replace("&", "&amp;").replace("\"", "&quot;").replace("<", "&lt;").replace(">", "&gt;").replace("'", "&#39;").replace(" ", "&#32;");
	}
	
	/**
	 * html escape, used in &lt;tr&gt;&lt;td&gt;&lt;%=HtmlUtils.td(?) %&gt;&lt;/td&gt;&lt;/tr&gt;
	 * @param value
	 * @return
	 */
	public static String td(Object value) {
		if (value == null) return "　";
		String td = String.valueOf(value).trim();
		if (td.length() == 0) return "　";
		return content(td);
	}
	
	/**
	 * 组装select的选项
	 * @param map
	 * @param defaultKey
	 * @return
	 */
	public static String buildOptions(Map<String, Object> map, String defaultKey) {
		if (map == null) return "";
		
		StringBuffer options = new StringBuffer();
		for (String key : map.keySet()) {
			Object value = map.get(key);
			String text = (value == null) ? "" : String.valueOf(value);
			boolean selected = String.valueOf(key).equals(defaultKey);
			options.append("<option").append(selected ? " selected" : "");
			options.append(" value='").append(value(key)).append("'>");
			options.append(value(text)).append("</option>\n");
		}
		
		return options.toString();
	}
	
}
