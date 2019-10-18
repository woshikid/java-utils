package com.github.woshikid.utils;

import java.io.Closeable;
import java.io.File;
import java.io.UnsupportedEncodingException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import javax.activation.DataHandler;
import javax.activation.FileDataSource;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Store;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.mail.internet.MimeUtility;

import com.sun.mail.util.MailSSLSocketFactory;

/**
 * JavaMail工具类
 * @author kid
 */
public class EmailUtils implements Closeable {

	private Session session;
	
	private Transport transport;
	
	private Store store;
	
	private List<Folder> folders = new ArrayList<>();
	
	private EmailUtils(Session session, Transport transport) {
		this.session = session;
		this.transport = transport;
	}
	
	private EmailUtils(Store store) {
		this.store = store;
	}
	
	/**
	 * 是否打印debug信息
	 */
	public static boolean debug = false;
	
	/**
	 * 加密认证方式
	 */
	public static enum Encryption {
		PLAIN, SSL, STARTTLS
	}
	
	private static final ThreadLocal<Boolean> trustAllHosts = new ThreadLocal<>();
	
	/**
	 * 信任自签名的证书
	 * @param trust
	 */
	public static void setTrustAllHosts(boolean trust) {
		trustAllHosts.set(trust);
	}
	
	/**
	 * 根据协议与加密方式获得Session
	 * @param protocol
	 * @param encryption
	 * @return
	 */
	private static Session getSession(String protocol, Encryption encryption) {
		Properties config = new Properties();
		
		if ("smtp".equals(protocol)) {
			config.setProperty("mail.smtp.auth", "true");
		}
		
		if (encryption == Encryption.SSL) {
			config.setProperty("mail." + protocol + ".ssl.enable", "true");
		} else if (encryption == Encryption.STARTTLS) {
			config.setProperty("mail." + protocol + ".starttls.enable", "true");
		}
		
		Boolean trust = trustAllHosts.get();
		if (trust != null) {
			trustAllHosts.remove();
			
			try {
				MailSSLSocketFactory sf = new MailSSLSocketFactory();
				sf.setTrustAllHosts(trust);
				config.put("mail." + protocol + ".ssl.socketFactory", sf);
			} catch (GeneralSecurityException e) {
				throw new RuntimeException(e);
			}
		}
		
		Session session = Session.getInstance(config);
		session.setDebug(debug);
		
		return session;
	}
	
	/**
	 * 使用smtp协议连接服务器
	 * @param host
	 * @param port
	 * @param user
	 * @param password
	 * @param encryption 加密方式
	 * @return 邮件发送工具
	 * @throws MessagingException
	 */
	public static EmailUtils smtp(String host, int port, String user, String password, Encryption encryption) throws MessagingException {
		Session session = getSession("smtp", encryption);
		
		Transport transport = session.getTransport("smtp");
		transport.connect(host, port, user, password);
		
		return new EmailUtils(session, transport);
	}
	
	/**
	 * 发送邮件<br/>
	 * 若想在正文中引用附件<br/>
	 * 可以使用&lt;img src='cid:filename'/>的方式
	 * @param from 发件人邮箱
	 * @param to 收件人列表，逗号分隔
	 * @param cc 抄送列表，逗号分隔
	 * @param bcc 暗送列表，逗号分隔
	 * @param subject 主题
	 * @param content 正文
	 * @param attachments 附件
	 * @throws MessagingException
	 */
	public void send(String from, String to, String cc, String bcc, String subject, String content, File[] attachments) throws MessagingException {
		MimeMessage message = new MimeMessage(session);
		message.setFrom(new InternetAddress(from));
		message.setRecipients(MimeMessage.RecipientType.TO, to);
		message.setRecipients(MimeMessage.RecipientType.CC, cc);
		message.setRecipients(MimeMessage.RecipientType.BCC, bcc);
		message.setSubject(subject);
		
		if (attachments == null) {
			message.setContent(content, "text/html; charset=UTF-8");
		} else {
			MimeBodyPart html = new MimeBodyPart();
			html.setContent(content, "text/html; charset=UTF-8");
			
			MimeMultipart multipart = new MimeMultipart();
			multipart.addBodyPart(html);
			
			for (File attachment : attachments) {
				DataHandler dataHandler = new DataHandler(new FileDataSource(attachment));
				
				MimeBodyPart bodyPart = new MimeBodyPart();
				try {
					bodyPart.setDataHandler(dataHandler);
					bodyPart.setFileName(MimeUtility.encodeText(dataHandler.getName()));
					bodyPart.setContentID(dataHandler.getName());
				} catch (UnsupportedEncodingException e) {
					throw new RuntimeException(e);
				}
				
				multipart.addBodyPart(bodyPart);
			}
			
			message.setContent(multipart);
		}
		
		transport.sendMessage(message, message.getAllRecipients());
	}
	
	/**
	 * 使用pop3协议连接服务器
	 * @param host
	 * @param port
	 * @param user
	 * @param password
	 * @param encryption 加密方式
	 * @return 邮件接收工具
	 * @throws MessagingException
	 */
	public static EmailUtils pop3(String host, int port, String user, String password, Encryption encryption) throws MessagingException {
		Session session = getSession("pop3", encryption);
		
		Store store = session.getStore("pop3");
		store.connect(host, port, user, password);
		
		return new EmailUtils(store);
	}
	
	/**
	 * 使用imap协议连接服务器
	 * @param host
	 * @param port
	 * @param user
	 * @param password
	 * @param encryption 加密方式
	 * @return 邮件接收工具
	 * @throws MessagingException
	 */
	public static EmailUtils imap(String host, int port, String user, String password, Encryption encryption) throws MessagingException {
		Session session = getSession("imap", encryption);
		
		Store store = session.getStore("imap");
		store.connect(host, port, user, password);
		
		return new EmailUtils(store);
	}
	
	/**
	 * 接收邮件，imap可选择文件夹<br/>
	 * 若想删除邮件，可设置message.setFlag(Flags.Flag.DELETED, true)
	 * @param folderName 文件夹
	 * @return
	 * @throws MessagingException
	 */
	public Message[] receive(String folderName) throws MessagingException {
		Folder folder = store.getFolder(folderName);
		folder.open(Folder.READ_WRITE);
		
		// 记录已打开的文件夹
		folders.add(folder);
		
		return folder.getMessages();
	}
	
	/**
	 * 接收邮件，pop3只能使用收件夹<br/>
	 * 若想删除邮件，可设置message.setFlag(Flags.Flag.DELETED, true)
	 * @return
	 * @throws MessagingException
	 */
	public Message[] receive() throws MessagingException {
		return receive("INBOX");
	}

	/**
	 * 关闭文件夹<br/>
	 * 关闭连接
	 */
	@Override
	public void close() {
		IOUtils.close(transport);
		folders.forEach(folder -> IOUtils.close(folder, true));
		IOUtils.close(store);
	}
	
}
