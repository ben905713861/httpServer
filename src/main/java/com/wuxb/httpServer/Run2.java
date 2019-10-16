package com.wuxb.httpServer;

import java.io.FileInputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.KeyStore;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;

public class Run2 {

	public static void main(String[] args) throws Exception {
		String key = "D:/phpstudy2018/nginx/sslkey-wuxb.club/2947140_wuxb.club.pfx"; // 要使用的证书名
		char keyStorePass[] = "hcHsK8rH".toCharArray(); // 证书密码
		
		KeyStore ks = KeyStore.getInstance("JKS"); // 创建JKS密钥库
		ks.load(new FileInputStream(key), keyStorePass);
		// 创建管理JKS密钥库的X.509密钥管理器
		KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
		kmf.init(ks, keyStorePass);
		SSLContext sslContext = SSLContext.getInstance("SSLv3");
		sslContext.init(kmf.getKeyManagers(), null, null);

		ServerSocket server = sslContext.getServerSocketFactory().createServerSocket(8080);
		while(true) {
			Socket client = server.accept();
			System.out.println(client.getLocalPort());
			System.out.println(client.getLocalAddress());
			System.out.println(client.getPort());
			System.out.println(client.getInetAddress());
			System.out.println(client.getLocalSocketAddress());
			System.out.println(client.getRemoteSocketAddress());
//			InetAddress
//			System.out.println(client.);
		}
		
		

	}

}
