package com.wuxb.httpServer.exception;

public class RequestFailedException extends Exception {
	
	private static final long serialVersionUID = -448185028192954771L;
	
	public RequestFailedException(String message) {
		super(message);
	}
	
}
