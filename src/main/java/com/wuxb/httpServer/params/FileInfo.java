package com.wuxb.httpServer.params;

public class FileInfo {
	
	public String key;
	public String filename;
	public String extname;
	public String contentType;
	public String path;
	
	@Override
	public String toString() {
		return "FileInfo [key=" + key + ", filename=" + filename + ", extname=" + extname + ", contentType="
				+ contentType + ", path=" + path + "]";
	}
	
}