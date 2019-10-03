package com.wuxb.httpServer.test.controller;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;

import com.wuxb.httpServer.HttpServletRequest;
import com.wuxb.httpServer.HttpServletResponse;
import com.wuxb.httpServer.annotation.RequestMapping;
import com.wuxb.httpServer.annotation.RestController;
import com.wuxb.httpServer.exception.RespTypeNotAllowedException;
import com.wuxb.httpServer.params.FileInfo;

@RestController
@RequestMapping("/index")
public class IndexController {
	
	@RequestMapping(value="/index")
	public String index(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) throws Exception {
		System.out.println(httpServletRequest.getBody().getBodyMap());
		return "hello world";
	}
	
	@RequestMapping("/json")
	public ArrayList<Object> json(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) {
		System.out.println(httpServletRequest.getBody().getBodyMap());
		System.out.println(httpServletRequest.getHeader().getContentLength());
		System.out.println(httpServletRequest.getCookie().get("SESSIONID"));
		httpServletRequest.getSession().set("name", "benbe");
		System.out.println(httpServletRequest.getSession().get("name"));
//		System.out.println(httpServletRequest.getRequestMethod());
		ArrayList<Object> list = new ArrayList<Object>();
		list.add("kk");
		list.add(45);
		return list;
	}
	
	@RequestMapping(value="/file")
	public void file(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) throws IOException, RespTypeNotAllowedException {
//		if(1==1)throw new IOException();
//		System.out.println(httpServletRequest.getQueryParams());
//		System.out.println(httpServletRequest.getBody().getBodyMap());
//		System.out.println(httpServletRequest.getBody().getFileList());
		FileInfo fileInfo = httpServletRequest.getBody().getFileList().get(0);
		FileInputStream fis = new FileInputStream(new File(fileInfo.path));
		byte[] fileBytes = fis.readAllBytes();
		fis.close();
		httpServletResponse.getHeader().setContentType(fileInfo.contentType);
		httpServletResponse.setResponse(fileBytes);
	}
	
}
