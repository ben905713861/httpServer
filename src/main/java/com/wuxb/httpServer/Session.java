package com.wuxb.httpServer;

import java.util.Date;

import com.alibaba.fastjson.JSONObject;
import com.wuxb.httpServer.Cookie.Attributes;
import com.wuxb.httpServer.util.Config;
import com.wuxb.httpServer.util.Encrypt;

public class Session {
	
	private static final String cookieName = Config.get("http.session.cookieName");
	private static final int sessionAge = Integer.parseInt(Config.get("http.session.sessionAge"));
	private static final String serverBeforeKeyName = Config.get("http.session.serverBeforeKeyName");
	
	private String sessionId;
	private JSONObject sessionJsonObject;
	
	public Session(Cookie cookie) {
		sessionId = cookie.get(cookieName);
		if(sessionId == null || sessionId.isEmpty()) {
			sessionId = createSessionId();
			Attributes attributes = cookie.getAttributesObj();
			attributes.httpOnly = true;
			attributes.sameSite = "Strict";
			cookie.set(cookieName, sessionId, attributes);
		}
		//获取数据并反序列化
		String sessionStr = Cache.get(serverBeforeKeyName + sessionId);
		if(sessionStr == null || sessionStr.isEmpty()) {
			sessionJsonObject = new JSONObject();
		} else {
			sessionJsonObject = JSONObject.parseObject(sessionStr);
		}
	}
	
	private static String createSessionId() {
		return Encrypt.md5(Math.random() + " " + (new Date().getTime()));
	}
	
	public Object get(String sessionKey) {
		return sessionJsonObject.get(sessionKey);
	}
	
	public void set(String sessionKey, Object value) {
		sessionJsonObject.put(sessionKey, value);
		Cache.set(serverBeforeKeyName+sessionId, sessionJsonObject.toString(), sessionAge);
	}
	
	public void destory() {
		sessionJsonObject.clear();
		Cache.delete(serverBeforeKeyName+sessionId);
	}
	
}
