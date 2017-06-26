package com.github.woshikid.utils;

import org.apache.zookeeper.ZooKeeper;

/**
 * 
 * @author kid
 *
 */
public abstract class DataWatcher extends AutoWatcher {

	public DataWatcher(ZooKeeper zk, String path) {
		super(zk, path);
	}
	
	protected final void doRegister(ZooKeeper zk, String path) throws Exception {
		zk.getData(path, this, null);
	}
	
}
