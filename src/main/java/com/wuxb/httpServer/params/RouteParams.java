package com.wuxb.httpServer.params;

import java.lang.reflect.Method;

public class RouteParams {
	
	private RequestMethod requestMethod;
	private Class<?> clazz;
	private Method method;

	public Class<?> getClazz() {
		return clazz;
	}

	public void setClazz(Class<?> clazz) {
		this.clazz = clazz;
	}

	public Method getMethod() {
		return method;
	}

	public void setMethod(Method method) {
		this.method = method;
	}

	public RequestMethod getRequestMethod() {
		return requestMethod;
	}

	public void setRequestMethod(RequestMethod requestMethod) {
		this.requestMethod = requestMethod;
	}

	@Override
	public String toString() {
		return "RouteParams [requestMethod=" + requestMethod + ", clazz=" + clazz + ", method=" + method + "]";
	}

}