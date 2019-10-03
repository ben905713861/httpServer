package com.wuxb.httpServer;

import java.net.ServerSocket;
import java.net.Socket;

import com.wuxb.httpServer.util.Config;

public class Run {

	private static final int port = Integer.parseInt(Config.get("server.port"));
	
	public static void main(String[] args) {
		System.out.println("======= httpServer2.0 start =======");
		try {
			Class.forName(Route.class.getName());
System.out.println(Route.getRouteMap().keySet());
			@SuppressWarnings("resource")
			ServerSocket server = new ServerSocket(port);
			while(true) {
				Socket client = server.accept();//阻塞
				new Thread(new HttpHandler(client)).start();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
