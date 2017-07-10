package com.github.woshikid.utils;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;

/**
 * 
 * @author kid
 *
 */
public class FileUtils {

	public static InputStream getResourceAsStream(String path) {
		return FileUtils.class.getClassLoader().getResourceAsStream(path);
	}
	
	public static String getWebRootPath() {
		String classPath = FileUtils.class.getClassLoader().getResource("").getPath();
		if (classPath.endsWith("WEB-INF/classes/")) {
			return classPath.substring(0, classPath.length() - 16);
		} else {
			return classPath;
		}
	}
	
	public static boolean copyFile(File from, File to) throws Exception {
		if (!from.isFile() || to.isDirectory() || from.equals(to)) return false;
		
		try (FileInputStream in = new FileInputStream(from);
			FileOutputStream out = new FileOutputStream(to)) {
			
			byte[] buffer = new byte[1024 * 1024];
			
			int length;
			while ((length = in.read(buffer)) != -1) {
				out.write(buffer, 0, length);
			}
			
			out.flush();
		}
		
		return true;
	}
	
	public static boolean deleteFile(File file) throws Exception {
		if (file.isFile()) return file.delete();
		
		if (file.isDirectory()) {
			for (File subfile : file.listFiles()) {
				deleteFile(subfile);
			}
			
			return file.delete();
		}
		
		return true;
	}
	
	public static boolean moveFile(File from, File to) throws Exception {
		if (!copyFile(from, to)) return false;
		return deleteFile(from);
	}
	
	public static byte[] readStream(InputStream in) throws Exception {
		ByteArrayOutputStream cache = new ByteArrayOutputStream();
		byte[] buffer = new byte[1024 * 1024];
		
		int length;
		while ((length = in.read(buffer)) != -1) {
			cache.write(buffer, 0, length);
		}
		
		return cache.toByteArray();
	}
	
	public static String readStream(InputStream in, String charset) throws Exception {
		if (charset == null) charset = Charset.defaultCharset().name();
		
		BufferedReader reader = new BufferedReader(new InputStreamReader(in, charset));
		StringBuffer buffer = new StringBuffer();
		
		String line;
		while ((line = reader.readLine()) != null) {
			buffer.append(line).append(System.lineSeparator());
		}
		
		return buffer.toString();
	}
	
	public static byte[] readFile(File file) throws Exception {
		try (InputStream in = new FileInputStream(file)) {
			return readStream(in);
		}
	}
	
	public static String readFile(File file, String charset) throws Exception {
		if (charset == null) charset = Charset.defaultCharset().name();
		
		try (InputStream in = new FileInputStream(file)) {
			return readStream(in, charset);
		}
	}
	
	public static void writeFile(File file, byte[] data, boolean append) throws Exception {
		try (FileOutputStream out = new FileOutputStream(file, append)) {
			out.write(data);
			out.flush();
		}
	}
	
}
