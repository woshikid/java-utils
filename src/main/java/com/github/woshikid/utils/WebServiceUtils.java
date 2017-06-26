package com.github.woshikid.utils;

import java.io.ByteArrayOutputStream;
import java.io.StringReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

/**
 * WebService工具
 * @author kid
 *
 */
public class WebServiceUtils {
	
	private static final Logger logger = LoggerFactory.getLogger(WebServiceUtils.class);
	
	/**
	 * SOAP xml解析异常
	 * @author chenhan
	 *
	 */
	public static class SOAPException extends Exception {
		private static final long serialVersionUID = 1L;
		
		/**
		 * 对方返回的原始文本
		 */
		public String responseText;
		
		public SOAPException(Throwable t, String responseText) {
			super(t);
			this.responseText = responseText;
		}
	}
	
	/**
	 * 将org.w3c.dom.Document转换为字符串
	 * @param xml
	 * @return
	 * @throws Exception
	 */
	private static String xmlToString(Document xml) throws Exception {
		Transformer transformer = TransformerFactory.newInstance().newTransformer();
		transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
		ByteArrayOutputStream cache = new ByteArrayOutputStream();
		transformer.transform(new DOMSource(xml), new StreamResult(cache));
		return cache.toString("UTF-8");
	}
	
	/**
	 * 得到SOAP的http请求头
	 * @param url
	 * @param SOAP12
	 * @return
	 * @throws MalformedURLException 
	 */
	public static Map<String, String> getSOAPHeader(String url, boolean SOAP12) throws MalformedURLException {
		Map<String, String> header = new HashMap<>();
		
		header.put("Accept", "application/soap+xml, text/*");
		header.put("Cache-Control", "no-cache");
		header.put("Pragma", "no-cache");
		header.put("Host", new URL(url).getHost());
		
		if (SOAP12) {
			header.put("Content-Type", "application/soap+xml; charset=utf-8");
		} else {
			header.put("Content-Type", "text/xml; charset=utf-8");
			header.put("SOAPAction", "");
		}
		
		return header;
	}
	
	/**
	 * 组装SOAP报文
	 * @param methodName
	 * @param targetNameSpace
	 * @param paramMap
	 * @param SOAP12 false则为SOAP1.1请求
	 * @return
	 * @throws Exception 
	 */
	public static String toSOAP(String methodName, String targetNameSpace, LinkedHashMap<String, String> paramMap, boolean SOAP12) throws Exception {
		//SOAP命名空间
		String nameSpace = "http://schemas.xmlsoap.org/soap/envelope/";
		if (SOAP12) nameSpace = "http://www.w3.org/2003/05/soap-envelope";
		
		//SOAP encoding style
		String encodingStyle = "http://schemas.xmlsoap.org/soap/encoding/";
		if (SOAP12) encodingStyle = "http://www.w3.org/2001/12/soap-encoding";
		
		//root
		Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
		document.setXmlStandalone(true);
		
		//Envelope
		Element envelope = document.createElementNS(nameSpace, "soapenv:Envelope");
		envelope.setAttribute("xmlns:xsd", "http://www.w3.org/2001/XMLSchema");
		envelope.setAttribute("xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance");
		document.appendChild(envelope);
		
		//Body
		Element body = document.createElement("soapenv:Body");
		envelope.appendChild(body);
		
		//method
		Element method = document.createElement(methodName);
		if (targetNameSpace != null)method.setAttribute("xmlns", targetNameSpace);
		method.setAttribute("soapenv:encodingStyle", encodingStyle);
		body.appendChild(method);
		
		//params
		if (paramMap != null) {
			for (String key : paramMap.keySet()) {
				Element param = document.createElement(key);
				param.setAttribute("xsi:type", "xsd:string");
				param.setTextContent(paramMap.get(key));
				method.appendChild(param);
			}
		}
		
		//Document to String
		return xmlToString(document);
	}
	
	/**
	 * 解析SOAP报文
	 * @param xml
	 * @param SOAP12
	 * @return
	 * @throws Exception
	 */
	public static LinkedHashMap<String, String> fromSOAP(String xml, boolean SOAP12) throws Exception {
		//SOAP命名空间
		String nameSpace = "http://schemas.xmlsoap.org/soap/envelope/";
		if (SOAP12) nameSpace = "http://www.w3.org/2003/05/soap-envelope";
		
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		//打开命名空间感知
		factory.setNamespaceAware(true);
		
		//root
		Document document = factory.newDocumentBuilder().parse(new InputSource(new StringReader(xml)));
		
		//Envelope
		Element envelope = (Element)document.getElementsByTagNameNS(nameSpace, "Envelope").item(0);
		
		//Body
		Element body = (Element)envelope.getElementsByTagNameNS(nameSpace, "Body").item(0);
		
		//method
		Element method = (Element)body.getFirstChild();
		
		//params
		LinkedHashMap<String, String> paramMap = new LinkedHashMap<>();
		NodeList params = method.getChildNodes();
		for (int i = 0; i < params.getLength(); i++) {
			Node node = params.item(i);
			if (node.getNodeType() == Node.ELEMENT_NODE) {
				Element param = (Element)node;
				paramMap.put(param.getTagName(), param.getTextContent());
			}
		}
		
		return paramMap;
	}
	
	/**
	 * 发起WebService请求
	 * @param url
	 * @param methodName
	 * @param targetNameSpace
	 * @param paramMap
	 * @param SOAP12 false则为SOAP1.1请求
	 * @return
	 * @throws Exception
	 */
	public static String invoke(String url, String methodName, String targetNameSpace, LinkedHashMap<String, String> paramMap, boolean SOAP12) throws Exception {
		long threadId = Thread.currentThread().getId();
		logger.info("[{}] invoke url={}, methodName={}", threadId, url, methodName);
		
		if (paramMap != null) {
			for (String key : paramMap.keySet()) {
				String value = paramMap.get(key);
				logger.info("[{}] param:{}={}", threadId, key, value);
			}
		}
		
		//设置SOAP请求头
		Map<String, String> header = getSOAPHeader(url, SOAP12);
		
		//得到请求的SOAP报文
		String requestText = toSOAP(methodName, targetNameSpace, paramMap, SOAP12);
		logger.info("[{}] requestText={}", threadId, requestText);
		
		//invoke
		HttpUtils.Response response = HttpUtils.request(url, header, requestText.getBytes("UTF-8"));
		
		//返回的原始文本
		String responseText;
		if (response.charset == null) {
			responseText = new String(response.data, "UTF-8");
		} else {
			responseText = new String(response.data, response.charset);
		}
		logger.info("[{}] responseText={}", threadId, responseText);
		
		//处理SOAP信息
		try {
			//解析SOAP报文
			LinkedHashMap<String, String> responseMap = fromSOAP(responseText, SOAP12);
			
			//返回的结果文本
			String returnText = responseMap.values().iterator().next();
			
			logger.info("[{}] returnText={}", threadId, returnText);
			return returnText;
		} catch (Exception e) {
			logger.error("[" + threadId + "] error when dealing with soap message", e);
			throw new SOAPException(e, responseText);
		}
	}
	
}
