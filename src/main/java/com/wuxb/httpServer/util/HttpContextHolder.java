package com.wuxb.httpServer.util;

import com.wuxb.httpServer.HttpServletRequest;
import com.wuxb.httpServer.HttpServletResponse;

public final class HttpContextHolder {

	private static ThreadLocal<HttpServletRequest> requesThreadLocal = new ThreadLocal<HttpServletRequest>();
	private static ThreadLocal<HttpServletResponse> responseThreadLocal = new ThreadLocal<HttpServletResponse>();
	
	public static void set(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) {
		requesThreadLocal.set(httpServletRequest);
		responseThreadLocal.set(httpServletResponse);
	}
	
	public static HttpServletRequest getHttpServletRequest() {
		return requesThreadLocal.get();
	}
	
	public static HttpServletResponse getHttpServletResponse() {
		return responseThreadLocal.get();
	}
	
}
