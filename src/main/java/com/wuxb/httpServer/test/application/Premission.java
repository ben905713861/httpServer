package com.wuxb.httpServer.test.application;

import com.wuxb.httpServer.HttpServletRequest;
import com.wuxb.httpServer.Interceptor;
import com.wuxb.httpServer.exception.ForbiddenException;
import com.wuxb.httpServer.exception.UnauthorizedException;

public class Premission implements Interceptor {

	@Override
	public void run(HttpServletRequest httpServletRequest)  throws Exception {
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
