package com.wuxb.httpServer.util;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.io.IOException;
import java.net.Socket;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.net.ssl.SSLSocketFactory;


public class Curl {

	private String method = "GET";
	private boolean useSSL;
	private String host;
	private int port;
	private String uri;
	private Map<String, String> headers = new LinkedHashMap<String, String>();
	private byte[] bodyByte;
	private Map<String, String> respHeaders = new LinkedHashMap<String, String>();
	private String respBody = "";
	
	public static void main(String[] args) {
		System.out.println(simpleGet("https://www.baidu.com/"));
	}
	
	public static String simpleGet(String url) {
		Curl curl = new Curl(url);
		curl.send();
		return curl.getResponseData();
	}
	
	public Curl(String url) {
		setUrl(url);
	}
	
	public Curl() {
		
	}
	
	public void setMethod(String method) {
		this.method = method;
	}
	
	public void setUrl(String url) {
		Matcher keyMatcher = Pattern.compile("^(http|https)://([\\w\\.\\-]+)(:[\\d]*)?(/.*)?$").matcher(url);
		if(!keyMatcher.find()) {
			System.err.println("url不合法");
			return;
		}
		String clientType = keyMatcher.group(1);
		if(clientType.equals("http")) {
			useSSL = false;
		} else {
			useSSL = true;
		}
		host = keyMatcher.group(2);
		String portStr = keyMatcher.group(3);
		if(portStr == null) {
			port = useSSL ? 443 : 80;
		} else {
			port = Integer.parseInt(portStr.substring(1));
		}
		uri = keyMatcher.group(4);
		if(uri == null || uri.isEmpty()) {
			uri = "/";
		}
		//header
		headers.put("Host", host);
		headers.put("Connection", "Close");
	}
	
	public void setContentType(String contentType) {
		headers.put("Content-Type", contentType);
	}
	
	public void setHeader(String key, String value) {
		headers.put(key, value);
	}
	
	public void setBodyStr(String bodyStr) {
		try {
			bodyByte = bodyStr.getBytes("UTF-8");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
	}
	
	public void setBodyByte(byte[] bodyByte) {
		this.bodyByte = bodyByte;
	}
	
	public void send() {
		Socket socket = null;
		BufferedOutputStream bos = null;
		BufferedReader br = null;
		try {
			if(useSSL) {
				socket = SSLSocketFactory.getDefault().createSocket(host, port);
			} else {
				socket = new Socket(host, port);
			}
			bos = new BufferedOutputStream(socket.getOutputStream());
			//头第一行基本信息
			bos.write((method.toUpperCase() +" "+ uri + " HTTP/1.0\r\n").getBytes("UTF-8"));
			//请求头
			String headerStr = "";
			for(Entry<String, String> encrypt : headers.entrySet()) {
				headerStr += encrypt.getKey() +": "+ encrypt.getValue() +"\r\n";
			}
			bos.write(headerStr.getBytes("UTF-8"));
			//分割线
			bos.write(13);
			bos.write(10);
			//请求体
			if(bodyByte != null) {
				bos.write(bodyByte);
			}
			//发送
			bos.flush();
			//接收数据
			br = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));
			String respBaseInfo = br.readLine();
			//响应头
			String line;
			String respHeaderStr = "";
			while(!(line = br.readLine()).isEmpty()) {
				respHeaderStr += line + "\r\n";
			}
			setRespHeaders(respHeaderStr);
			String respCode = respBaseInfo.split(" ")[1];
			if(!respCode.equals("200")) {
				System.err.println("请求失败，响应码不是200，"+ respCode);
				return;
			}
			//响应体
			while((line = br.readLine()) != null) {
				respBody += line + "\r\n";
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				if(bos != null) bos.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
			try {
				if(br != null) br.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
			try {
				if(socket != null) socket.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	private void setRespHeaders(String headerStr) {
		headerStr = headerStr.trim();
		String[] keyValueArray = headerStr.split("\r\n");
		for(String keyValue : keyValueArray) {
			String[] temp = keyValue.split(":");
			String key = temp[0].trim();
			String value = temp[1].trim();
			//赋值
			respHeaders.put(key, value);
		}
	}
	
	public Map<String, String> getResponseHeaders() {
		return respHeaders;
	}
	
	public String getResponseData() {
		return respBody;
	}
	
}
