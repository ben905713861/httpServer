package com.wuxb.httpServer;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import com.wuxb.httpServer.exception.HttpErrorException;
import com.wuxb.httpServer.exception.HttpInterceptInterrupt;
import com.wuxb.httpServer.util.Config;

public class UserStaticSource extends StaticSource {

	private static final String FILE_BASE_DIR = Config.get("http.staticSource.userUploadDir");
	
	public UserStaticSource(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) {
		super(httpServletRequest, httpServletResponse);
	}
	
	@Override
	public void action() throws IOException, HttpInterceptInterrupt, HttpErrorException {
		if(path.equals("/")) {
			return;
		}
		//扩展名
		String extName = path.substring(path.lastIndexOf(".") + 1);
		//body
		setRespBody(extName);
		//ContentType
		setContentType(extName);
		//浏览器缓存
		responseHeader.set("Cache-Control", "max-age="+ CACHE_TIME);
		httpServletResponse.setResponseCode(200);
		throw new HttpInterceptInterrupt();
	}
	
	@Override
	protected InputStream getInputStream() throws IOException {
		String fileFullPath = FILE_BASE_DIR + path;
		File file = new File(fileFullPath);
		if(!file.isFile()) {
			return null;
		}
		InputStream is = new FileInputStream(file);
		return is;
	}

}
