package com.github.woshikid.utils;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.security.Key;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.crypto.Cipher;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

/**
 * 
 * @author kid
 *
 */
public class RSAUtils {

	@SuppressWarnings("unused")
	private static final String PUBLIC = ".public.pem";//公钥文件名后缀
	private static final String PRIVATE = ".private.pem";//私钥文件名后缀
	private static String ENCRYPTKEY = PRIVATE;
	private static String DECRYPTKEY = PRIVATE;
	private static final String ALGORITHM = "RSA/ECB/PKCS1Padding";
	
	//读取的公钥私钥缓存
	private static Map<String, Key> keyCache = new ConcurrentHashMap<String, Key>();
	//微型缓冲池
	private static Map<String, Cipher> cipherCache = new ConcurrentHashMap<String, Cipher>();
	
	private String client;
	private Cipher encryptCipher;//由于Cipher是线程非安全，不能作为静态变量
	private Cipher decryptCipher;
	
	/**
	 * 从字符串中加载私钥<br>
	 * 加载时使用的是PKCS8EncodedKeySpec（PKCS#8编码的Key指令）。
	 * 
	 * @param privateKeyStr
	 * @return
	 * @throws Exception
	 */
	private PrivateKey getPrivateKey(String privateKeyStr) throws Exception {
		byte[] buffer = Base64.decode(privateKeyStr);
		PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(buffer);
		KeyFactory keyFactory = KeyFactory.getInstance("RSA");
		return keyFactory.generatePrivate(keySpec);
	}
	
	/**
	 * 从字符串中加载公钥
	 * 
	 * @param publicKeyStr
	 *            公钥数据字符串
	 * @throws Exception
	 *             加载公钥时产生的异常
	 */
	private static PublicKey getPublicKey(String publicKeyStr) throws Exception {
		byte[] buffer = Base64.decode(publicKeyStr);
		KeyFactory keyFactory = KeyFactory.getInstance("RSA");
		X509EncodedKeySpec keySpec = new X509EncodedKeySpec(buffer);
		return keyFactory.generatePublic(keySpec);
	}
	
	private String getKeyString(String keyName) throws Exception {
		InputStream is = this.getClass().getClassLoader().getResourceAsStream(keyName);
		BufferedReader br = new BufferedReader(new InputStreamReader(is, "UTF-8"));
		String temp = null;
		StringBuffer sb = new StringBuffer();
		while ((temp = br.readLine()) != null) {
			sb.append(temp);
		}
		String keyString =  sb.toString().replaceAll("-+[^-]+KEY-+", "");
		br.close();
		return keyString;
	}
	
	private Key getKey(String client, String type) throws Exception {
		String keyName = client + type;
		Key key = keyCache.get(keyName);
		
		if (key == null) {
			synchronized (RSAUtils.class) {
				key = keyCache.get(keyName);
				if (key == null) {
					String keyString = getKeyString(keyName);
					if (type.equals(PRIVATE)) {
						key = getPrivateKey(keyString);
					} else {
						key = getPublicKey(keyString);
					}
					keyCache.put(keyName, key);
				}
			}
		}
		
		return key;
	}
	
	public RSAUtils(String client) {
		this.client = client;
	}
	
	public void destroy() {
		if (encryptCipher != null) {
			synchronized (RSAUtils.class) {
				String cipherName = client + ".encryptCipher";
				cipherCache.put(cipherName, encryptCipher);
				encryptCipher = null;
			}
		}
		
		if (decryptCipher != null) {
			synchronized (RSAUtils.class) {
				String cipherName = client + ".decryptCipher";
				cipherCache.put(cipherName, decryptCipher);
				decryptCipher = null;
			}
		}
	}
	
	protected void finalize() throws Throwable {
		destroy();
		super.finalize();
	}
	
	public byte[] encryptData(byte[] data) throws Exception {
		if (data == null) return null;
		
		//lazy init
		if (encryptCipher == null) {
			//try to load cache
			synchronized (RSAUtils.class) {
				String cipherName = client + ".encryptCipher";
				encryptCipher = cipherCache.get(cipherName);
				cipherCache.remove(cipherName);
			}
			
			if (encryptCipher == null) {
				Key key = getKey(client, ENCRYPTKEY);
				encryptCipher = Cipher.getInstance(ALGORITHM, new BouncyCastleProvider());
				encryptCipher.init(Cipher.ENCRYPT_MODE, key);
			}
		}
		
		int blockSize = encryptCipher.getBlockSize();
		int outputSize = encryptCipher.getOutputSize(0);
		if (blockSize == 0) {//SunJCE returns 0
			blockSize = 117;
			outputSize = 128;
		}
		
		int pieces = (data.length - 1) / blockSize + 1;
		int rest = data.length % blockSize;
		if (rest == 0 && data.length != 0) rest = blockSize;
		byte[] result = new byte[pieces * outputSize];
		
		for (int p = 0; p < pieces; p++) {
			//last piece length = rest or blockSize
			int length = (p == pieces - 1) ? rest : blockSize;
			encryptCipher.doFinal(data, p * blockSize, length, result, p * outputSize);
		}
		
		return result;
	}
	
