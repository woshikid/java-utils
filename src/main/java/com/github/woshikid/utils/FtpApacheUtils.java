package com.github.woshikid.utils;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.net.ftp.FTPClient;

/**
 * apache的ftp工具
 * @author kid
 */
public class FtpApacheUtils implements Closeable {
	
	private FTPClient ftp;
	
	private FtpApacheUtils(FTPClient ftp) {
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
	 */
	public static FtpApacheUtils connect(String ip, int port) throws IOException {
		FTPClient ftp = new FTPClient();
		
		//设置超时时间
		Integer timeout = ftpTimeout.get();
		if (timeout == null) {
			timeout = 60000;
		} else {
			ftpTimeout.remove();
		}
		ftp.setConnectTimeout(timeout);
		ftp.setDataTimeout(timeout);
		
		ftp.connect(ip, port);
		return new FtpApacheUtils(ftp);
	}
	
	/**
	 * 用指定的用户名密码登录服务器
	 * @param username
	 * @param password
	 * @throws IOException
	 */
	public void login(String username, String password) throws IOException {
		ftp.login(username, password);
		ftp.setFileType(FTPClient.BINARY_FILE_TYPE);
	}
	
	/**
	 * 进入主动模式<br/>
	 * 服务器主动连接客户端
	 */
	public void active() {
		ftp.enterLocalActiveMode();
	}
	
	/**
	 * 进入被动模式<br/>
	 * 服务器等待客户端连接
	 */
	public void passive() {
		ftp.enterLocalPassiveMode();
	}
	
	/**
	 * 关闭ftp连接
	 * @throws IOException
	 */
	@Override
	public void close() throws IOException {
		ftp.disconnect();
	}
	
	/**
	 * 遍历文件名
	 * @param path
	 * @return
	 * @throws IOException
	 */
	public List<String> list(String path) throws IOException {
		List<String> list = new ArrayList<>();
		Arrays.asList(ftp.listFiles(path)).forEach(f -> list.add(f.getName()));
		return list;
	}
	
	/**
	 * 变更当前目录
	 * @param path
	 * @throws IOException
	 */
	public void cd(String path) throws IOException {
		ftp.changeWorkingDirectory(path);
	}
	
	/**
	 * 得到当前目录
	 * @return
	 * @throws IOException
	 */
	public String pwd() throws IOException {
		return ftp.printWorkingDirectory();
	}
	
	/**
	 * 创建文件夹
	 * @param path
	 * @throws IOException
	 */
	public void mkdir(String path) throws IOException {
		ftp.makeDirectory(path);
	}
	
	/**
	 * 删除非空文件夹
	 * @param path
	 * @throws IOException
	 */
	public void rmdir(String path) throws IOException {
		ftp.removeDirectory(path);
	}
	
	/**
	 * 上传文件
	 * @param path
	 * @param input
	 * @throws IOException
	 */
	public void upload(String path, InputStream input) throws IOException {
		ftp.storeFile(path, input);
	}
	
	/**
	 * 上传文件
	 * @param path
	 * @return
	 * @throws IOException
	 */
	public OutputStream upload(String path) throws IOException {
		return ftp.storeFileStream(path);
	}
	
	/**
	 * 下载文件
	 * @param path
	 * @param out
	 * @throws IOException
	 */
	public void download(String path, OutputStream out) throws IOException {
		ftp.retrieveFile(path, out);
	}
	
	/**
	 * 下载文件
	 * @param path
	 * @return
	 * @throws IOException
	 */
	public InputStream download(String path) throws IOException {
		return ftp.retrieveFileStream(path);
	}
	
	/**
	 * 重命名或移动文件
	 * @param pathFrom
	 * @param pathTo
	 * @throws IOException
	 */
	public void mv(String pathFrom, String pathTo) throws IOException {
		ftp.rename(pathFrom, pathTo);
	}
	
	/**
	 * 删除文件
	 * @param path
	 * @throws IOException
	 */
	public void rm(String path) throws IOException {
		ftp.deleteFile(path);
	}
	
	/**
	 * 获得文件大小
	 * @param path
	 * @return
	 * @throws IOException
	 */
	public long size(String path) throws IOException {
		return ftp.listFiles(path)[0].getSize();
	}

}
