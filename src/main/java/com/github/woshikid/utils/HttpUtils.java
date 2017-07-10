package com.github.woshikid.utils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * http工具，用来发送接收报文
 * @author kid
 *
 */
public class HttpUtils {
	
	private static final Logger logger = LoggerFactory.getLogger(HttpUtils.class);
	private static final ThreadLocal<Boolean> followRedirects = new ThreadLocal<>();
	private static final ThreadLocal<Integer> httpTimeout = new ThreadLocal<>();
	
	/**
	 * 设置是否自动重定向302跳转
	 * 默认为true
	 * 本线程有效，并且在调用request方法后自动清除(仅生效一次)
	 * @param follow
	 */
	public static void setFollowRedirects(boolean follow) {
		followRedirects.set(follow);
	}
	
	/**
	 * 设置超时时间
	 * 默认为60000
	 * 本线程有效，并且在调用request方法后自动清除(仅生效一次)
	 * @param timeout
	 */
	public static void setTimeout(int timeout) {
		httpTimeout.set(timeout);
	}
	
	/**
	 * http返回数据结构
	 * @author chenhan
	 *
	 */
	public static class Response {
		/**
		 * http返回码
		 */
		public int code;
		
		/**
		 * 返回的http头
		 */
		public Map<String, List<String>> header;
		
		/**
		 * 解析出来的文本编码格式
		 */
		public Charset charset;
		
		/**
		 * 返回的字节流
		 */
		public byte[] data;
		
		/**
		 * 得到正常情况下返回的文本
		 * HTTP Status Code = 200
		 * @return
		 * @throws IOException
		 */
		public String getText() throws IOException {
			if (code != 200) throw new IOException("HTTP Status Code:" + code);
			return getTextIgnoreError();
		}
		
		/**
		 * 得到任何情况下返回的文本
		 * 包含302、404、500等返回码的情况
		 * @return
		 * @throws IOException
		 */
		public String getTextIgnoreError() {
			if (charset != null) {
				return new String(data, charset);
			} else {
				return new String(data, StandardCharsets.UTF_8);
			}
		}
	}
	
	/**
	 * get/post请求，兼容https
	 * 当data为null时为get请求，否则为post请求
	 * @param url 连接的url
	 * @param header 发送的头信息
	 * @param data 发送的post数据
	 * @return
	 * @throws Exception
	 */
	public static Response request(String url, Map<String, String> header, byte[] data) throws IOException {
		long threadId = Thread.currentThread().getId();
		logger.info("[{}] request url={}", threadId, url);
		
		//初始化连接并设置参数
		HttpURLConnection http = (HttpURLConnection)new URL(url).openConnection();
		http.setUseCaches(false);
		http.setDoInput(true);
		http.setDoOutput(data != null);
		//默认立即关闭连接
		http.setRequestProperty("Connection", "Close");
		//设置是否自动重定向
		Boolean follow = followRedirects.get();
		if (follow != null) {
			followRedirects.remove();
			http.setInstanceFollowRedirects(follow);
		}
		//设置超时时间
		Integer timeout = httpTimeout.get();
		if (timeout == null) {
			timeout = 60000;
		} else {
			httpTimeout.remove();
		}
		http.setConnectTimeout(timeout);
		http.setReadTimeout(timeout);
		
		//设置http头信息
		if (header != null) {
			for (String key : header.keySet()) {
				String value = header.get(key);
				logger.info("[{}] header:{}={}", threadId, key, value);
				http.setRequestProperty(key, value);
			}
		}
		
		//进行连接
		http.connect();
		
		//处理数据
		try {
			//发送请求数据
			if (data != null) {
				try (OutputStream out = http.getOutputStream()) {
					out.write(data);
				}
			}
			
			//接收返回数据
			//如果对方服务器返回500等错误
			//需要获取getErrorStream
			ByteArrayOutputStream cache = new ByteArrayOutputStream();
			InputStream in;
			try {
				in = http.getInputStream();
			} catch (IOException e) {
				in = http.getErrorStream();
			}
			
			if (in != null) {
				try {
					//处理gzip压缩
					String encode = http.getContentEncoding();
					if (encode != null && encode.trim().equalsIgnoreCase("gzip")) {
						in = new GZIPInputStream(in);
					}
					
					//读取返回数据至数组
					int length;
					byte[] buffer = new byte[8192];
					while ((length = in.read(buffer)) != -1) {
						cache.write(buffer, 0, length);
					}
				} finally {
					in.close();
				}
			}
			
			//开始组装返回内容
			Response response = new Response();
			response.data = cache.toByteArray();
			response.header = http.getHeaderFields();
			response.code = http.getResponseCode();
			
			//尝试解析字符编码格式
			String contentType = http.getContentType();
			if (contentType != null) {
				Matcher matcher = Pattern.compile("(?i)\\bcharset\\s*=\\s*([^\\s;]+)").matcher(contentType);
				if (matcher.find()) {
					String charset = matcher.group(1);
					if (Charset.isSupported(charset)) {
						response.charset = Charset.forName(charset);
					}
				}
			}
			
			//尝试从正文解析字符编码格式
			if (response.charset == null) {
				int length = response.data.length;
				if (length > 1024) length = 1024;
				
				String head = new String(response.data, 0, length);
				Matcher matcher = Pattern.compile("(?is)<meta\\s[^>]*\\bcharset\\s*=\\s*['\"]?\\s*([^\\s'\"/>]+)[^>]*>").matcher(head);
				if (matcher.find()) {
					String charset = matcher.group(1);
					if (Charset.isSupported(charset)) {
						response.charset = Charset.forName(charset);
					}
				}
			}
			
			logger.info("[{}] response.code={}", threadId, response.code);
			logger.info("[{}] response.data.length={}", threadId, response.data.length);
			return response;
		} finally {
			http.disconnect();
		}
	}
	
	/**
	 * url参数utf8转义
	 * @param input
	 * @return
	 */
	public static String encodeUtf8(Object input) {
		try {
			return URLEncoder.encode(String.valueOf(input), "UTF-8");
		} catch (Exception e) {
			return null;
		}
	}
	
	/**
	 * 生成请求参数字符串
	 * 参数将被编码为utf8转义格式
	 * @param map
	 * @return
	 */
	public static String buildGetParameter(Map<String, Object> map) {
		if (map == null) return "";
		
		StringBuffer parameter = new StringBuffer();
		for (String key : map.keySet()) {
			Object value = map.get(key);
			parameter.append("&").append(encodeUtf8(key));
			parameter.append("=").append(encodeUtf8(value));
		}
		
		if (parameter.length() > 0) parameter.deleteCharAt(0);
		return parameter.toString();
	}
	
	/**
	 * 生成请求参数字符串
	 * 不进行字符转义
	 * 只适合在post中使用
	 * @param map
	 * @return
	 */
	public static String buildPostParameter(Map<String, Object> map) {
		if (map == null) return "";
		
		StringBuffer parameter = new StringBuffer();
		for (String key : map.keySet()) {
			Object value = map.get(key);
			parameter.append("&").append(key);
			parameter.append("=").append(value);
		}
		
		if (parameter.length() > 0) parameter.deleteCharAt(0);
		return parameter.toString();
	}
	
}
