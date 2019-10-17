package com.github.woshikid.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

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
	
	public static boolean copyFile(File from, File to) throws IOException {
		if (!from.isFile() || to.isDirectory() || from.equals(to)) return false;
		
		try (FileInputStream in = new FileInputStream(from);
			FileOutputStream out = new FileOutputStream(to)) {
			IOUtils.copyStream(in, out);
		}
		
		return true;
	}
	
	public static boolean deleteFile(File file) {
		if (file.isFile()) return file.delete();
		
		if (file.isDirectory()) {
			for (File subfile : file.listFiles()) {
				deleteFile(subfile);
			}
			
			return file.delete();
		}
		
		return true;
	}
	
	public static boolean moveFile(File from, File to) throws IOException {
		if (!copyFile(from, to)) return false;
		return deleteFile(from);
	}
	
	public static byte[] readFile(File file) throws IOException {
		try (InputStream in = new FileInputStream(file)) {
			return IOUtils.readStream(in);
		}
	}
	
	public static String readFile(File file, String charset) throws IOException {
		try (InputStream in = new FileInputStream(file)) {
			return IOUtils.readStream(in, charset);
		}
	}
	
	public static void writeFile(File file, byte[] data, boolean append) throws IOException {
		try (FileOutputStream out = new FileOutputStream(file, append)) {
			out.write(data);
		}
	}
	
	public static void writeFile(File file, byte[] data) throws IOException {
		writeFile(file, data, false);
	}
	
	public static void writeFile(File file, InputStream in) throws IOException {
		try (FileOutputStream out = new FileOutputStream(file)) {
			IOUtils.copyStream(in, out);
		}
	}
	
}
