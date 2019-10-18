package com.wuxb.httpServer;

import java.util.HashMap;
import java.util.Map;

public class RequestHeader {

	private Map<String, Object> headerMap = new HashMap<String, Object>();
	
	public RequestHeader(String headerStr) {
		headerStr = headerStr.trim();
		String[] keyValueArray = headerStr.split("\r\n");
		for(String keyValue : keyValueArray) {
			String[] temp = keyValue.split(":");
			String key = temp[0].trim().toLowerCase();
			String value = temp[1].trim();
			//是整数数字
			boolean is_number = value.matches("^0|(\\-?[1-9]{1}[0-9]*)$");
			headerMap.put(key, is_number ? Long.parseLong(value) : value);
		}
	}
	
	public String getCookieStr() {
		return (String) headerMap.get("cookie");
	}

	public String getContentType() {
		return (String) headerMap.get("content-type");
	}

	public long getContentLength() {
		if(headerMap.containsKey("content-length")) {
			return (long) headerMap.get("content-length");
		} else {
			return 0;
		}
	}

	public String getHost() {
		return (String) headerMap.get("host");
	}
	
	public Object get(String key) {
		return headerMap.get(key.toLowerCase());
	}
	
	public String getStringValue(String key) {
		return (String) headerMap.get(key);
	}
	
	public long getIntValue(String key) {
		return (long) headerMap.get(key);
	}
	
}

