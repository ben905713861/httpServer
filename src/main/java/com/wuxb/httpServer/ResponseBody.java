package com.wuxb.httpServer;

import java.io.InputStream;

public class ResponseBody {
	
	private byte[] bodyByte;
	private InputStream inputStream;
	
	public void setBodyText(String bodyText) {
		bodyByte = bodyText.getBytes();
	}
	
	public byte[] getBodyByte() {
		return bodyByte;
	}
	
	public void setBodyByte(byte[] bodyByte) {
		this.bodyByte = bodyByte;
	}

	public InputStream getInputStream() {
		return inputStream;
	}

	public void setInputStream(InputStream inputStream) {
		this.inputStream = inputStream;
	}
	
}
