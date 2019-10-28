package com.wuxb.httpServer.util;

public class HtmlFilter {

	public static String stripTags(String html) {
		return html.replaceAll("<([^>]*)>", "");
	}

	//将特定标记换成转义字符
	public static String htmlspecialcharsEncode(String input) {
		StringBuffer filtered = new StringBuffer(input.length());
		char c;
		for (int i = 0; i < input.length(); i++) {
			c = input.charAt(i);
			switch (c) {
				case '<':
					filtered.append("&lt;");
					break;
				case '>':
					filtered.append("&gt;");
					break;
				case '"':
					filtered.append("&quot;");
					break;
				case '\'':
					filtered.append("&#39;");
					break;
				case '&':
					filtered.append("&amp;");
					break;
				default:
					filtered.append(c);
			}
		}
		return filtered.toString();
	}
	
	//将转义字符还原成特定标记
		public static String htmlspecialcharsDecode(String input) {
			return input.replaceAll("&lt;", "<")
				.replaceAll("&gt;", ">")
				.replaceAll("&quot;", "\"")
				.replaceAll("&#39;", "'")
				.replaceAll("&amp;", "&");
		}
	
}
