package com.wuxb.httpServer;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.Socket;

public class HttpKeepAlive implements Runnable {
	
	private Socket client;
	private BufferedInputStream bis;
	private BufferedOutputStream bos;

	public HttpKeepAlive(Socket client) {
		try {
			bis = new BufferedInputStream(client.getInputStream());
			bos = new BufferedOutputStream(client.getOutputStream());
			client.setKeepAlive(true);
			client.setSoTimeout(60000);
		} catch (IOException e) {
			e.printStackTrace();
		}
		this.client = client;
	}
	
	@Override
	public void run() {
		while(true) {
			System.out.println("即将开始http请求");
			if(!new HttpHandler2(client, bis, bos).run()) {
				break;
			}
		}
	}

}
