package com.wuxb.httpServer;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.wuxb.httpServer.annotation.RequestMapping;
import com.wuxb.httpServer.annotation.RestController;
import com.wuxb.httpServer.params.RequestMethod;
import com.wuxb.httpServer.params.RouteParams;

public class Route {

	private static Map<String, RouteParams> routeMap = new HashMap<String, RouteParams>();
	
	static {
		List<Class<?>> classList = ClassReader.getAllClass();
		for(Class<?> clazz : classList) {
			//筛选类注解，必须是控制器
			if(!clazz.isAnnotationPresent(RestController.class)) {
				continue;
			}
			String reqMap = "";
			if(clazz.isAnnotationPresent(RequestMapping.class)) {
				RequestMapping requestMapping = clazz.getAnnotation(RequestMapping.class);
				reqMap = requestMapping.value();
			}
			//获取方法
			Method[] methods = clazz.getDeclaredMethods();
			for(Method method : methods) {
				//验证方法类型，1public 2private 4protected
				if(method.getModifiers() != 1) {
					continue;
				}
				//检查每个方法是否有RequestMapping注解
				if(method.isAnnotationPresent(RequestMapping.class)) {
					RequestMapping requestMapping = method.getAnnotation(RequestMapping.class);
					RequestMethod requestMethod = requestMapping.method();
					String routeUrl = reqMap + requestMapping.value();
					RouteParams routeParams = new RouteParams();
					routeParams.setRequestMethod(requestMethod);
					routeParams.setClazz(clazz);
					routeParams.setMethod(method);
					routeMap.put(routeUrl, routeParams);
				}
			}
		}
	}
	
	public static Map<String, RouteParams> getRouteMap() {
		return routeMap;
	}
	
}