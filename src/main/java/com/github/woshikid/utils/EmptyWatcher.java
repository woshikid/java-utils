package com.github.woshikid.utils;

import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * @author kid
 *
 */
public class EmptyWatcher implements Watcher {

	private final static Logger logger = LoggerFactory.getLogger(EmptyWatcher.class);
	
	public void process(WatchedEvent event) {
		logger.info(String.valueOf(event));
	}
}
