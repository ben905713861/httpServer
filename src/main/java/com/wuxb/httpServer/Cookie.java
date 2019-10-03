package com.wuxb.httpServer;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;


public class Cookie {
	
	private Map<String, String> cookiesMap = new HashMap<String, String>();
	private List<String> setCookiesList = new LinkedList<String>();
	
	public class Attributes {
		public int maxAge = 0;
		public String path = "/";
		public String domain = "";
		public boolean secure = false;
		public boolean httpOnly = false;
		public String sameSite = null;
	}
	
	public Cookie(String cookiesStr) {
		if(cookiesStr == null) {
			return;
		}
		//处理
		String[] keyValueArray = cookiesStr.split(";");
		for(String keyValue : keyValueArray) {
			String[] temp = keyValue.split("=");
			if(temp.length < 2) {
				continue;
			}
			cookiesMap.put(temp[0].trim(), temp[1].trim());
		}
	}
	
	public String get(String cookieName) {
		return cookiesMap.get(cookieName);
	}
	
	public Attributes getAttributesObj() {
		return new Attributes();
	}
	
	public void set(String cookieName, Object value) {
		set(cookieName, value, null);
	}
	
	public void set(String cookieName, Object value, Attributes attributes) {
		String cookiesStr = "";
		cookiesStr += cookieName + "=" + value.toString() + ";";
		if(attributes == null) {
			attributes = getAttributesObj();
		}
		if(attributes.maxAge > 0) {
			cookiesStr += "Max-Age="+ attributes.maxAge  + ";";
		}
		if(!attributes.path.isEmpty()) {
			cookiesStr += "path="+ attributes.path + ";";
		}
		if(!attributes.domain.isEmpty()) {
			cookiesStr += "domain="+ attributes.domain + ";";
		}
		if(attributes.secure) {
			cookiesStr += "secure;";
		}
		if(attributes.httpOnly) {
			cookiesStr += "httpOnly;";
		}
		if(attributes.sameSite != null) {
			cookiesStr += "SameSite="+ attributes.sameSite +";";
		}
		cookiesStr = cookiesStr.substring(0, cookiesStr.length()-1);
		setCookiesList.add(cookiesStr);
	}
	
	public List<String> getRespCookies() {
		return setCookiesList;
	}
	
}
