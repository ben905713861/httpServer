package com.wuxb.httpServer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.GZIPOutputStream;

import com.wuxb.httpServer.exception.HttpInterceptInterrupt;
import com.wuxb.httpServer.exception.HttpNotModified;
import com.wuxb.httpServer.exception.NotFoundException;
import com.wuxb.httpServer.util.Encrypt;

public class StaticSource {

	private static final String PRXFIX_ROUTE = "/static";//静态资源指定路由前缀
	private String path;
	private RequestHeader requestHeader;
	private ResponseHeader responseHeader;
	private ResponseBody responseBody;
	private static Map<String, SourceInfo> routeMap = new HashMap<String, StaticSource.SourceInfo>();
	private SourceInfo sourceInfo;
	
	private class SourceInfo {
		public String etag;
		public boolean useGzip;
		public byte[] bodyByte;
	}
	
	public StaticSource(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) {
		path = httpServletRequest.getPath();
		requestHeader = httpServletRequest.getHeader();
		responseHeader = httpServletResponse.getHeader();
		responseBody = httpServletResponse.getBody();
	}
	
	public void action() throws NotFoundException, IOException, HttpNotModified, HttpInterceptInterrupt {
		if(path.indexOf(PRXFIX_ROUTE) != 0) {
			return;
		}
		//扩展名
		String extName = path.substring(path.lastIndexOf(".") + 1);
		//ContentType
		setContentType(extName);
		//body
		setRespBody(extName);
		//浏览器缓存
		responseHeader.set("Cache-Control", "max-age=10");
		throw new HttpInterceptInterrupt();
	}
	
	private void setContentType(String extName) {
		//contentType判断
		String contentType;
		switch(extName) {
			case "html":
			case "htm":
				contentType = "text/html; charset=UTF-8";
				break;
			case "css":
				contentType = "text/css; charset=UTF-8";
				break;
			case "js":
				contentType = "application/x-javascript; charset=UTF-8";
				break;
			case "jpg":
			case "jpeg":
			case "png":
			case "gif":
				contentType = "image/"+ extName;
				break;
			case "pdf":
				contentType = "application/pdf";
				break;
			case "txt":
				contentType = "text/plain; charset=UTF-8";
				break;
			case "json":
				contentType = "application/json; charset=UTF-8";
				break;
			default:
				contentType = "application/octet-stream";
		}
		responseHeader.setContentType(contentType);
	}
	
	private void setRespBody(String extName) throws NotFoundException, IOException, HttpNotModified {
		if(routeMap.containsKey(path)) {
			useCache();
			return;
		}
		InputStream is = ClassLoader.getSystemResourceAsStream(path.substring(1));
		if(is == null) {
			throw new NotFoundException(path +"，资源不存在");
		}
		sourceInfo = new SourceInfo();
		byte[] bodyByte = is.readAllBytes();
		switch(extName) {
			case "html":
			case "htm":
			case "css":
			case "js":
			case "txt":
			case "json":
			case "pdf":
			case "doc":
			case "docx":
			case "xls":
			case "xlsx":
				if(bodyByte.length > 10240) {
					gzipOutput(bodyByte);
				} else {
					defaultOutput(bodyByte);
				}
				break;
			default:
				defaultOutput(bodyByte);
		}
		//etag文件指纹返回
		sourceInfo.etag = Encrypt.md5(bodyByte);
		responseHeader.set("ETag", sourceInfo.etag);
		routeMap.put(path, sourceInfo);
	}
	
	private void useCache() throws HttpNotModified {
		sourceInfo = routeMap.get(path);
		//使用304响应
		String cacheControl = (String) requestHeader.get("Cache-Control");
		if(cacheControl == null || !cacheControl.equals("no-cache")) {
			String requestEtag = (String) requestHeader.get("If-None-Match");
			if(requestEtag != null && !requestEtag.isEmpty()) {
				if(requestEtag.equals(sourceInfo.etag)) {
					responseHeader.set("ETag", requestEtag);
					throw new HttpNotModified();
				}
			}
		}
		//使用缓存响应
		if(sourceInfo.useGzip) {
			responseHeader.set("Content-Encoding", "gzip");
			responseHeader.set("Vary", "Accept-Encoding");
		} else {
			responseHeader.setContentLength(sourceInfo.bodyByte.length);
		}
		responseHeader.set("ETag", sourceInfo.etag);
		responseBody.setBodyByte(sourceInfo.bodyByte);
	}
	
	private void gzipOutput(byte[] bodyByte) throws IOException {
		responseHeader.set("Content-Encoding", "gzip");
		responseHeader.set("Vary", "Accept-Encoding");
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		GZIPOutputStream gzipOutputStream = new GZIPOutputStream(baos);
		gzipOutputStream.write(bodyByte);
		gzipOutputStream.finish();
		byte[] respByte = baos.toByteArray();
		gzipOutputStream.close();
		responseBody.setBodyByte(respByte);
		sourceInfo.useGzip = true;
		sourceInfo.bodyByte = respByte;
	}
	
	private void defaultOutput(byte[] bodyByte) {
		responseHeader.setContentLength(bodyByte.length);
		responseBody.setBodyByte(bodyByte);
		sourceInfo.useGzip = false;
		sourceInfo.bodyByte = bodyByte;
	}
	
}
