package com.wuxb.httpServer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.alibaba.fastjson.JSONObject;
import com.wuxb.httpServer.params.FileInfo;
import com.wuxb.httpServer.util.FormdataParamsDecode;
import com.wuxb.httpServer.util.HttpUrlParams;


public class RequestBody {

	private byte[] bodyBytes;
	private Map<String, Object> bodyMap;
	private List<FileInfo> fileList;
	
	public RequestBody(String contentType, byte[] bodyBytes) {
		this.bodyBytes = bodyBytes;
		if(contentType == null) {
			return;
		}
		else if(contentType.indexOf("application/json") != -1) {
			bodyMap = JSONObject.parseObject(new String(bodyBytes));
		}
		else if(contentType.indexOf("application/x-www-form-urlencoded") != -1) {
			bodyMap = HttpUrlParams.urldecode(new String(bodyBytes));
		}
		else if(contentType.indexOf("multipart/form-data") != -1) {
			bodyMap = new HashMap<String, Object>();
			fileList = new ArrayList<FileInfo>();
			FormdataParamsDecode.getInstance(bodyMap, fileList).decode(contentType, bodyBytes);
		}
		else {
			bodyMap = null;
		}
	}
	
	public byte[] getBodyBytes() {
		return bodyBytes;
	}

	public Map<String, Object> getBodyMap() {
		return bodyMap;
	}

	public List<FileInfo> getFileList() {
		return fileList;
	}

}
