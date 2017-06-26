package com.github.woshikid.utils;

import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.ZooKeeper.States;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;

/**
 * 
 * @author kid
 *
 */
public abstract class AutoWatcher implements Watcher, InitializingBean {

	private final static Logger logger = LoggerFactory.getLogger(AutoWatcher.class);
	
	private ZooKeeper zk;
	private String path;
	
	public AutoWatcher(ZooKeeper zk, String path) {
		this.zk = zk;
		this.path = path;
	}
	
	private void waitForConnect() {
		for (int i = 0; i < 10; i++) {
			if (zk.getState().equals(States.CONNECTED)) break;
			
			try {
				Thread.sleep(1000);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	public final void register() {
		waitForConnect();
		
		try {
			doRegister(zk, path);
		} catch (Exception e) {
			logger.error("Error while registering watcher", e);
			throw new RuntimeException(e);
		}
	}
	
	protected abstract void doRegister(ZooKeeper zk, String path) throws Exception;
	
	public final void process(WatchedEvent event) {
		try {
			doProcess(event);
		} catch (Exception e) {
			logger.error("Error while calling watcher", e);
		} finally {
			register();
		}
	}
	
	protected abstract void doProcess(WatchedEvent event) throws Exception;
	
	public final void afterPropertiesSet() throws Exception {
		register();
	}

}
