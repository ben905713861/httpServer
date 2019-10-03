package com.wuxb.httpServer.util;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HttpUrlParams {
	
	public static void main(String args[]) {
		Map<String, Object> map = new HashMap<String, Object>();
			List<Object> list = new ArrayList<Object>();
			list.add("aa");
			list.add(123);
			list.add(new Object[] {"qq", 11});
		map.put("list", list);
			Map<String, Object> childMap = new HashMap<String, Object>();
			childMap.put("num", 78);
			childMap.put("str", "str?!+-str");
			childMap.put("array", new int[] {44,55});
		map.put("childMap", childMap);
		map.put("array", new String[] {"ss", "Kk"});
		
		System.out.println(map);
		String encodeStr = urlencode(map, "");
		System.out.println(encodeStr);
		Object decodeObj = urldecode(encodeStr);
		System.out.println(decodeObj);
	}
	
	@SuppressWarnings("unchecked")
	public static String urlencode(Object params, String key) {
		String res = "";
		if(params instanceof Map) {
			Map<String, Object> _params = (Map<String, Object>) params;
			for(String i : _params.keySet()) {
				String k = key.isEmpty() ? i : (key +"["+ i +"]");
				String encodeValue = urlencode(_params.get(i), k);
				if(!encodeValue.isEmpty()) {
					res += '&'+ encodeValue;
				}
			}
		}
		else if(params instanceof List) {
			List<Object> _params = (List<Object>) params;
			for(Integer i = 0; i < _params.size(); i++) {
				String k = key.isEmpty() ? i.toString() : (key +"["+ i.toString() +"]");
				String encodeValue = urlencode(_params.get(i), k);
				if(!encodeValue.isEmpty()) {
					res += '&'+ encodeValue;
				}
			}
		}
		else if(params.getClass().isArray()) {
			Object[] _params;
			if(params instanceof Object[]) {
				_params = (Object[]) params;
			}
			else if(params instanceof String[]) {
				_params = (String[]) params;
			}
			else if(params instanceof int[]) {
				_params = Arrays.stream((int[]) params).boxed().toArray(Integer[]::new);
			}
			else if(params instanceof double[]) {
				_params = Arrays.stream((double[]) params).boxed().toArray(Double[]::new);
			}
			else {
				_params = new Object[] {};
			}
			for(Integer i = 0; i < _params.length; i++) {
				String k = key.isEmpty() ? i.toString() : (key +"["+ i.toString() +"]");
				String encodeValue = urlencode(_params[i], k);
				if(!encodeValue.isEmpty()) {
					res += '&'+ encodeValue;
				}
			}
		}
		else if(params instanceof String) {
			String _params = (String) params;
			try {
				res += '&'+ URLEncoder.encode(key, "UTF-8") +'='+ URLEncoder.encode(_params, "UTF-8");
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			}
		}
		else if(params instanceof Number) {
			Number _params = (Number) params;
			try {
				res += '&'+ URLEncoder.encode(key, "UTF-8") +'='+ URLEncoder.encode(_params.toString(), "UTF-8");
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			}
		}
		else {
			return "";
		}
		return res.substring(1);
	}
	
	
	@SuppressWarnings("unchecked")
	public static Map<String, Object> urldecode(String param) {
		Map<String, Object> map = new HashMap<String, Object>();
		if(param == null || param.isEmpty()) {
			return map;
		}
		//解码
		String[] params = param.split("&");
		Map<String, String> key2value = new TreeMap<String, String>();
		for(int i = 0; i < params.length; i++) {
			if(params[i].indexOf("=") == -1) {
				continue;
			}
			String[] p = params[i].split("=");
			if(p.length == 0) {
				continue;
			}
			try {
				String keyStr = URLDecoder.decode(p[0], "UTF-8");
				if(keyStr.isBlank()) {
					continue;
				}
				String valueStr;
				if(p.length == 2) {
					valueStr = URLDecoder.decode(p[1], "UTF-8");
				} else {
					valueStr = "";
				}
				key2value.put(keyStr, valueStr);
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			}
		}
		//遍历每一行传�?
		for(Map.Entry<String, String> entry : key2value.entrySet()) {
			String keyStr = entry.getKey();
			String value = entry.getValue();
			//根目录的key
			Matcher keyMatcher = Pattern.compile("^[a-zA-Z\\_]{1}[\\w]*").matcher(keyStr);
			if(!keyMatcher.find()) {
				continue;
			}
			String key = keyMatcher.group(0);
			if(!map.containsKey(key)) {
				map.put(key, new HashMap<String, Object>());
			}
			
			//二级以及二级目录以上的key
			String pattern = "\\[([\\w]+?)\\]";
			Matcher filterMatcher = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE).matcher(keyStr);
			//获取�?有的patternKey
			List<String> patternKeyList = new ArrayList<String>();
			while(filterMatcher.find()) {
				String patternKey = filterMatcher.group(1);
				patternKeyList.add(patternKey);
			}
			//有子元素
			if(!patternKeyList.isEmpty()) {
				//遍历并写�?
				Object childMap = map.get(key);
				int patternKeyListSize = patternKeyList.size();
				for(int j = 0; j < patternKeyListSize; j++) {
					String patternKey = patternKeyList.get(j);
					Map<String, Object> _childMap = (HashMap<String, Object>) childMap;
					if(!_childMap.containsKey(patternKey)) {
						//是否是最后一个节点，是的话直接赋�?
						if(j == patternKeyListSize-1) {
							_childMap.put(patternKey, value);
							break;
						}
						_childMap.put(patternKey, new HashMap<String, Object>());
					}
					childMap = _childMap.get(patternKey);
				}
			}
			//只有�?级元�?
			else {
				map.put(key, value);
			}
		}
		map = (Map<String, Object>) map2list(map);
		return map;
	}
	
	@SuppressWarnings("unchecked")
	private static Object map2list(Map<String, Object> map) {
		Set<String> keySet = map.keySet();
		boolean all_is_number = true;
		for(String key : keySet) {
			//不是数字
			if(!Pattern.matches("^[0-9]+$", key)) {
				all_is_number = false;
			}
			Object childNode = map.get(key);
			if(childNode instanceof Map) {
				childNode = map2list((Map<String, Object>) childNode);
				map.put(key, childNode);
			}
		}
		Object res;
		if(all_is_number) {
			res = new ArrayList<Object>();
			for(String key : keySet) {
				Object value = map.get(key);
				((List<Object>) res).add(value);
			}
		} else {
			res = map;
		}
		return res;
	}

}
