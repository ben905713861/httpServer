package com.wuxb.httpServer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.zip.GZIPOutputStream;

import com.wuxb.httpServer.exception.HttpErrorException;
import com.wuxb.httpServer.exception.HttpInterceptInterrupt;
import com.wuxb.httpServer.util.Config;
import com.wuxb.httpServer.util.Encrypt;

public class StaticSource {

	private static final String PRXFIX_PATH;//静态资源指定路由前缀,相对/resources/的存放路径
	protected static final int CACHE_TIME;//静态资源在浏览器的缓存时间
	protected static final int MAX_CACHE_LEN = 100;//最大缓存文件数量
	protected static final long MAX_CACHE_FILE_SIZE = 1000000;//单文件允许加入缓存的最大字节数 1MB
	protected String path;
	protected RequestHeader requestHeader;
	protected HttpServletResponse httpServletResponse;
	protected ResponseHeader responseHeader;
	protected ResponseBody responseBody;
	protected static Map<String, SourceInfo> routeMap = new LinkedHashMap<String, StaticSource.SourceInfo>();
	protected SourceInfo sourceInfo;
	
	static {
		String path = Config.get("http.staticSource.path");
		if(path == null || path.isEmpty()) {
			PRXFIX_PATH = "static";
		} else {
			PRXFIX_PATH = path;
		}
		String cacheTime = Config.get("http.staticSource.cacheTime");
		if(cacheTime == null || cacheTime.isEmpty()) {
			CACHE_TIME = 172800;
		} else {
			CACHE_TIME = Integer.parseInt(cacheTime);
		}
	}
	
	protected class SourceInfo {
		public String etag;
		public boolean useGzip;
		public byte[] bodyByte;
	}
	
	public StaticSource(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) {
		path = httpServletRequest.getPath();
		requestHeader = httpServletRequest.getHeader();
		responseHeader = httpServletResponse.getHeader();
		responseBody = httpServletResponse.getBody();
		this.httpServletResponse = httpServletResponse;
	}
	
	public void action() throws IOException, HttpInterceptInterrupt, HttpErrorException {
		if(path.indexOf(PRXFIX_PATH) != 0) {
			return;
		}
		//扩展名
		String extName = path.substring(path.lastIndexOf(".") + 1);
		//body
		setRespBody(extName);
		//ContentType
		setContentType(extName);
		//浏览器缓存
		responseHeader.set("Cache-Control", "max-age="+ CACHE_TIME);
		httpServletResponse.setResponseCode(200);
		throw new HttpInterceptInterrupt();
	}
	
	protected void setContentType(String extName) {
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
	
	protected InputStream getInputStream() throws IOException {
		return ClassLoader.getSystemResourceAsStream(path.substring(1));
	}
	
	protected void setRespBody(String extName) throws IOException, HttpErrorException, HttpInterceptInterrupt {
		//检测到服务器有缓存
		if(routeMap.containsKey(path)) {
			useCache();
			return;
		}
		InputStream is = getInputStream();
		if(is == null) {
			httpServletResponse.setResponseCode(404);
			throw new HttpErrorException(path +"资源不存在");
		}
		//特大文件不做缓存，且使用分段输出
		long contentLength = is.available();
		if(contentLength > MAX_CACHE_FILE_SIZE) {
			responseHeader.setContentLength(contentLength);
			responseBody.setInputStream(is);
			return;
		}
		sourceInfo = new SourceInfo();
		byte[] bodyByte = is.readAllBytes();
		is.close();
		switch(extName) {
			case "html":
			case "htm":
			case "css":
			case "js":
			case "txt":
			case "json":
			case "xml":
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
		//更新并维持链表长度
		synchronized(StaticSource.class) {
			routeMap.put(path, sourceInfo);
			Set<String> keySet = routeMap.keySet();
			if(keySet.size() > MAX_CACHE_LEN) {
				String firstKey = keySet.iterator().next();
				routeMap.remove(firstKey);
			}
		}
	}
	
	protected void useCache() throws HttpInterceptInterrupt {
		sourceInfo = routeMap.get(path);
		//使用304响应
		String cacheControl = (String) requestHeader.get("Cache-Control");
		if(cacheControl == null || !cacheControl.equals("no-cache")) {
			String requestEtag = (String) requestHeader.get("If-None-Match");
			if(requestEtag != null && !requestEtag.isEmpty()) {
				if(requestEtag.equals(sourceInfo.etag)) {
					responseHeader.set("ETag", requestEtag);
					httpServletResponse.setResponseCode(304);
					throw new HttpInterceptInterrupt();
				}
			}
		}
		//使用缓存响应
		if(sourceInfo.useGzip) {
			responseHeader.set("Content-Encoding", "gzip");
			responseHeader.set("Vary", "Accept-Encoding");
		}
		responseHeader.setContentLength(sourceInfo.bodyByte.length);
		responseHeader.set("ETag", sourceInfo.etag);
		responseBody.setBodyByte(sourceInfo.bodyByte);
	}
	
	protected void gzipOutput(byte[] bodyByte) throws IOException {
		responseHeader.set("Content-Encoding", "gzip");
		responseHeader.set("Vary", "Accept-Encoding");
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		GZIPOutputStream gzipOutputStream = new GZIPOutputStream(baos);
		gzipOutputStream.write(bodyByte);
		gzipOutputStream.finish();
		byte[] respByte = baos.toByteArray();//压缩后的字节流
		gzipOutputStream.close();
		responseHeader.setContentLength(respByte.length);
		responseBody.setBodyByte(respByte);
		sourceInfo.useGzip = true;
		sourceInfo.bodyByte = respByte;
	}
	
	protected void defaultOutput(byte[] bodyByte) {
		responseHeader.setContentLength(bodyByte.length);
		responseBody.setBodyByte(bodyByte);
		sourceInfo.useGzip = false;
		sourceInfo.bodyByte = bodyByte;
	}
	
}
