package com.wuxb.httpServer.util;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.wuxb.httpServer.params.FileInfo;

public class FormdataParamsDecode {
	
	//结果集
	private Map<String, Object> paramsMap;
	private List<FileInfo> fileList;
	
	public static FormdataParamsDecode getInstance(Map<String, Object> paramsMap, List<FileInfo> fileList) {
		return new FormdataParamsDecode(paramsMap, fileList);
	}
	
	public FormdataParamsDecode(Map<String, Object> paramsMap, List<FileInfo> fileList) {
		this.paramsMap = paramsMap;
		this.fileList = fileList;
	}
	
	public void decode(String contentType, byte[] bodyByte) {
		//分割线
		String boundary = "--" + contentType.split("boundary=")[1];
		byte[] boundaryByte = boundary.getBytes();
		int boundaryLength = boundaryByte.length;
		//数据体处理
		List<Integer> boundaryIndexList = new LinkedList<Integer>();
		OUT:
		for(int i = 0; i < bodyByte.length - boundaryLength - 4 + 1; i++) {
			for(int j = 0; j < boundaryLength; j++) {
				if(bodyByte[j+i] != boundaryByte[j]) {
					continue OUT;
				}
			}
			//匹配成功，定义分割线在数据体中的位置
			boundaryIndexList.add(i);
		}
		//取出分割线后的值
		for(int i = 0; i < boundaryIndexList.size()-1; i++) {
			int start = boundaryIndexList.get(i) + boundary.length();
//			System.out.println("start: " + ((char) bodyByte[start]));
			int end = boundaryIndexList.get(i+1);
//			System.out.println("end: " + ((char) bodyByte[end]));
			int size = end - start;
			byte[] temp = new byte[size];
			int tempIndex = 0;
			for(int j = start; j < end; j++) {
				temp[tempIndex] = bodyByte[j];
				tempIndex++;
			}
			getKeyValue(temp);//分离key value
		}
	}
	
	//分离key value
	private void getKeyValue(byte[] temp) {
		byte[] boundBytes = "\r\n\r\n".getBytes();
		int boundIndex = 0;
		OUT:
		for(int i = 0; i < temp.length; i++) {
			for(int j = 0; j < boundBytes.length; j++) {
				if(temp[j+i] != boundBytes[j]) {
					continue OUT;
				}
			}
			boundIndex = i;
			break;
		}
		//取出属性字符串
		byte[] attribute = new byte[boundIndex];
		for(int i = 0; i < boundIndex; i++) {
			attribute[i] = temp[i];
		}
		//取出结果值
		byte[] value = new byte[temp.length - (boundIndex + boundBytes.length)];
		int index = 0;
		for(int i = boundIndex + boundBytes.length; i < temp.length; i++) {
			value[index] = temp[i];
			index++;
		}
		decodeAttributes(new String(attribute), value);//分离属性值
	}
	
	//分离属性值
	private void decodeAttributes(String attribute, byte[] valueBytes) {
		String key = null;
		String filename = null;
		String contentType = null;
		//key
		Matcher nameMatcher = Pattern.compile("name=\\\"(.*?)\\\"").matcher(attribute);
		if(nameMatcher.find()) {
			key = nameMatcher.group(1);
		} else {
			System.err.println("formdata key is null");
			return;
		}
		//filename
		Matcher filenameMatcher = Pattern.compile("filename=\\\"(.*?)\\\"").matcher(attribute);
		if(filenameMatcher.find()) {
			filename = filenameMatcher.group(1);
		}
		//content-type
		Matcher contentTypeMatcher = Pattern.compile("Content-Type: (.*?)$").matcher(attribute);
		if(contentTypeMatcher.find()) {
			contentType = contentTypeMatcher.group(1);
		}
		//是文件还是k-v
		if(filename == null) {
			String value = new String(trim(valueBytes));
			if(Pattern.matches("^[0-9]+$", value)) {
				paramsMap.put(key, Integer.parseInt(value));
			} else {
				paramsMap.put(key, value);
			}
		}
		else {
			FileInfo keyValue = new FileInfo();
			keyValue.key = key;
			keyValue.filename = filename;
			//extname 扩展名
			keyValue.extname = filename.substring(filename.lastIndexOf(".") + 1);
			keyValue.contentType = contentType;
			//写文件
			try {
				keyValue.path = saveTempFile(trim(valueBytes));
			} catch (IOException e) {
				e.printStackTrace();
				return;
			}
			fileList.add(keyValue);
		}
	}
	
	private static String saveTempFile(byte[] fileBytes) throws IOException {
		String tempPath = System.getProperty("java.io.tmpdir") + Encrypt.md5(Math.random() + "-" + new Date().getTime());
		FileOutputStream fos = new FileOutputStream(tempPath);
		BufferedOutputStream bos = new BufferedOutputStream(fos);
		bos.write(fileBytes);
		bos.close();
		return tempPath;
	}
	
	private static byte[] trim(byte[] bytes) {
		int start = 0;
		int end = bytes.length - 1;
		for(int i = start; i < bytes.length; i++) {
			if(bytes[i] == 10 || bytes[i] == 13 || bytes[i] == 32) {
				continue;
			}
			start = i;
			break;
		}
		for(int i = end; i >= 0; i--) {
			if(bytes[i] == 10 || bytes[i] == 13 || bytes[i] == 32) {
				continue;
			}
			end = i;
			break;
		}
		//结果
		byte[] resBytes = new byte[end - start +1];
		int index = 0;
		for(int i = start; i <= end; i++) {
			resBytes[index] = bytes[i];
			index++;
		}
		return resBytes;
	}
	
}
