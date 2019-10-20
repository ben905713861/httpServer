package com.wuxb.httpServer;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.Map;

import com.wuxb.httpServer.annotation.GetParam;
import com.wuxb.httpServer.annotation.PostParam;
import com.wuxb.httpServer.exception.HttpErrorException;
import com.wuxb.httpServer.exception.HttpInterceptInterrupt;
import com.wuxb.httpServer.exception.RequestFailedException;
import com.wuxb.httpServer.exception.TCPClientClose;
import com.wuxb.httpServer.params.RequestMethod;
import com.wuxb.httpServer.params.RouteParams;
import com.wuxb.httpServer.util.Config;
import com.wuxb.httpServer.util.HttpContextHolder;

public class HttpHandler {
	
	private static final int MAX_HEADER_SIZE;
	private static final long MAX_BODY_SIZE;
	private static final boolean USE_SESSION;
	private Socket client;
	private BufferedInputStream bis;
	private HttpServletRequest httpServletRequest;
	private RequestHeader requestHeader;
	private Cookie cookie;
	private Session session;
	private RequestBody requestBody;
	private BufferedOutputStream bos;
	private HttpServletResponse httpServletResponse;
	private ResponseHeader responseHeader;
	private ResponseBody responseBody;
	
	static {
		String maxHeaderSize = Config.get("http.maxHeaderSize");
		if(maxHeaderSize == null || maxHeaderSize.isEmpty()) {
			MAX_HEADER_SIZE = 10240;
		} else {
			MAX_HEADER_SIZE = Integer.parseInt(maxHeaderSize);
		}
		String maxBodysize = Config.get("http.maxBodySize");
		if(maxBodysize == null || maxBodysize.isEmpty()) {
			MAX_BODY_SIZE = 10485760;
		} else {
			MAX_BODY_SIZE = Long.parseLong(maxBodysize);
		}
		String useSession = Config.get("http.session.useSession");
		if(useSession == null || useSession.isEmpty() || useSession.equals("true")) {
			USE_SESSION = true;
		} else {
			USE_SESSION = false;
		}
	}
	
	public HttpHandler(Socket client, BufferedInputStream bis, BufferedOutputStream bos) {
		this.bis = bis;
		this.bos = bos;
		this.client = client;
	}
	
	public boolean action() {
		try {
			initServlet();
			//记录到static方法内，方便当前线程从其他地方获取
			HttpContextHolder.set(httpServletRequest, httpServletResponse);
			//拦截器
			try {
				InterceptLoader.run(httpServletRequest, httpServletResponse);
				//获取控制器的返回数据
				getRespData();
				response(null);
			} catch (HttpInterceptInterrupt e) {
				//拦截器中断，直接执行响应
				response(e.getMessage());
			}
			//判断是否为持久连接
			checkKeepAlive();
		} catch (HttpErrorException e) {
			//http本框架内错误
			response(e.getMessage());
			System.err.println(e.getMessage());
		} catch (RequestFailedException e) {
			//http请求 接收数据过程致命错误
			e.printStackTrace();
			close();
			return false;
		} catch (TCPClientClose e) {
			//达到维持tcp连接的最大等待时长,则关闭tcp连接
			close();
			return false;
		} catch(Exception e) {
			httpServletResponse.setResponseCode(500);
			response(e.getMessage());
			e.printStackTrace();
		}
		return true;
	}
	
	private void initServlet() throws HttpErrorException, IOException, RequestFailedException, TCPClientClose  {
		//请求头源码
		String headerStr = readHeader();
		int rowOneIndex = headerStr.indexOf("\r\n");//请求头第一行位置
		String baseInfoStr = headerStr.substring(0, rowOneIndex);
		//请求头
		requestHeader = new RequestHeader(headerStr.substring(rowOneIndex));
		//请求头中的cookie
		cookie = new Cookie(requestHeader.getCookieStr());
		//session
		if(USE_SESSION) {
			session = new Session(cookie);
		}
		//响应头
		responseHeader = new ResponseHeader(cookie);
		Object connection = requestHeader.get("Connection");
		if(connection != null) {
			responseHeader.set("Connection", connection);
		}
		//响应体
		responseBody = new ResponseBody();
		//servletResp响应对象
		httpServletResponse = new HttpServletResponse(responseHeader, responseBody);
		//请求体
		long bodySize = requestHeader.getContentLength();
		if(bodySize > MAX_BODY_SIZE) {
			httpServletResponse.setResponseCode(413);
			throw new HttpErrorException("请求内容过大");
		}
		byte[] bodyBytes = readBody(bodySize);
		requestBody = new RequestBody(requestHeader.getContentType(), bodyBytes);
		//servletReq请求对象
		httpServletRequest = new HttpServletRequest(baseInfoStr, requestHeader, requestBody, cookie, session, client.getInetAddress().getHostAddress());
	}
	
