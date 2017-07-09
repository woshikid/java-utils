package com.github.woshikid.utils;

import java.security.SecureRandom;
import java.security.cert.X509Certificate;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

/**
 * 
 * @author kid
 *
 */
public class HttpsHelper {
	
	private static SSLContext defaultSSLContext = null;
	private static HostnameVerifier defaultHostnameVerifier = null;
	
	private static SSLContext getSSLContext() throws Exception {
		TrustManager trustAllCerts = new X509TrustManager() {
			public void checkClientTrusted(X509Certificate[] cert, String authType) {}
			public void checkServerTrusted(X509Certificate[] cert, String authType) {}
			public X509Certificate[] getAcceptedIssuers() {return null;}
		};
		
		SSLContext context = SSLContext.getInstance("TLS");
		context.init(null, new TrustManager[]{trustAllCerts}, new SecureRandom());
		
		return context;
	}
	
	public static void disableTrustManager(HttpsURLConnection connection) {
		try {
			SSLContext context = getSSLContext();
			connection.setSSLSocketFactory(context.getSocketFactory());
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	public static void disableHostnameVerifier(HttpsURLConnection connection) {
		connection.setHostnameVerifier((h, s) -> true);
	}
	
	public static void disableTrustManager() {
		if (defaultSSLContext != null) return;
		
		try {
			synchronized (HttpsHelper.class) {
				if (defaultSSLContext != null) return;
				defaultSSLContext = SSLContext.getDefault();
				
				SSLContext context = getSSLContext();
				SSLContext.setDefault(context);
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	public static void disableHostnameVerifier() {
		if (defaultHostnameVerifier != null) return;
		
		synchronized (HttpsHelper.class) {
			if (defaultHostnameVerifier != null) return;
			defaultHostnameVerifier = HttpsURLConnection.getDefaultHostnameVerifier();
			
			HttpsURLConnection.setDefaultHostnameVerifier((h, s) -> true);
		}
	}
	
	public static void enableTrustManager() {
		if (defaultSSLContext == null) return;
		
		synchronized (HttpsHelper.class) {
			if (defaultSSLContext == null) return;
			
			SSLContext.setDefault(defaultSSLContext);
			defaultSSLContext = null;
		}
	}
	
	public static void enableHostnameVerifier() {
		if (defaultHostnameVerifier == null) return;
		
		synchronized (HttpsHelper.class) {
			if (defaultHostnameVerifier == null) return;
			
			HttpsURLConnection.setDefaultHostnameVerifier(defaultHostnameVerifier);
			defaultHostnameVerifier = null;
		}
	}
	
}
