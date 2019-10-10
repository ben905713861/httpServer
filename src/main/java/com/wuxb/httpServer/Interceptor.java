package com.wuxb.httpServer;

public interface Interceptor {

	public void run(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse)  throws Exception;
	
}
