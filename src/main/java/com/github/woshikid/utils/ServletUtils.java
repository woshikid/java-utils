package com.github.woshikid.utils;

import java.io.UnsupportedEncodingException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/**
 * 
 * @author kid
 *
 */
public class ServletUtils {

	public static void setCharset(HttpServletResponse response, String charset) {
		response.setCharacterEncoding(charset);
		response.setHeader("Content-Type", "text/html; charset=" + charset);
	}
	
	public static void setDownload(HttpServletResponse response, String fileName) {
		try {
			fileName = new String(fileName.getBytes("GBK"), "ISO-8859-1");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		
		response.setContentType("application/force-download");
		response.setHeader("Content-Disposition", "attachment; filename=" + fileName);
	}
	
	public static Object getSession(HttpServletRequest request, String name) {
		HttpSession session = request.getSession(false);
		if (session == null) return null;
		return session.getAttribute(name);
	}
	
	private static boolean unknownIP(String ip) {
		if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
			return true;
		} else {
			return false;
		}
	}
	
	public static String getRemoteAddr(HttpServletRequest request) {
		String ip = request.getHeader("x-forwarded-for");
		if(unknownIP(ip)) ip = request.getHeader("Proxy-Client-IP");
		if(unknownIP(ip)) ip = request.getHeader("WL-Proxy-Client-IP");
		if(unknownIP(ip)) ip = request.getHeader("HTTP_CLIENT_IP");
		if(unknownIP(ip)) ip = request.getHeader("HTTP_X_FORWARDED_FOR");
		if(unknownIP(ip)) ip = request.getRemoteAddr();
		if(unknownIP(ip)) ip = "";
		return ip.split(",")[0];
	}
	
	public static String getAgent(HttpServletRequest request) {
		return request.getHeader("User-Agent");
	}
	
	public static Map<String, String> getParameterMap(HttpServletRequest request) {
		Enumeration<String> e = request.getParameterNames();
		Map<String, String> map = new HashMap<String, String>();
		if (e == null) return map;
		
		while (e.hasMoreElements()) {
			String key = e.nextElement();
			map.put(key, request.getParameter(key));
		}
		
		return map;
	}
}
