package com.github.woshikid.utils;

import javax.xml.namespace.QName;

import org.apache.axis2.addressing.EndpointReference;
import org.apache.axis2.client.Options;
import org.apache.axis2.rpc.client.RPCServiceClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Axis2的WebService客户端
 * @author kid
 *
 */
public class Axis2Utils {

	private static final Logger logger = LoggerFactory.getLogger(Axis2Utils.class);
	
	/**
	 * 发起WebService请求
	 * @param url
	 * @param methodName
	 * @param targetNameSpace wsdl:definitions标签targetNamespace属性
	 * @param params 调用方法的参数值
	 * @return
	 * @throws Exception
	 */
	@SuppressWarnings("rawtypes")
	public static String invoke(String url, String methodName, String targetNameSpace, Object... params) throws Exception {
		long threadId = Thread.currentThread().getId();
		logger.info("[{}] invoke url={}, methodName={}", threadId, url, methodName);
		
		for (Object param : params) {
			logger.info("[{}] param:{}", threadId, param);
		}
		
		//使用RPC方式调用WebService
		RPCServiceClient serviceClient = new RPCServiceClient();
		Options options = serviceClient.getOptions();
		
		//指定调用WebService的URL
		EndpointReference targetEPR = new EndpointReference(url);
		options.setTo(targetEPR);
		
		//调用方法返回值的数据类型的Class对象
		Class[] classes = new Class[] {String.class};
		
		//调用方法名及WSDL文件的命名空间
		QName qName = new QName(targetNameSpace, methodName);
		
		//执行方法获取返回值
		////没有返回值的方法使用serviceClient.invokeRobust(qName, params)
		Object result = serviceClient.invokeBlocking(qName, params, classes)[0];
		
		logger.info("[{}] responseObject={}", threadId, result);
		return String.valueOf(result);
	}
}
