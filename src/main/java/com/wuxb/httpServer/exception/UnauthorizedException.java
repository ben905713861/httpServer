package com.wuxb.httpServer.exception;

public class UnauthorizedException extends Exception {
	
	private static final long serialVersionUID = -448185028192954772L;

	public UnauthorizedException(String message) {
		super(message);
	}
	
}
