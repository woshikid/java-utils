package com.github.woshikid.utils;

import org.apache.zookeeper.ZooKeeper;

/**
 * 
 * @author kid
 *
 */
public abstract class ChildrenWatcher extends AutoWatcher {

	public ChildrenWatcher(ZooKeeper zk, String path) {
		super(zk, path);
	}
	
	protected final void doRegister(ZooKeeper zk, String path) throws Exception {
		zk.getChildren(path, this);
	}
	
}
