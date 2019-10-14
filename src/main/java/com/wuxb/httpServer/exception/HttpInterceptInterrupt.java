package com.wuxb.httpServer.exception;

public class HttpInterceptInterrupt extends Exception {
	
	private static final long serialVersionUID = -448185028192954771L;
	
	public HttpInterceptInterrupt() {
		
	}
	
	public HttpInterceptInterrupt(String message) {
		super(message);
	}
	
}
