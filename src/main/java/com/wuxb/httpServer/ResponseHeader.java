package com.wuxb.httpServer;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class ResponseHeader {

	private Cookie cookie;
	private Map<String, Object> headerMap = new HashMap<String, Object>();

	public ResponseHeader(Cookie cookie) {
		this.cookie = cookie;
	}
	
	public Cookie getCookie() {
		return cookie;
	}
	
	public void setContentType(String value) {
		Object contentType = headerMap.get("Content-Type");
		if(contentType == null || contentType.equals("")) {
			headerMap.put("Content-Type", value);
		}
	}
	
	public String getContentType() {
		return (String) headerMap.get("Content-Type");
	}

	public void setContentLength(long value) {
		headerMap.put("Content-Length", value);
	}
	
	public void set(String key, Object value) {
		headerMap.put(key, value);
	}
	
	@Override
	public String toString() {
//		if(!headerMap.containsKey("Content-Type")) {
//			headerMap.put("Content-Type", "text/plain; charset=utf-8");
//		}
		if(!headerMap.containsKey("Connection")) {
			headerMap.put("Connection", "keep-alive");
		}
		if(!headerMap.containsKey("Content-Length")) {
			headerMap.put("Content-Length", 0);
		}
		String headerString = "";
		if(cookie != null) {
			List<String> setCookieList = cookie.getRespCookies();
			for(String setCookieStr : setCookieList) {
				headerString += "Set-Cookie: "+ setCookieStr + "\r\n";
			}
		}
		for(Entry<String, Object> entry : headerMap.entrySet()) {
			String key = entry.getKey();
			Object value = entry.getValue();
			try {
				this.getClass().getField(key.replace("-", "").toLowerCase());
				continue;
			} catch (NoSuchFieldException e) {
				headerString += key + ": " + value.toString() + "\r\n";
			}
		}
		return headerString;
	}
	
}