	public byte[] decryptData(byte[] data) throws Exception {
		if (data == null) return null;
		
		//lazy init
		if (decryptCipher == null) {
			//try to load cache
			synchronized (RSAUtils.class) {
				String cipherName = client + ".decryptCipher";
				decryptCipher = cipherCache.get(cipherName);
				cipherCache.remove(cipherName);
			}
			
			if (decryptCipher == null) {
				Key key = getKey(client, DECRYPTKEY);
				decryptCipher = Cipher.getInstance(ALGORITHM, new BouncyCastleProvider());
				decryptCipher.init(Cipher.DECRYPT_MODE, key);
			}
		}
		
		int blockSize = decryptCipher.getBlockSize();
		if (blockSize == 0) {//SunJCE returns 0
			blockSize = 128;
		}
		
		int pieces = data.length / blockSize;
		if (pieces == 0) throw new Exception("encrypted data is too short");
		
		ByteArrayOutputStream result = new ByteArrayOutputStream();
		for (int p = 0; p < pieces; p++) {
			result.write(decryptCipher.doFinal(data, p * blockSize, blockSize));
		}
		
		return result.toByteArray();
	}
	
	public String encryptString(String param) throws Exception {
		if (param == null) return null;
		byte[] encryptedByte = encryptData(param.getBytes("UTF-8"));
		return Base64.encode(encryptedByte);
	}
	
	public String decryptString(String param) throws Exception {
		if (param == null) return null;
		byte[] decryptedByte = decryptData(Base64.decode(param));
		return new String(decryptedByte, "UTF-8");
	}
	
	@SuppressWarnings("unchecked")
	private <T> T opearteObject(T object, boolean encrypt, boolean withSuperClass) throws Exception {
		if (object == null) return null;
		
		if (object instanceof String) {
			if (encrypt) {
				return (T)encryptString((String)object);
			} else {
				return (T)decryptString((String)object);
			}
		}
		
		Class<?> clazz = object.getClass();
		Field[] allFields = clazz.getDeclaredFields();
		while (withSuperClass && (clazz = clazz.getSuperclass()) != null) {
			Field[] newFields = clazz.getDeclaredFields();
			if (newFields.length == 0) continue;
			
			Field[] fields = allFields;
			allFields = new Field[fields.length + newFields.length];
			System.arraycopy(fields, 0, allFields, 0, fields.length);
			System.arraycopy(newFields, 0, allFields, fields.length, newFields.length);
		}
		
		for (Field field : allFields) {
			if (field.getType().equals(String.class)) {
				field.setAccessible(true);
				String value = (String) field.get(object);
				if (encrypt) {
					value = encryptString(value);
				} else {
					value = decryptString(value);
				}
				field.set(object, value);
			} else if (field.getType().equals(String[].class)) {
				field.setAccessible(true);
				String[] value = (String[]) field.get(object);
				
				for (int i = 0; i < value.length; i++) {
					if (encrypt) {
						value[i] = encryptString(value[i]);
					} else {
						value[i] = decryptString(value[i]);
					}
				}
			}
		}
		
		return object;
	}
	
	public <T> T encryptObject(T object) throws Exception {
		return opearteObject(object, true, false);
	}
	
	public <T> T decryptObject(T object) throws Exception {
		return opearteObject(object, false, false);
	}
	
	public <T> T encryptObjectWithSuperClass(T object) throws Exception {
		return opearteObject(object, true, true);
	}
	
	public <T> T decryptObjectWithSuperClass(T object) throws Exception {
		return opearteObject(object, false, true);
	}
	
	public static <T> T encrypt(T object, String client) throws Exception {
		RSAUtils rsaUtil = new RSAUtils(client);
		T t = rsaUtil.encryptObject(object);
		rsaUtil.destroy();
		return t;
	}
	
	public static <T> T decrypt(T object, String client) throws Exception {
		RSAUtils rsaUtil = new RSAUtils(client);
		T t = rsaUtil.decryptObject(object);
		rsaUtil.destroy();
		return t;
	}
	
	public static <T> T deepEncrypt(T object, String client) throws Exception {
		RSAUtils rsaUtil = new RSAUtils(client);
		T t = rsaUtil.encryptObjectWithSuperClass(object);
		rsaUtil.destroy();
		return t;
	}
	
	public static <T> T deepDecrypt(T object, String client) throws Exception {
		RSAUtils rsaUtil = new RSAUtils(client);
		T t = rsaUtil.decryptObjectWithSuperClass(object);
		rsaUtil.destroy();
		return t;
	}
	
}
