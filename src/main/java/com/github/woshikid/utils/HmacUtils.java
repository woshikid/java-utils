package com.github.woshikid.utils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

/**
 * @author kid
 */
public class HmacUtils {

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
    
    public static String encrypt(String algorithmName, byte[] bytes, byte[] key) {
        if (bytes == null) return null;
        if (key == null) throw new IllegalArgumentException();

        try {
            Mac mac = Mac.getInstance(algorithmName);
            mac.init(new SecretKeySpec(key, algorithmName));
            return byte2Hex(mac.doFinal(bytes));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static String hmacSHA1(byte[] bytes, byte[] key) {
        return encrypt("HmacSHA1", bytes, key);
    }

    public static String hmacSHA1(String message, String key) {
        if (message == null) return null;
        if (key == null) throw new IllegalArgumentException();
        return hmacSHA1(message.getBytes(StandardCharsets.UTF_8), key.getBytes(StandardCharsets.UTF_8));
    }
    
    public static String hmacSHA256(byte[] bytes, byte[] key) {
        return encrypt("HmacSHA256", bytes, key);
    }
    
    public static String hmacSHA256(String message, String key) {
        if (message == null) return null;
        if (key == null) throw new IllegalArgumentException();
        return hmacSHA256(message.getBytes(StandardCharsets.UTF_8), key.getBytes(StandardCharsets.UTF_8));
    }

    public static String hmacSHA512(byte[] bytes, byte[] key) {
        return encrypt("HmacSHA512", bytes, key);
    }

    public static String hmacSHA512(String message, String key) {
        if (message == null) return null;
        if (key == null) throw new IllegalArgumentException();
        return hmacSHA512(message.getBytes(StandardCharsets.UTF_8), key.getBytes(StandardCharsets.UTF_8));
    }
}
