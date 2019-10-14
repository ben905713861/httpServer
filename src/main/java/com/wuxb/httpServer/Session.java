package com.wuxb.httpServer;

import java.util.Date;

import com.alibaba.fastjson.JSONObject;
import com.wuxb.httpServer.Cookie.Attributes;
import com.wuxb.httpServer.util.Config;
import com.wuxb.httpServer.util.Encrypt;

public class Session {
	
	private static final String COOKIE_NAME;
	private static final int SESSION_AGE;
	private static final String SERVER_BEFORE_KEY_NAME;
	private String sessionId;
	private JSONObject sessionJsonObject;
	
	static {
		String cookieName = Config.get("http.session.cookieName");
		if(cookieName == null || cookieName.isEmpty()) {
			COOKIE_NAME = "SESSIONID";
		} else {
			COOKIE_NAME = cookieName;
		}
		String sessionAge = Config.get("http.session.sessionAge");
		if(sessionAge == null || sessionAge.isEmpty()) {
			SESSION_AGE = 1440;
		} else {
			SESSION_AGE = Integer.parseInt(sessionAge);
		}
		String serverBeforeKeyName = Config.get("http.session.serverBeforeKeyName");
		if(serverBeforeKeyName == null || serverBeforeKeyName.isEmpty()) {
			SERVER_BEFORE_KEY_NAME = "SESSIONID";
		} else {
			SERVER_BEFORE_KEY_NAME = serverBeforeKeyName;
		}
	}
	
	public Session(Cookie cookie) {
		sessionId = cookie.get(COOKIE_NAME);
		if(sessionId == null || sessionId.isEmpty()) {
			sessionId = createSessionId();
			Attributes attributes = cookie.getAttributesObj();
			attributes.httpOnly = true;
			attributes.sameSite = "Strict";
			cookie.set(COOKIE_NAME, sessionId, attributes);
		}
		//获取数据并反序列化
		String sessionStr = Cache.get(SERVER_BEFORE_KEY_NAME + sessionId);
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
		Cache.set(SERVER_BEFORE_KEY_NAME+sessionId, sessionJsonObject.toString(), SESSION_AGE);
	}
	
	public void destory() {
		sessionJsonObject.clear();
		Cache.delete(SERVER_BEFORE_KEY_NAME+sessionId);
	}
	
}
