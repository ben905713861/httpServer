package com.wuxb.httpServer.util;

public class HtmlFilter {

	public static String stripTags(String html) {
		String pattern1 = "<\\s*/?[a-zA-Z]+\\s*>";
		String pattern2 = "<\\s*[a-zA-Z]+\\s*/\\s*>";
		String pattern3 = "<.*?/\\s*>";
		return html.replaceAll(pattern1, "").replaceAll(pattern2, "").replaceAll(pattern3, "");
	}
	
}
