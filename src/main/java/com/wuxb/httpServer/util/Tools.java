package com.wuxb.httpServer.util;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class Tools {
	
	public static int time() {
		long tiemStamp = (new Date().getTime())/1000;
		return (int) tiemStamp;
	}
	
	public static int dateStr2time(String dateStr) {
		try {
			long time = new SimpleDateFormat("yyyy-MM-dd").parse(dateStr).getTime()/1000;
			return (int) time;
		} catch (ParseException e) {
			e.printStackTrace();
			return 0;
		}
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
	
	public static Map<String, Object> returnErr(String msg) {
		Map<String, Object> res = new HashMap<String, Object>();
		res.put("status", false);
		res.put("msg", msg);
		return res;
	}
	
	public static Map<String, Object> returnSucc() {
		Map<String, Object> res = new HashMap<String, Object>();
		res.put("status", true);
		return res;
	}
	
}
