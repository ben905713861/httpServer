package com.wuxb.httpServer;

public class ResponseBody {
	
	private byte[] bodyByte;
	
	public void setBodyText(String bodyText) {
		bodyByte = bodyText.getBytes();
	}
	
	public byte[] getBodyByte() {
		return bodyByte;
	}
	
	public void setBodyByte(byte[] bodyByte) {
		this.bodyByte = bodyByte;
	}

}
