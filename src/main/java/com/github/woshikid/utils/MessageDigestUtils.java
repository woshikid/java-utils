package com.github.woshikid.utils;

import java.nio.charset.Charset;
import java.security.MessageDigest;

/**
 * 
 * @author kid
 *
 */
public class MessageDigestUtils {
	
	public static final Charset charset = Charset.forName("UTF-8");

	private static String byte2Hex(byte[] bytes) {
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
	
	public static MessageDigest getDigest(String algorithmName) {
		try {
			return MessageDigest.getInstance(algorithmName);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	public static String digest(String algorithmName, byte[] bytes, int iterations) {
		if (bytes == null) return null;
		if (iterations < 1) throw new IllegalArgumentException();
		
		MessageDigest digest = getDigest(algorithmName);
		for (int i = 0; i < iterations; i++) {
			bytes = digest.digest(bytes);
		}
		
		return byte2Hex(bytes);
	}
	
	public static String md5(byte[] bytes, int iterations) {
		return digest("MD5", bytes, iterations);
	}
	
	public static String md5(byte[] bytes) {
		return md5(bytes, 1);
	}

	public static String md5(String message, int iterations) {
		if (message == null) return null;
		return md5(message.getBytes(charset), iterations);
	}
	
	public static String md5(String message) {
		if (message == null) return null;
		return md5(message.getBytes(charset));
	}
	
	public static String sha1(byte[] bytes, int iterations) {
		return digest("SHA1", bytes, iterations);
	}
	
	public static String sha1(byte[] bytes) {
		return sha1(bytes, 1);
	}

	public static String sha1(String message, int iterations) {
		if (message == null) return null;
		return sha1(message.getBytes(charset), iterations);
	}
	
	public static String sha1(String message) {
		if (message == null) return null;
		return sha1(message.getBytes(charset));
	}
	
	public static String sha256(byte[] bytes, int iterations) {
		return digest("SHA-256", bytes, iterations);
	}
	
	public static String sha256(byte[] bytes) {
		return sha256(bytes, 1);
	}

	public static String sha256(String message, int iterations) {
		if (message == null) return null;
		return sha256(message.getBytes(charset), iterations);
	}
	
	public static String sha256(String message) {
		if (message == null) return null;
		return sha256(message.getBytes(charset));
	}
	
	public static String sha512(byte[] bytes, int iterations) {
		return digest("SHA-512", bytes, iterations);
	}
	
	public static String sha512(byte[] bytes) {
		return sha512(bytes, 1);
	}

	public static String sha512(String message, int iterations) {
		if (message == null) return null;
		return sha512(message.getBytes(charset), iterations);
	}
	
	public static String sha512(String message) {
		if (message == null) return null;
		return sha512(message.getBytes(charset));
	}
	
}
