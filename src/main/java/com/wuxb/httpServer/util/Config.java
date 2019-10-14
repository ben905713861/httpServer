package com.wuxb.httpServer.util;

import java.util.MissingResourceException;
import java.util.ResourceBundle;

public class Config {
	
	private static final ResourceBundle resourceBundle;

	static {
		resourceBundle = ResourceBundle.getBundle("application");
	}

	public static String get(String key) {
		String value = null;
		try {
			value = resourceBundle.getString(key);
		} catch (MissingResourceException e) {
			System.err.println("配置项 "+ key +" 不存在");
			e.printStackTrace();
		}
		return value;
	}
	
}