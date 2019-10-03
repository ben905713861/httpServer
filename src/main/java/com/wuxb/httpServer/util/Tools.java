package com.wuxb.httpServer.util;

import java.util.Date;
import java.util.LinkedList;
import java.util.List;

public class Tools {
	
	public static int time() {
		long tiemStamp = (new Date().getTime())/1000;
		return (int) tiemStamp;
	}
	
	public static String array2String(String glue, String[] strings) {
		String res = "";
		for (int i = 0; i < strings.length; i++) {
			String string = strings[i];
			res += string + glue;
		}
		res = res.substring(0, res.length()-1);
		return res;
	}
	
	public static Object[] array_merge(Object[] objects1, Object[] objects2) {
		List<Object> res = new LinkedList<Object>();
		if(objects1 != null) for(Object object : objects1) {
			res.add(object);
		}
		if(objects2 != null) for(Object object : objects2) {
			res.add(object);
		}
		return res.toArray();
	}
	
}