	private String readHeader() throws RequestFailedException, TCPClientClose {
		ByteArrayOutputStream tempOutputStream = new ByteArrayOutputStream();
		//获取header
		int[] checkTemp = new int[4];
		int byteTemp;
		while(true) {
			try {
				byteTemp = bis.read();
			} catch (SocketTimeoutException e) {
				System.out.println("服务器因超时关闭了套接字");
				throw new TCPClientClose();
			} catch (IOException e) {
				throw new RequestFailedException("致命错误！IOException："+ e.getMessage());
			}
			if(byteTemp == -1) {
				System.out.println("客户端关闭了套接字");
				throw new TCPClientClose();
			}
			checkTemp[0] = checkTemp[1];
			checkTemp[1] = checkTemp[2];
			checkTemp[2] = checkTemp[3];
			checkTemp[3] = byteTemp;
			//写入临时数组
			tempOutputStream.write(byteTemp);
			if(tempOutputStream.size() > MAX_HEADER_SIZE) {
				throw new RequestFailedException("致命错误！请求头信息量过大，服务器拒绝继续请求");
			}
			//判断header结束位置
			if(checkTemp[0] == 13 && checkTemp[1] == 10 && checkTemp[2] == 13 && checkTemp[3] == 10) {
				break;
			}
		}
		try {
			String headerStr = tempOutputStream.toString("UTF-8");
			tempOutputStream.reset();
			tempOutputStream.close();
			return headerStr;
		} catch (IOException e) {
			throw new RequestFailedException("致命错误！IOException："+ e.getMessage());
		}
	}
	
	private byte[] readBody(long length) throws IOException, HttpErrorException {
		if(length == 0) {
			return new byte[0];
		}
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		byte[] temp = new byte[4096];
		long lenSum = 0;
		while(true) {
			int len;
			try {
				len = bis.read(temp);
			} catch (SocketTimeoutException e) {
				httpServletResponse.setResponseCode(408);
				throw new HttpErrorException("发送请求数据超时");
			}
			if(len < 4096) {
				baos.write(temp, 0, len);
			} else {
				baos.writeBytes(temp);
			}
			lenSum += len;
			if(lenSum >= length) {
				break;
			}
		}
		return baos.toByteArray();
	}
	
	private void getRespData() throws Exception {
		// 根据路由执行
		String path =  httpServletRequest.getPath();
		RouteParams routeParams = Route.getRouteMap().get(path);
		if(routeParams == null) {
			httpServletResponse.setResponseCode(404);
			throw new HttpErrorException(path +" 路由不存在");
		}
		//请求方法检查
		if(!routeParams.getRequestMethod().equals(RequestMethod.ALL) && !routeParams.getRequestMethod().toString().equals(httpServletRequest.getRequestMethod())) {
			httpServletResponse.setResponseCode(405);
			throw new HttpErrorException("请求方法不合法");
		}
		Class<?> clazz = routeParams.getClazz();
		Method method = routeParams.getMethod();
		Parameter[] parameters = method.getParameters();
		Object[] paramsObjects = new Object[parameters.length]; //需要传入控制器方法中的参数
		for(int i = 0; i < parameters.length; i++) {
			Class<?> type = parameters[i].getType();
			if(type.equals(Map.class)) {
				if(parameters[i].isAnnotationPresent(PostParam.class)) {
					paramsObjects[i] = httpServletRequest.getBody().getBodyMap();
				} else if(parameters[i].isAnnotationPresent(GetParam.class)) {
					paramsObjects[i] = httpServletRequest.getQueryParams();
				} else {
					paramsObjects[i] = null;
				}
			}
			else if(type.equals(HttpServletRequest.class)) {
				paramsObjects[i] = httpServletRequest;
			}
			else if(type.equals(HttpServletResponse.class)) {
				paramsObjects[i] = httpServletResponse;
			} else {
				paramsObjects[i] = null;
			}
		}
		try {
			Object bodyObject = method.invoke(clazz.getDeclaredConstructor().newInstance(), paramsObjects);
			if(bodyObject != null) {
				httpServletResponse.setResponse(bodyObject);
			}
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
			throw (Exception) e.getCause();
		}
		if(httpServletResponse.getResponseCode() == 0) {
			httpServletResponse.setResponseCode(200);
		}
	}
	
	private void response(String message) {
		try {
			byte[] bodyByte;
			if(message != null && !message.isEmpty()) {
				bodyByte = message.getBytes();
				responseBody.setBodyByte(bodyByte);
				responseHeader.setContentLength(bodyByte.length);
			} else {
				bodyByte = responseBody.getBodyByte();
			}
			//基本信息
			int code = httpServletResponse.getResponseCode();
			String status = httpServletResponse.getResponseCodeMsg();
			bos.write(("HTTP/1.1 "+ code +" "+ status +"\r\n").getBytes());
			//写入响应头
			bos.write(responseHeader.toString().getBytes("UTF-8"));
			//写入分割线
			bos.write(13);
			bos.write(10);
			//写入响应体，有is流的优先用is流
			InputStream bodyInputStream = responseBody.getInputStream();
			if(bodyInputStream != null) {
				byte[] tempBytes = new byte[4096];
				int len;
				while((len = bodyInputStream.read(tempBytes)) != -1) {
					bos.write(tempBytes, 0, len);
				}
				return;
			}
			if(bodyByte != null) {
				bos.write(bodyByte);
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				bos.flush();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	//判断是否为持久连接
	private void checkKeepAlive() throws TCPClientClose  {
		String connection = (String) requestHeader.get("Connection");
		if(httpServletRequest.getHttpVersion().toUpperCase().equals("HTTP/1.0")) {
			if(connection == null || connection.toLowerCase().equals("close")) {
				throw new TCPClientClose();
			}
		} else {
			if(connection != null && connection.toLowerCase().equals("close")) {
				throw new TCPClientClose();
			}
		}
	}
	
	private void close() {
		try {
			if(bos != null) bos.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		try {
			if(bis != null) bis.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		try {
			client.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
