package com.wuxb.httpServer;

import java.util.List;
import java.util.Map;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.wuxb.httpServer.exception.RespTypeNotAllowedException;

public class HttpServletResponse {
	
	private ResponseHeader responseHeader;
	private ResponseBody responseBody;
	
	public HttpServletResponse(ResponseHeader responseHeader, ResponseBody responseBody) {
		this.responseHeader = responseHeader;
		this.responseBody = responseBody;
	}
	
	public void setResponse(Object bodyObject) throws RespTypeNotAllowedException {
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
				throw new RespTypeNotAllowedException("响应格式不支持");
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
	
}
