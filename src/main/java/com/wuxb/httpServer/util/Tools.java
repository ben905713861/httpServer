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
	
	public static long ip2long(String ip) {
		String[] temp = ip.split("\\.");
		int a0 = Integer.parseInt(temp[0]);
		int a1 = Integer.parseInt(temp[1]);
		int a2 = Integer.parseInt(temp[2]);
		int a3 = Integer.parseInt(temp[3]);
		long res = a3 + a2*256 + a1*65536 + a0*16777216;
		return res;
	}
	
	public static String long2ip(long longIp) {
		StringBuffer sb = new StringBuffer("");
		// 直接右移24位
		sb.append(String.valueOf((longIp >>> 24)));
		sb.append(".");
		// 将高8位置0，然后右移16位
		sb.append(String.valueOf((longIp & 0x00FFFFFF) >>> 16));
		sb.append(".");
		// 将高16位置0，然后右移8位
		sb.append(String.valueOf((longIp & 0x0000FFFF) >>> 8));
		sb.append(".");
		// 将高24位置0
		sb.append(String.valueOf((longIp & 0x000000FF)));
		return sb.toString();
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
