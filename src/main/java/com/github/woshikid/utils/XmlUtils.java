package com.github.woshikid.utils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.adapters.XmlAdapter;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

/**
 * xml转换工具类
 * @author kid
 *
 */
public class XmlUtils {

	/**
	 * 日期转换适配器
	 * @author chenhan
	 *
	 */
	public static class DateAdapter extends XmlAdapter<String, Date> {
		private SimpleDateFormat formater = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		
		public String marshal(Date date) {
			return formater.format(date);
		}
		
		public Date unmarshal(String source) throws ParseException {
			return formater.parse(source);
		}
	}
	
	/**
	 * 将对象转换为xml字符串
	 * @param object
	 * @param encoding
	 * @param fragment 如果为true，则不输出xml头
	 * <?xml version="1.0" encoding="UTF-8"?>
	 * @XmlRootElement(name="Config")
	 * @XmlAccessorType(XmlAccessType.FIELD)
	 * @XmlElementWrapper(name="Servers")
	 * @XmlElement(name="Server")
	 * @XmlJavaTypeAdapter(JaxbDateAdapter.class)
	 * @return
	 */
	public static String toXml(Object object, String encoding, boolean fragment) {
		try {
			JAXBContext context = JAXBContext.newInstance(object.getClass());
			Marshaller marshaller = context.createMarshaller();
			marshaller.setProperty(Marshaller.JAXB_ENCODING, encoding);
			marshaller.setProperty(Marshaller.JAXB_FRAGMENT, fragment);
			
			StringWriter writer = new StringWriter();
			marshaller.marshal(object, writer);
			return writer.toString();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	/**
	 * 将对象转换为xml字符串
	 * @param object
	 * @param encoding
	 * @return
	 */
	public static String toXml(Object object, String encoding) {
		return toXml(object, encoding, false);
	}
	
	/**
	 * 将Document转换为xml字符串
	 * @param document
	 * @param encoding
	 * @return
	 */
	public static String toXml(Document document, String encoding) {
		try {
			Transformer transformer = TransformerFactory.newInstance().newTransformer();
			transformer.setOutputProperty(OutputKeys.ENCODING, encoding);
			
			ByteArrayOutputStream cache = new ByteArrayOutputStream();
			transformer.transform(new DOMSource(document), new StreamResult(cache));
			return cache.toString(encoding);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	/**
	 * 将xml字符串转换为对象
	 * @param xml
	 * @param targetClass
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public static <T> T toObject(String xml, Class<T> targetClass) {
		try {
			JAXBContext context = JAXBContext.newInstance(targetClass);
			Unmarshaller unmarshaller = context.createUnmarshaller();
			return (T)unmarshaller.unmarshal(new StringReader(xml));
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	/**
	 * 将Document转换为对象
	 * @param document
	 * @param targetClass
	 * @return
	 */
	public static <T> T toObject(Document document, Class<T> targetClass) {
		String xml = toXml(document, "UTF-8");
		return toObject(xml, targetClass);
	}
	
	/**
	 * 将Element转换为String,Map或List
	 * @param element
	 * @return
	 */
	public static Object toObject(Element element) {
		if (element == null) return null;
		
		NodeList nodeList = element.getChildNodes();
		
		List<Node> children = new ArrayList<>();
		for (int i = 0; i < nodeList.getLength(); i++) {
			children.add(nodeList.item(i));
		}
		
		//忽略空格形成的空白节点
		if (children.size() > 1) {
			for (int i = children.size() - 1; i >= 0; i--) {
				if (children.get(i).getNodeType() != Node.ELEMENT_NODE) {
					children.remove(i);
				}
			}
		}
		
		if (children.size() == 0) {
			return "";
		} else if (children.size() == 1) {
			Node node = children.get(0);
			
			if (node.getNodeType() == Node.ELEMENT_NODE) {
				Element child = (Element)node;
				
				Map<String, Object> map = new HashMap<>();
				map.put(child.getTagName(), toObject(child));
				
				return map;
			} else {
				return node.getTextContent();
			}
		} else {
			Element child0 = (Element)children.get(0);
			Element child1 = (Element)children.get(1);
			
			if (child0.getTagName().equals(child1.getTagName())) {
				List<Object> list = new ArrayList<>();
				
				for (Node node : children) {
					Element child = (Element)node;
					list.add(toObject(child));
				}
				
				return list;
			} else {
				Map<String, Object> map = new HashMap<>();
				
				for (Node node : children) {
					Element child = (Element)node;
					map.put(child.getTagName(), toObject(child));
				}
				
				return map;
			}
		}
	}
	
	/**
	 * 将Document转换为String,Map或List
	 * @param document
	 * @return
	 */
	public static Object toObject(Document document) {
		return toObject(document.getDocumentElement());
	}

	/**
	 * 将xml字符串转换为String,Map或List
	 * @param xml
	 * @return
	 */
	public static Object toObject(String xml) {
		Document document = toDocument(xml);
		return toObject(document);
	}
	
	/**
	 * 将Map转成Element
	 * @param element
	 * @param map
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public static Element toElement(Element element, Map<String, ?> map) {
		Document document = element.getOwnerDocument();
		
		for (String key : map.keySet()) {
			Object value = map.get(key);
			Element child = document.createElement(key);
			
			if (value instanceof Map) {
				toElement(child, (Map<String, ?>)value);
			} else if (value instanceof List) {
				if (key.endsWith("s")) key = key.substring(0, key.length() - 1);
				toElement(child, (List<?>)value, key);
			} else {
				child.setTextContent(String.valueOf(value));
			}
			
			element.appendChild(child);
		}
		
		return element;
	}
	
	/**
	 * 将List转成Element
	 * @param element
	 * @param list
	 * @param tagName
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public static Element toElement(Element element, List<?> list, String tagName) {
		Document document = element.getOwnerDocument();
		
		for (Object value : list) {
			Element child = document.createElement(tagName);
			
			if (value instanceof Map) {
				toElement(child, (Map<String, ?>)value);
			} else if (value instanceof List) {
				toElement(child, (List<?>)value, tagName);
			} else {
				child.setTextContent(String.valueOf(value));
			}
			
			element.appendChild(child);
		}
		
		return element;
	}
	
	/**
	 * 将Map转成Document
	 * @param map
	 * @param rootName
	 * @return
	 */
	public static Document toDocument(Map<String, ?> map, String rootName) {
		try {
			Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
			Element root = document.createElement(rootName);
			document.appendChild(toElement(root, map));
			return document;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	/**
	 * 将List转成Document
	 * @param list
	 * @param rootName
	 * @param tagName
	 * @return
	 */
	public static Document toDocument(List<?> list, String rootName, String tagName) {
		try {
			Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
			Element root = document.createElement(rootName);
			document.appendChild(toElement(root, list, tagName));
			return document;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	/**
	 * 将xml字符串转换为Document
	 * @param xml
	 * @return
	 */
	public static Document toDocument(String xml) {
		try {
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			factory.setNamespaceAware(true);
			return factory.newDocumentBuilder().parse(new InputSource(new StringReader(xml)));
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	/**
	 * 将输入流转换为Document
	 * @param in
	 * @return
	 */
	public static Document toDocument(InputStream in) {
		try {
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			factory.setNamespaceAware(true);
			return factory.newDocumentBuilder().parse(in);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	/**
	 * 将字节数组转换为Document
	 * @param data
	 * @return
	 */
	public static Document toDocument(byte[] data) {
		return toDocument(new ByteArrayInputStream(data));
	}
	
	/**
	 * 将对象转换为Document
	 * @param object
	 * @return
	 */
	public static Document toDocument(Object object) {
		String xml = toXml(object, "UTF-8");
		return toDocument(xml);
	}
	
}
