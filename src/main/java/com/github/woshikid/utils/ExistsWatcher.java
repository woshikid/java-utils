package com.github.woshikid.utils;

import org.apache.zookeeper.ZooKeeper;

/**
 * 
 * @author kid
 *
 */
public abstract class ExistsWatcher extends AutoWatcher {

	public ExistsWatcher(ZooKeeper zk, String path) {
		super(zk, path);
	}
	
	protected final void doRegister(ZooKeeper zk, String path) throws Exception {
		zk.exists(path, this);
	}
	
}
