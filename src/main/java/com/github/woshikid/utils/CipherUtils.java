package com.github.woshikid.utils;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

/**
 * @author kid
 */
public class CipherUtils {

    public static byte[] encrypt(String algorithm, byte[] bytes, byte[] key, byte[] iv) {
        String algorithmName = algorithm.split("/")[0];
        if (bytes == null) return null;
        if (key == null) throw new IllegalArgumentException();
        
        try {
            Cipher cipher = Cipher.getInstance(algorithm);
            
            if (iv == null) {
                cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, algorithmName));
            } else {
                cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, algorithmName), new IvParameterSpec(iv));
            }
            
            return cipher.doFinal(bytes);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    
    public static byte[] decrypt(String algorithm, byte[] bytes, byte[] key, byte[] iv) {
        String algorithmName = algorithm.split("/")[0];
        if (bytes == null) return null;
        if (key == null) throw new IllegalArgumentException();

        try {
            Cipher cipher = Cipher.getInstance(algorithm);
            
            if (iv == null) {
                cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, algorithmName));
            } else {
                cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, algorithmName), new IvParameterSpec(iv));
            }
            
            return cipher.doFinal(bytes);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    
    public static byte[] encryptDES(byte[] bytes, String key, String iv) {
        if (key == null) throw new IllegalArgumentException();
        if (iv == null) throw new IllegalArgumentException();
        return encrypt("DES/CBC/PKCS5Padding", bytes, key.getBytes(StandardCharsets.UTF_8), iv.getBytes(StandardCharsets.UTF_8));
    }
    
    public static byte[] encryptDES(byte[] bytes, String key) {
        if (key == null) throw new IllegalArgumentException();
        return encrypt("DES/ECB/PKCS5Padding", bytes, key.getBytes(StandardCharsets.UTF_8), null);
    }
    
    public static byte[] decryptDES(byte[] bytes, String key, String iv) {
        if (key == null) throw new IllegalArgumentException();
        if (iv == null) throw new IllegalArgumentException();
        return decrypt("DES/CBC/PKCS5Padding", bytes, key.getBytes(StandardCharsets.UTF_8), iv.getBytes(StandardCharsets.UTF_8));
    }
    
    public static byte[] decryptDES(byte[] bytes, String key) {
        if (key == null) throw new IllegalArgumentException();
        return decrypt("DES/ECB/PKCS5Padding", bytes, key.getBytes(StandardCharsets.UTF_8), null);
    }

    public static byte[] encrypt3DES(byte[] bytes, String key, String iv) {
        if (key == null) throw new IllegalArgumentException();
        if (iv == null) throw new IllegalArgumentException();
        return encrypt("DESede/CBC/PKCS5Padding", bytes, key.getBytes(StandardCharsets.UTF_8), iv.getBytes(StandardCharsets.UTF_8));
    }

    public static byte[] encrypt3DES(byte[] bytes, String key) {
        if (key == null) throw new IllegalArgumentException();
        return encrypt("DESede/ECB/PKCS5Padding", bytes, key.getBytes(StandardCharsets.UTF_8), null);
    }

    public static byte[] decrypt3DES(byte[] bytes, String key, String iv) {
        if (key == null) throw new IllegalArgumentException();
        if (iv == null) throw new IllegalArgumentException();
        return decrypt("DESede/CBC/PKCS5Padding", bytes, key.getBytes(StandardCharsets.UTF_8), iv.getBytes(StandardCharsets.UTF_8));
    }

    public static byte[] decrypt3DES(byte[] bytes, String key) {
        if (key == null) throw new IllegalArgumentException();
        return decrypt("DESede/ECB/PKCS5Padding", bytes, key.getBytes(StandardCharsets.UTF_8), null);
    }
    
    public static byte[] encryptAES(byte[] bytes, String key, String iv) {
        if (key == null) throw new IllegalArgumentException();
        if (iv == null) throw new IllegalArgumentException();
        return encrypt("AES/CBC/PKCS5Padding", bytes, key.getBytes(StandardCharsets.UTF_8), iv.getBytes(StandardCharsets.UTF_8));
    }

    public static byte[] encryptAES(byte[] bytes, String key) {
        if (key == null) throw new IllegalArgumentException();
        return encrypt("AES/ECB/PKCS5Padding", bytes, key.getBytes(StandardCharsets.UTF_8), null);
    }
    
    public static byte[] decryptAES(byte[] bytes, String key, String iv) {
        if (key == null) throw new IllegalArgumentException();
        if (iv == null) throw new IllegalArgumentException();
        return decrypt("AES/CBC/PKCS5Padding", bytes, key.getBytes(StandardCharsets.UTF_8), iv.getBytes(StandardCharsets.UTF_8));
    }
    
    public static byte[] decryptAES(byte[] bytes, String key) {
        if (key == null) throw new IllegalArgumentException();
        return decrypt("AES/ECB/PKCS5Padding", bytes, key.getBytes(StandardCharsets.UTF_8), null);
    }
}
