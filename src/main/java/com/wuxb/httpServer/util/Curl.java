package com.wuxb.httpServer.util;

import java.net.Socket;
import java.util.LinkedHashMap;
import java.util.Map;

public class Curl {

	private String method = "GET";
	private String url;
	private String path;
	private String queryString;
	private String host;
	private int port;
	private String contentType;
	private Map<String, String> headers = new LinkedHashMap<String, String>();
	private Map<String, Object> data;
	private String dataStr;
	
	public void setMethod(String method) {
		this.method = method;
	}
	
	public void setUrl(String url) {
		this.url = url;
		
	}
	
	public void setContentType(String contentType) {
		this.contentType = contentType;
	}
	
	public void setHeader(String key, String value) {
		headers.put(key, value);
	}
	
	public void setData(Map<String, Object> data) {
		this.data = data;
	}
	
	public void setDataStr(String dataStr) {
		this.dataStr = dataStr;
	}
	
	public void send() {
		Socket socket = new Socket();
		
	}
	
}
