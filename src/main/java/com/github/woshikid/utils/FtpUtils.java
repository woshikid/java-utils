package com.github.woshikid.utils;

import sun.net.ftp.FtpClient;
import sun.net.ftp.FtpDirEntry;
import sun.net.ftp.FtpProtocolException;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * sun的ftp工具
 * JDK 1.8
 * @author kid
 */
public class FtpUtils implements Closeable {
	
	private FtpClient ftp;

	private FtpUtils(FtpClient ftp) {
		this.ftp = ftp;
	}

	private static final ThreadLocal<Integer> ftpTimeout = new ThreadLocal<>();

	/**
	 * 设置超时时间
	 * 默认为60000
	 * 本线程有效，并且在调用connect方法后自动清除(仅生效一次)
	 * @param timeout
	 */
	public static void setTimeout(int timeout) {
		ftpTimeout.set(timeout);
	}

	/**
	 * 连接指定的ftp地址并返回实例
	 * @param ip
	 * @param port
	 * @return
	 * @throws IOException
	 * @throws FtpProtocolException
	 */
	public static FtpUtils connect(String ip, int port) throws IOException, FtpProtocolException {
		FtpClient ftp = FtpClient.create();

		//设置超时时间
		Integer timeout = ftpTimeout.get();
		if (timeout == null) {
			timeout = 60000;
		} else {
			ftpTimeout.remove();
		}
		ftp.setConnectTimeout(timeout);
		ftp.setReadTimeout(timeout);
		
		ftp.connect(new InetSocketAddress(ip, port));
		return new FtpUtils(ftp);
	}

	/**
	 * 用指定的用户名密码登录服务器
	 * @param username
	 * @param password
	 * @throws IOException
	 * @throws FtpProtocolException
	 */
	public void login(String username, String password) throws IOException, FtpProtocolException {
		ftp.login(username, password.toCharArray());
		ftp.setBinaryType();
	}

	/**
	 * 关闭ftp连接
	 * @throws IOException
	 */
	public void close() throws IOException {
		ftp.close();
	}

	/**
	 * 遍历文件名
	 * @param path
	 * @return
	 * @throws IOException
	 * @throws FtpProtocolException
	 */
	public List<String> list(String path) throws IOException, FtpProtocolException {
		Iterator<FtpDirEntry> it = ftp.listFiles(path);
		List<String> list = new ArrayList<>();
		
		while (it.hasNext()) {
			String name = it.next().getName();
			list.add(name);
		}
		
		return list;
	}

	/**
	 * 变更当前目录
	 * @param path
	 * @throws IOException
	 * @throws FtpProtocolException
	 */
	public void cd(String path) throws IOException, FtpProtocolException {
		ftp.changeDirectory(path);
	}

	/**
	 * 得到当前目录
	 * @return
	 * @throws IOException
	 * @throws FtpProtocolException
	 */
	public String pwd() throws IOException, FtpProtocolException {
		return ftp.getWorkingDirectory();
	}

	/**
	 * 创建文件夹
	 * @param path
	 * @throws IOException
	 * @throws FtpProtocolException
	 */
	public void mkdir(String path) throws IOException, FtpProtocolException {
		ftp.makeDirectory(path);
	}

	/**
	 * 删除非空文件夹
	 * @param path
	 * @throws IOException
	 * @throws FtpProtocolException
	 */
	public void rmdir(String path) throws IOException, FtpProtocolException {
		ftp.removeDirectory(path);
	}

	/**
	 * 上传文件
	 * @param path
	 * @param input
	 * @throws IOException
	 * @throws FtpProtocolException
	 */
	public void upload(String path, InputStream input) throws IOException, FtpProtocolException {
		ftp.putFile(path, input);
	}

	/**
	 * 上传文件
	 * @param path
	 * @return
	 * @throws IOException
	 * @throws FtpProtocolException
	 */
	public OutputStream upload(String path) throws IOException, FtpProtocolException {
		return ftp.putFileStream(path);
	}

	/**
	 * 下载文件
	 * @param path
	 * @param out
	 * @throws IOException
	 * @throws FtpProtocolException
	 */
	public void download(String path, OutputStream out) throws IOException, FtpProtocolException {
		ftp.getFile(path, out);
	}

	/**
	 * 下载文件
	 * @param path
	 * @return
	 * @throws IOException
	 * @throws FtpProtocolException
	 */
	public InputStream download(String path) throws IOException, FtpProtocolException {
		return ftp.getFileStream(path);
	}

	/**
	 * 重命名或移动文件
	 * @param pathFrom
	 * @param pathTo
	 * @throws IOException
	 * @throws FtpProtocolException
	 */
	public void mv(String pathFrom, String pathTo) throws IOException, FtpProtocolException {
		ftp.rename(pathFrom, pathTo);
	}

	/**
	 * 删除文件
	 * @param path
	 * @throws IOException
	 * @throws FtpProtocolException
	 */
	public void rm(String path) throws IOException, FtpProtocolException {
		ftp.deleteFile(path);
	}

	/**
	 * 获得文件大小
	 * @param path
	 * @return
	 * @throws IOException
	 * @throws FtpProtocolException
	 */
	public long size(String path) throws IOException, FtpProtocolException {
		return ftp.getSize(path);
	}
	
}
