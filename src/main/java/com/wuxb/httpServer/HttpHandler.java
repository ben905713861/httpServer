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
import com.wuxb.httpServer.params.RequestMethod;
import com.wuxb.httpServer.params.RouteParams;
import com.wuxb.httpServer.util.Config;
import com.wuxb.httpServer.util.HttpContextHolder;

public class HttpHandler implements Runnable {
	
	private static final int HTTP_TIMEOUT;
	private static final int MAX_BODY_SIZE;
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
		String timeout = Config.get("http.timeout");
		if(timeout == null || timeout.isEmpty()) {
			HTTP_TIMEOUT = 5000;
		} else {
			HTTP_TIMEOUT = Integer.parseInt(timeout);
		}
		String size = Config.get("http.maxBodySize");
		if(size == null || size.isEmpty()) {
			MAX_BODY_SIZE = 10485760;
		} else {
			MAX_BODY_SIZE = Integer.parseInt(size);
		}
		String useSession = Config.get("http.session.useSession");
		if(useSession == null || useSession.isEmpty() || useSession.equals("true")) {
			USE_SESSION = true;
		} else {
			USE_SESSION = false;
		}
	}
	
	public HttpHandler(Socket client) {
		try {
			bis = new BufferedInputStream(client.getInputStream());
			bos = new BufferedOutputStream(client.getOutputStream());
//			client.setKeepAlive(true);
			client.setSoTimeout(HTTP_TIMEOUT);
		} catch (IOException e) {
			e.printStackTrace();
		}
		this.client = client;
	}
	
	@Override
	public void run() {
		try {
			initServlet();
			//记录到static方法内，方便当前线程从其他地方获取
			HttpContextHolder.set(httpServletRequest, httpServletResponse);
			//拦截器
			InterceptLoader.run(httpServletRequest, httpServletResponse);
			//获取控制器的返回数据
			getRespData();
			response(null);
		} catch (HttpInterceptInterrupt e) {
			//拦截器中断
			response(e.getMessage());
//			System.out.println(e.getMessage());
		} catch (HttpErrorException e) {
			//http本框架内错误
			response(e.getMessage());
			System.err.println(e.getMessage());
		} catch (RequestFailedException e) {
			//http请求 接收数据过程致命错误
			e.printStackTrace();
		} catch(Exception e) {
			httpServletResponse.setResponseCode(500);
			response(e.getMessage());
			e.printStackTrace();
		} finally {
			close();
		}
	}
	
	private void initServlet() throws HttpErrorException, IOException, RequestFailedException  {
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
	
	private String readHeader() throws RequestFailedException {
		ByteArrayOutputStream tempOutputStream = new ByteArrayOutputStream();
		//获取header
		int[] checkTemp = new int[4];
		int byteTemp;
		while(true) {
			try {
				byteTemp = bis.read();
			} catch (SocketTimeoutException e) {
				throw new RequestFailedException("致命错误！接收请求头数据超时");
			} catch (IOException e) {
				throw new RequestFailedException("致命错误！IOException："+ e.getMessage());
			}
			checkTemp[0] = checkTemp[1];
			checkTemp[1] = checkTemp[2];
			checkTemp[2] = checkTemp[3];
			checkTemp[3] = byteTemp;
			//写入临时数组
			tempOutputStream.write(byteTemp);
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
//			//响应头
//			if(responseHeader == null) {
//				responseHeader = new ResponseHeader(cookie);
//				responseHeader.setContentType("text/plain");
//			}
//			//响应体
//			if(responseBody == null) {
//				responseBody = new ResponseBody();
//			}
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
