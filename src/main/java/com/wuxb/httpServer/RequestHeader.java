package com.wuxb.httpServer;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

public class RequestHeader {

	private String cookie;
	private String contenttype;
	private long contentlength = 0;
	private String host;
	private Map<String, Object> otherHeaderMap = new HashMap<String, Object>();
	
	public RequestHeader(String headerStr) {
		headerStr = headerStr.trim();
		String[] keyValueArray = headerStr.split("\r\n");
		for(String keyValue : keyValueArray) {
			String[] temp = keyValue.split(":");
			String key = temp[0].trim();
			String _key = key.replace("-", "").toLowerCase();
			String value = temp[1].trim();
			if(key.equals("otherHeaderMap") || _key.equals("otherHdeaderMap")) {
				continue;
			}
			//是整数数字
			boolean is_number = value.matches("^0|(\\-?[1-9]{1}[0-9]*)$");
			//赋值
			try {
				Field field = this.getClass().getDeclaredField(_key);
				field.setAccessible(true);
				field.set(this, is_number ? Long.parseLong(value) : value);
			} catch(NoSuchFieldException e) {
				otherHeaderMap.put(key, is_number ? Long.parseLong(value) : value);
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			}
		}
	}
	
	public String getCookieStr() {
		return cookie;
	}

	public String getContentType() {
		return contenttype;
	}

	public long getContentLength() {
		return contentlength;
	}

	public String getHost() {
		return host;
	}
	
	public Object get(String key) {
		return otherHeaderMap.get(key);
	}
	
}

