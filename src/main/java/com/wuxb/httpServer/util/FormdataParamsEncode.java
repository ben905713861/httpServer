package com.wuxb.httpServer.util;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import com.wuxb.httpServer.params.FileInfo;

public class FormdataParamsEncode {

	private List<String> keyList = new ArrayList<String>();
	private List<Object> valueList = new ArrayList<Object>();
	private ByteArrayOutputStream baos;
	private byte[] byteArray;
	private String boundary;
	
	public FormdataParamsEncode() {
		int max=99999999, min=10000000;
		int ran1 = (int) (Math.random()*(max-min)+min);
		int ran2 = (int) (Math.random()*(max-min)+min);
		int ran3 = (int) (Math.random()*(max-min)+min);
		boundary = "--------------------------" + ran1 + ran2 + ran3;
	}
	
	public void add(String key, String value) {
		keyList.add(key);
		valueList.add(value);
	}
	
	public void add(String key, Number value) {
		keyList.add(key);
		valueList.add(value);
	}
	
	public void add(String key, FileInfo value) {
		keyList.add(key);
		valueList.add(value);
	}
	
	public byte[] toByteArray() {
		if(byteArray != null) {
			return byteArray;
		}
		baos = new ByteArrayOutputStream();
		for(int i = 0; i < keyList.size(); i++) {
			String key = keyList.get(i);
			Object value = valueList.get(i);
			if(value instanceof String) {
				setStringValue(key, (String) value);
			}
			else if(value instanceof Number) {
				setStringValue(key, value.toString());
			}
			else if(value instanceof FileInfo) {
				setFileByte(key, (FileInfo) value);
			}
			else {
				System.err.println("类型不支持");
				continue;
			}
		}
		try {
			baos.write(("--"+ boundary +"--\r\n").getBytes());
			byteArray = baos.toByteArray();
			return byteArray;
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}
	
	private void setStringValue(String key, String value) {
		String temp = "--"+ boundary + "\r\n"+
			"Content-Disposition: form-data; name=\""+ key +"\"" + "\r\n"+
			"\r\n"+
			value + "\r\n";
		try {
			baos.write(temp.getBytes("UTF-8"));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private void setFileByte(String key, FileInfo fileInfo) {
		String temp = "--"+ boundary + "\r\n"+
			"Content-Disposition: form-data; name=\""+ key +"\"; filename=\""+ fileInfo.filename +"\"\r\n"+
			"Content-Type: "+ fileInfo.contentType +"\r\n"+
			"\r\n";
		try {
			FileInputStream fis = new FileInputStream(new File(fileInfo.path));
			byte[] byteTemp = fis.readAllBytes();
			fis.close();
			baos.write(temp.getBytes("UTF-8"));
			baos.write(byteTemp);
			baos.write("\r\n".getBytes());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public String getContentType() {
		return "multipart/form-data; boundary="+ boundary;
	}
	
}
