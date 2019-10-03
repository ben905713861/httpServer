package com.wuxb.httpServer;

public interface Interceptor {

	public void run(HttpServletRequest httpServletRequest)  throws Exception;
	
}
