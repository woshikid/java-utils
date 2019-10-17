package com.github.woshikid.utils;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.StringWriter;
import java.nio.charset.Charset;

/**
 * 
 * @author kid
 *
 */
public class IOUtils {

	public static void close(Closeable closeable) {
		try {
			if (closeable != null) {
				closeable.close();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public static void close(Object object) {
		try {
			if (object != null) {
				object.getClass().getMethod("close").invoke(object);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public static void close(Object object, boolean flag) {
		try {
			if (object != null) {
				object.getClass().getMethod("close", boolean.class).invoke(object, flag);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public static long copyStream(InputStream in, OutputStream out) throws IOException {
		byte[] buffer = new byte[8192];
		
		long count = 0;
		int length = 0;
		while ((length = in.read(buffer)) != -1) {
			out.write(buffer, 0, length);
			count += length;
		}
		
		return count;
	}
	
	public static byte[] readStream(InputStream in) throws IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		copyStream(in, out);
		return out.toByteArray();
	}
	
	public static String readStream(InputStream in, String charset) throws IOException {
		if (charset == null) charset = Charset.defaultCharset().name();
		
		InputStreamReader reader = new InputStreamReader(in, charset);
		StringWriter writer = new StringWriter();
		
		char[] buffer = new char[8192];
		int length = 0;
		while ((length = reader.read(buffer)) != -1) {
			writer.write(buffer, 0, length);
		}
		
		return writer.toString();
	}
	
}
