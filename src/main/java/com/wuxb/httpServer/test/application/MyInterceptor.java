package com.wuxb.httpServer.test.application;

import java.io.InputStream;

import com.wuxb.httpServer.HttpServletRequest;
import com.wuxb.httpServer.HttpServletResponse;
import com.wuxb.httpServer.Interceptor;
import com.wuxb.httpServer.ResponseHeader;
import com.wuxb.httpServer.exception.ForbiddenException;
import com.wuxb.httpServer.exception.InterceptInterruptException;
import com.wuxb.httpServer.exception.NotFoundException;
import com.wuxb.httpServer.exception.UnauthorizedException;

public class MyInterceptor implements Interceptor {

	private HttpServletRequest httpServletRequest;
	private HttpServletResponse httpServletResponse;
	
	@Override
	public void run(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse)  throws Exception {
		this.httpServletRequest = httpServletRequest;
		this.httpServletResponse = httpServletResponse;
		//静态资源处理
		staticSource();
//		checkLogin();
//		checkPremission();
	}
	
	private void staticSource() throws Exception {
		String path = httpServletRequest.getPath();
		if(path.indexOf("/static") != 0) {
			return;
		}
		InputStream is = ClassLoader.getSystemResourceAsStream(path.substring(1));
		if(is == null) {
			throw new NotFoundException(path +"，资源不存在");
		}
		byte[] respByte = is.readAllBytes();
		httpServletResponse.getBody().setBodyByte(respByte);
		ResponseHeader respHeader = httpServletResponse.getHeader();
		respHeader.setContentLength(respByte.length);
		String extName = path.substring(path.lastIndexOf(".") + 1);//扩展名
		switch(extName) {
			case "html":
			case "htm":
				respHeader.setContentType("text/html; charset=UTF-8");
				break;
			case "css":
				respHeader.setContentType("text/css; charset=UTF-8");
				break;
			case "js":
				respHeader.setContentType("application/x-javascript; charset=UTF-8");
				break;
			case "jpg":
			case "jpeg":
			case "png":
			case "gif":
				respHeader.setContentType("image/"+ extName);
				break;
			case "pdf":
				respHeader.setContentType("application/pdf");
				break;
			case "txt":
				respHeader.setContentType("text/plain; charset=UTF-8");
				break;
			default:
				respHeader.setContentType("application/octet-stream");
				break;
			}
		respHeader.set("Cache-Control", "max-age=10");
		throw new InterceptInterruptException();
	}
	
	private void checkLogin() throws UnauthorizedException {
		throw new UnauthorizedException("尚未登录");
	}
	
	private void checkPremission() throws ForbiddenException {
		throw new ForbiddenException("没有权限访问");
	}
	
}
