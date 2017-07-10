package com.github.woshikid.utils;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.apache.zookeeper.ZooKeeper;

/**
 * 
 * @author kid
 *
 */
public class ZooKeeperUtils {

	private static ZooKeeper zk = null;
	
	static {
		Properties config = new Properties();
		
		try (InputStream in = ZooKeeperUtils.class.getResourceAsStream("/zookeeper.properties")) {
			config.load(in);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		
		String enable = config.getProperty("enable");
		if ("true".equals(enable)) {
			String connectString = config.getProperty("connectString");
			int sessionTimeout = Integer.parseInt(config.getProperty("sessionTimeout"));
			
			try {
				zk = new ZooKeeper(connectString, sessionTimeout, new EmptyWatcher());
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
	}
	
	public static ZooKeeper getZooKeeper() {
		return zk;
	}
	
}
