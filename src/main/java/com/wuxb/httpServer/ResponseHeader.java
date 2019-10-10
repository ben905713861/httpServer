package com.wuxb.httpServer;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class ResponseHeader {

	private String contenttype;
	private int contentlength = 0;
	private Cookie cookie;
	private Map<String, Object> otherHeaderMap = new HashMap<String, Object>();

	public ResponseHeader(Cookie cookie) {
		this.cookie = cookie;
	}
	
	public Cookie getCookie() {
		return cookie;
	}
	
	public void setContentType(String contenttype) {
		if(this.contenttype == null) {
			this.contenttype = contenttype;
		}
	}

	public void setContentLength(int contentlength) {
		this.contentlength = contentlength;
	}
	
	public void set(String key, Object value) {
		otherHeaderMap.put(key, value);
	}
	
	@Override
	public String toString() {
		String headerString = "";
		headerString += "Content-Type: "+ (contenttype==null ? "text/plain;charset=utf-8" : contenttype ) + "\r\n";
		if(contentlength > 0) {
			headerString += "Content-Length: "+ contentlength + "\r\n";
		}
		if(cookie != null) {
			List<String> setCookieList = cookie.getRespCookies();
			for(String setCookieStr : setCookieList) {
				headerString += "Set-Cookie: "+ setCookieStr + "\r\n";
			}
		}
		for(Entry<String, Object> entry : otherHeaderMap.entrySet()) {
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
