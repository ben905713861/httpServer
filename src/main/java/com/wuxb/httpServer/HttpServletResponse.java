package com.wuxb.httpServer;

import java.util.List;
import java.util.Map;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.wuxb.httpServer.exception.HttpErrorException;

public class HttpServletResponse {
	
	private ResponseHeader responseHeader;
	private ResponseBody responseBody;
	private int responseCode = 0;
	
	public HttpServletResponse(ResponseHeader responseHeader, ResponseBody responseBody) {
		this.responseHeader = responseHeader;
		this.responseBody = responseBody;
	}
	
	public void setResponse(Object bodyObject) throws Exception {
		String contentType;
		String bodyText;
		byte[] bodyByte;
		// 根据返回格式判断
		if(bodyObject instanceof byte[]) {
			// 设置响应格式
			bodyByte = (byte[]) bodyObject;
			contentType = "application/octet-stream";
		} else {
			if(bodyObject instanceof String) {
				bodyText = (String) bodyObject;
				contentType = "text/plain; charset=utf-8";
			} else if(bodyObject instanceof Number) {
				bodyText = bodyObject.toString();
				contentType = "text/plain; charset=utf-8";
			} else if(bodyObject instanceof Map) {
				bodyText = JSONObject.toJSONString(bodyObject, SerializerFeature.WriteMapNullValue);
				contentType = "application/json; charset=utf-8";
			} else if(bodyObject instanceof List) {
				@SuppressWarnings("unchecked")
				JSON json = new JSONArray((List<Object>) bodyObject);
				bodyText = json.toString();
				contentType = "application/json; charset=utf-8";
			} else {
				throw new HttpErrorException("响应格式不支持");
			}
			bodyByte = bodyText.getBytes();
			responseBody.setBodyText(bodyText);
		}
		responseBody.setBodyByte(bodyByte);
		responseHeader.setContentType(contentType);
		responseHeader.setContentLength(bodyByte.length);
	}
	
	public Cookie getCookie() {
		return responseHeader.getCookie();
	}
	
	public ResponseHeader getHeader() {
		return responseHeader;
	}
	
	public ResponseBody getBody() {
		return responseBody;
	}

	public void setResponseCode(int responseCode) {
		this.responseCode = responseCode;
	}
	
	public int getResponseCode() {
		return responseCode;
	}

	public String getResponseCodeMsg() {
		String msg;
		switch(responseCode) {
			case 200: msg = "OK"; break;
			case 302: msg = "Moved Temporarily"; break;
			case 304: msg = "Not Modified"; break;
			case 401: msg = "Unauthorized"; break;
			case 403: msg = "Forbidden"; break;
			case 404: msg = "Not Found"; break;
			case 405: msg = "Method Not Allowed"; break;
			case 408: msg = "Request Timeout"; break;
			case 413: msg = "Request Entity Too Large"; break;
			case 500: msg = "Internal Server Error"; break;
			default:
				msg = "";
				System.err.println("responseCode尚未定义");
		}
		return msg;
	}
	
	//重定向
	public void location(String url) {
		responseHeader.set("location", url);
		setResponseCode(302);
	}
	
}
