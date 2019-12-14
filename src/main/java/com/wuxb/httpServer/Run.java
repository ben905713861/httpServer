package com.wuxb.httpServer;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.KeyStore;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocket;

import com.wuxb.httpServer.util.Config;

public class Run {

	private static final int PORT = Integer.parseInt(Config.get("server.port"));
	private static final boolean USE_HTTPS;
	private static final String SSL_KEY_PATH = Config.get("server.sslKeyPath");
	private static final String SSL_KEY_PASSWORD = Config.get("server.sslKeyPassword");
	
	static {
		String useHttps = Config.get("server.useHttps");
		if(useHttps == null || useHttps.isEmpty()) {
			USE_HTTPS = false;
		} else {
			USE_HTTPS = Boolean.parseBoolean(useHttps);
		}
	}
	
	public static void main(String[] args) {
		System.out.println("======= httpServer2.0 start on port "+ PORT +" =======");
		try {
			Class.forName(Route.class.getName());
			System.out.println(Route.getRouteMap().keySet());
			
			ServerSocket server;
			if(USE_HTTPS) {
				server = https();
			} else {
				server = http();
			}
			while(true) {
				Socket client = server.accept();//阻塞
				new Thread(new HttpKeepAlive(client)).start();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private static ServerSocket http() throws IOException {
		ServerSocket server = new ServerSocket(PORT);
		return server;
	}
	
	private static ServerSocket https() throws Exception {
		System.out.println("===>>> SERVER USE HTTPS");
		char keyStorePass[] = SSL_KEY_PASSWORD.toCharArray(); // 证书密码
		KeyStore ks = KeyStore.getInstance("JKS"); // 创建JKS密钥库
		ks.load(ClassLoader.getSystemResourceAsStream(SSL_KEY_PATH), keyStorePass);
		// 创建管理JKS密钥库的X.509密钥管理器
		KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
		kmf.init(ks, keyStorePass);
		SSLContext sslContext = SSLContext.getInstance("TLSv1.2");
		sslContext.init(kmf.getKeyManagers(), null, null);
		SSLServerSocket server = (SSLServerSocket) sslContext.getServerSocketFactory().createServerSocket(PORT);
		return server;
	}

}
