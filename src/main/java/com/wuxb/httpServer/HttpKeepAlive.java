package com.wuxb.httpServer;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.Socket;

import com.wuxb.httpServer.util.Config;

public class HttpKeepAlive implements Runnable {
	
	private static final int HTTP_TIMEOUT;
	private Socket client;
	private BufferedInputStream bis;
	private BufferedOutputStream bos;
	
	static {
		String timeout = Config.get("http.timeout");
		if(timeout == null || timeout.isEmpty()) {
			HTTP_TIMEOUT = 5000;
		} else {
			HTTP_TIMEOUT = Integer.parseInt(timeout);
		}
	}

	public HttpKeepAlive(Socket client) {
		try {
			bis = new BufferedInputStream(client.getInputStream());
			bos = new BufferedOutputStream(client.getOutputStream());
			client.setSoTimeout(HTTP_TIMEOUT);
		} catch (IOException e) {
			e.printStackTrace();
		}
		this.client = client;
	}
	
	@Override
	public void run() {
		while(true) {
			if(!new HttpHandler(client, bis, bos).action()) {
				break;
			}
		}
	}

}
