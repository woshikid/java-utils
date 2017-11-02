package com.github.woshikid.utils;

import javax.crypto.Cipher;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

/**
 * @author kid
 */
public class RSAUtils {

    public static KeyPair generateKeyPair(int keySize) {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(keySize);
            return generator.generateKeyPair();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static PrivateKey getPrivateKey(String key) throws Exception {
        byte[] keyBytes = Base64.getDecoder().decode(key.replaceAll("-+[^-]+-+", ""));
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(keyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        return keyFactory.generatePrivate(keySpec);
    }

    public static PublicKey getPublicKey(String key) throws Exception {
        byte[] keyBytes = Base64.getDecoder().decode(key.replaceAll("-+[^-]+-+", ""));
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(keyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        return keyFactory.generatePublic(keySpec);
    }

    public static Cipher getCipher(String algorithm, Provider provider, int mode, Key key) {
        try {
            Cipher cipher;

            if (provider == null) {
                cipher = Cipher.getInstance(algorithm);
            } else {
                cipher = Cipher.getInstance(algorithm, provider);
            }

            cipher.init(mode, key);
            return cipher;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static Cipher getEncryptCipher(Key key) {
        return getCipher("RSA/ECB/PKCS1Padding", null, Cipher.ENCRYPT_MODE, key);
    }

    public static Cipher getDecryptCipher(Key key) {
        return getCipher("RSA/ECB/PKCS1Padding", null, Cipher.DECRYPT_MODE, key);
    }

    private static byte[] doFinal(Cipher cipher, byte[] bytes, int blockSize) throws Exception {
        ByteArrayInputStream in = new ByteArrayInputStream(bytes);
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        int size;
        byte[] block = new byte[blockSize];
        while ((size = in.read(block)) != -1) {
            out.write(cipher.doFinal(block, 0, size));
        }

        return out.toByteArray();
    }

    public static byte[] encrypt(Cipher cipher, byte[] bytes) {
        int blockSize = cipher.getBlockSize();
        //SunJCE returns 0
        if (blockSize == 0) {
            blockSize = cipher.getOutputSize(0) - 11;
        }

        try {
            return doFinal(cipher, bytes, blockSize);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static byte[] decrypt(Cipher cipher, byte[] bytes) {
        int blockSize = cipher.getBlockSize();
        //SunJCE returns 0
        if (blockSize == 0) {
            blockSize = cipher.getOutputSize(0);
        }

        try {
            return doFinal(cipher, bytes, blockSize);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static byte[] encrypt(String algorithm, Provider provider, byte[] bytes, Key key) {
        if (bytes == null) return null;

        Cipher cipher = getCipher(algorithm, provider, Cipher.ENCRYPT_MODE, key);
        return encrypt(cipher, bytes);
    }

    public static byte[] decrypt(String algorithm, Provider provider, byte[] bytes, Key key) {
        if (bytes == null) return null;

        Cipher cipher = getCipher(algorithm, provider, Cipher.DECRYPT_MODE, key);
        return decrypt(cipher, bytes);
    }

    public static byte[] encrypt(byte[] bytes, Key key) {
        if (bytes == null) return null;

        Cipher cipher = getEncryptCipher(key);
        return encrypt(cipher, bytes);
    }

    public static byte[] decrypt(byte[] bytes, Key key) {
        if (bytes == null) return null;

        Cipher cipher = getDecryptCipher(key);
        return decrypt(cipher, bytes);
    }
}
