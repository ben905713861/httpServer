package com.wuxb.httpServer.exception;

public class ReqTooLargeException extends Exception {
	
	private static final long serialVersionUID = 1L;

	public ReqTooLargeException(String message) {
		super(message);
	}
	
}
