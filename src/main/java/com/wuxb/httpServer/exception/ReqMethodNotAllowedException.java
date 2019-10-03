package com.wuxb.httpServer.exception;

public class ReqMethodNotAllowedException extends Exception {

	private static final long serialVersionUID = 8491093050562801728L;
	
	public ReqMethodNotAllowedException(String message) {
		super(message);
	}

}
