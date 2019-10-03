package com.wuxb.httpServer;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.net.Socket;
import java.net.SocketTimeoutException;

import com.wuxb.httpServer.exception.ForbiddenException;
import com.wuxb.httpServer.exception.NotFoundException;
import com.wuxb.httpServer.exception.ReqMethodNotAllowedException;
import com.wuxb.httpServer.exception.ReqTooLargeException;
import com.wuxb.httpServer.exception.RespTypeNotAllowedException;
import com.wuxb.httpServer.exception.UnauthorizedException;
import com.wuxb.httpServer.params.RequestMethod;
import com.wuxb.httpServer.params.RouteParams;
import com.wuxb.httpServer.util.Config;

public class HttpHandler implements Runnable {
	
	private static final int httpTimeout = Integer.parseInt(Config.get("http.timeout"));
	private static final int maxBodySize = Integer.parseInt(Config.get("http.maxBodySize"));
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
	
	public HttpHandler(Socket client) {
		try {
			bis = new BufferedInputStream(client.getInputStream());
			bos = new BufferedOutputStream(client.getOutputStream());
//			client.setKeepAlive(true);
			client.setSoTimeout(httpTimeout);
		} catch (IOException e) {
			e.printStackTrace();
		}
		this.client = client;
	}
	
	@Override
	public void run() {
		try {
			initServletRequest();
			initServletResponse();
			getRespData();
			response(200, "OK", null);
		} catch (UnauthorizedException e) {
			response(401, "Unauthorized", e.getMessage());
			e.printStackTrace();
		} catch (ForbiddenException e) {
			response(403, "Forbidden", e.getMessage());
			e.printStackTrace();
		} catch (NotFoundException e) {
			response(404, "Not Found", e.getMessage());
			e.printStackTrace();
		} catch (ReqMethodNotAllowedException e) {
			response(405, "Method Not Allowed", e.getMessage());
			e.printStackTrace();
		} catch (SocketTimeoutException e) {
			response(408, "Request Timeout", e.getMessage());
			e.printStackTrace();
		} catch (ReqTooLargeException e) {
			response(413, "Request Entity Too Large", e.getMessage());
			e.printStackTrace();
		} catch (IOException e) {
			response(500, "Internal Server Error", e.getMessage());
			e.printStackTrace();
		} catch (RespTypeNotAllowedException e) {
			response(500, "Internal Server Error", e.getMessage());
			e.printStackTrace();
		} catch(Exception e) {
			response(500, "Internal Server Error", e.getMessage());
			e.printStackTrace();
		} finally {
			try {
				close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	private void initServletRequest() throws Exception {
		//请求头源码
		String headerStr = readHeader();
		int rowOneIndex = headerStr.indexOf("\r\n");//请求头第一行位置
		String baseInfoStr = headerStr.substring(0, rowOneIndex);
		//请求头
		requestHeader = new RequestHeader(headerStr.substring(rowOneIndex));
		//请求头中的cookie
		cookie = new Cookie(requestHeader.getCookieStr());
		//session
		session = new Session(cookie);
		//请求体
		int bodySize = requestHeader.getContentLength();
		if(bodySize > maxBodySize) {
			throw new ReqTooLargeException("请求内容过大");
		}
		byte[] bodyBytes = readBody(bodySize);
		requestBody = new RequestBody(requestHeader.getContentType(), bodyBytes);
		//servletReq请求对象
		httpServletRequest = new HttpServletRequest(baseInfoStr, requestHeader, requestBody, cookie, session);
	}
	
	private String readHeader() throws IOException, SocketTimeoutException {
		ByteArrayOutputStream tempOutputStream = new ByteArrayOutputStream();
		//获取header
		int[] checkTemp = new int[4];
		int byteTemp;
		while(true) {
			byteTemp = bis.read();
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
		String headerStr = tempOutputStream.toString("UTF-8");
		tempOutputStream.reset();
		tempOutputStream.close();
		return headerStr;
	}
	
	private byte[] readBody(Integer length) throws IOException {
		if(length == null || length == 0) {
			return new byte[0];
		}
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		byte[] temp = new byte[4096];
		long lenSum = 0;
		while(true) {
			int len = bis.read(temp);
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
	
	private void initServletResponse() {
		responseHeader = new ResponseHeader(cookie);
		responseBody = new ResponseBody();
		httpServletResponse = new HttpServletResponse(responseHeader, responseBody);
	}
	
	private void response(int code, String status, String message) {
		try {
			byte[] temp;
			//响应头
			if(responseHeader == null) {
				responseHeader = new ResponseHeader(cookie);
				responseHeader.setContentType("text/plain");
			}
			//响应体
			if(responseBody == null) {
				responseBody = new ResponseBody();
				if(message != null) {
					temp = message.getBytes();
					responseBody.setBodyByte(temp);
					responseHeader.setContentLength(temp.length);
				}
			}
			//基本信息
			bos.write(("HTTP/1.1 "+ code +" "+ status +"\r\n").getBytes());
			//写入响应头
			temp = responseHeader.toString().getBytes();
			System.out.println(new String(temp));
			bos.write(temp);
			//写入分割线
			bos.write(13);
			bos.write(10);
			//写入响应体
			temp = responseBody.getBodyByte();
			System.out.println(new String(temp));
			bos.write(temp);
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
	
	private void getRespData() throws Exception {
		// 根据路由执行
		RouteParams routeParams = Route.getRouteMap().get(httpServletRequest.getPath());
		if(routeParams == null) {
			throw new NotFoundException("路由不存在");
		}
		//请求方法检查
		if(!routeParams.getRequestMethod().equals(RequestMethod.ALL) && !routeParams.getRequestMethod().toString().equals(httpServletRequest.getRequestMethod())) {
			throw new ReqMethodNotAllowedException("请求方法不合法");
		}
		Class<?> clazz = routeParams.getClazz();
		Method method = routeParams.getMethod();
		Parameter[] parameters = method.getParameters();
		Object[] paramsObjects = new Object[parameters.length]; //需要传入控制器方法中的参数
		for(int i = 0; i < parameters.length; i++) {
			Class<?> type = parameters[i].getType();
			if(type.equals(HttpServletRequest.class)) {
				paramsObjects[i] = httpServletRequest;
			}
			else if(type.equals(HttpServletResponse.class)) {
				paramsObjects[i] = httpServletResponse;
			}
		}
		Object bodyObject = method.invoke(clazz.getDeclaredConstructor().newInstance(), paramsObjects);
		if(bodyObject != null) {
			httpServletResponse.setResponse(bodyObject);
		}
	}
	
	private void close() throws IOException {
		if(bos != null) bos.close();
		if(bis != null) bis.close();
		client.close();
	}

}
