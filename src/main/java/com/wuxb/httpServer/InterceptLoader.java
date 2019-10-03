package com.wuxb.httpServer;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class InterceptLoader {

	private static Map<Class<?>, Method> claszz2methodList = new HashMap<Class<?>, Method>();
	
	static {
		List<Class<?>> classList = ClassReader.getAllClass();
		for(Class<?> clazz : classList) {
			//筛选类注解，必须是拦截器
			Class<?>[] interfaces = clazz.getInterfaces();
			if(interfaces.length == 0) {
				continue;
			}
			if(interfaces[0] == Interceptor.class) {
				try {
					Method method = clazz.getDeclaredMethod("run", HttpServletRequest.class);
					claszz2methodList.put(clazz, method);
				} catch (NoSuchMethodException | SecurityException e) {
					e.printStackTrace();
				}
			}
		}
	}
	
	public static void run(HttpServletRequest httpServletRequest) throws Exception {
		for(Entry<Class<?>, Method> entry : claszz2methodList.entrySet()) {
			Class<?> clazz = entry.getKey();
			Method method = entry.getValue();
			Object newObject = clazz.getDeclaredConstructor().newInstance();
			try {
				method.invoke(newObject, httpServletRequest);
			} catch (InvocationTargetException e) {
				throw (Exception) e.getCause();
			}
		}
	}
	
}
