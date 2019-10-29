package com.wuxb.httpServer;

import java.util.Map;

import com.wuxb.httpServer.util.HttpUrlParams;

public class HttpServletRequest {

	private static final int PROXY_LEN = 1;//服务端已知反向代理层数
	private String httpVersion;
	private String requestMethod;
	private String url;
	private String path;
	private String queryString;
	private Map<String, Object> queryParams;
	private RequestHeader requestHeader;
	private Cookie cookie;
	private Session session;
	private RequestBody requestBody;
	private String ip;
	
	public HttpServletRequest(String baseInfoStr, RequestHeader requestHeader, RequestBody requestBody, Cookie cookie, Session session, String ip) {
		//基本信息
		String[] baseInfo = baseInfoStr.split(" ");
		requestMethod = baseInfo[0].toUpperCase();
		url = baseInfo[1];
		if(url.indexOf("?") == -1) {
			path = url;
		} else {
			String[] urlTemp = url.split("\\?");
			path = urlTemp[0];
			if(urlTemp.length  > 1) {
				queryString = urlTemp[1];
			} else {
				queryString = "";
			}
			queryParams = HttpUrlParams.urldecode(queryString);
		}
		httpVersion = baseInfo[2];
		this.requestHeader = requestHeader;
		this.cookie = cookie;
		this.session = session;
		this.requestBody = requestBody;
		//客户端ip
		String xForwardFor = (String) requestHeader.get("X-Forwarded-For");
		if(xForwardFor == null || xForwardFor.isEmpty()) {
			this.ip = ip;
		} else {
			String[] ips = xForwardFor.split(",");
			this.ip = ips[ips.length - PROXY_LEN].trim();
		}
	}
	
	public String getRequestMethod() {
		return requestMethod;
	}

	public String getUrl() {
		return url;
	}

	public Map<String, Object> getQueryParams() {
		return queryParams;
	}

	public RequestHeader getHeader() {
		return requestHeader;
	}
	
	public RequestBody getBody() {
		return requestBody;
	}

	public String getPath() {
		return path;
	}

	public String getQueryString() {
		return queryString;
	}

	public Cookie getCookie() {
		return cookie;
	}

	public Session getSession() {
		return session;
	}

	public String getIp() {
		return ip;
	}

	public String getHttpVersion() {
		return httpVersion;
	}

}

