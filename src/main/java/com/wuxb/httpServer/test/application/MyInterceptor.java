package com.wuxb.httpServer.test.application;

import com.wuxb.httpServer.HttpServletRequest;
import com.wuxb.httpServer.HttpServletResponse;
import com.wuxb.httpServer.Interceptor;
import com.wuxb.httpServer.StaticSource;
import com.wuxb.httpServer.exception.ForbiddenException;
import com.wuxb.httpServer.exception.UnauthorizedException;

public class MyInterceptor implements Interceptor {

	private HttpServletRequest httpServletRequest;
	private HttpServletResponse httpServletResponse;
	
	@Override
	public void run(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse)  throws Exception {
		this.httpServletRequest = httpServletRequest;
		this.httpServletResponse = httpServletResponse;
		//静态资源处理
		new StaticSource(httpServletRequest, httpServletResponse).action();
//		checkLogin();
//		checkPremission();
	}
	
	private void checkLogin() throws UnauthorizedException {
		throw new UnauthorizedException("尚未登录");
	}
	
	private void checkPremission() throws ForbiddenException {
		throw new ForbiddenException("没有权限访问");
	}
	
}
