package com.wuxb.httpServer.exception;

public class ForbiddenException extends Exception {
	
	private static final long serialVersionUID = -448185028192954772L;

	public ForbiddenException(String message) {
		super(message);
	}
	
}
