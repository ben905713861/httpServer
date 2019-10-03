package com.wuxb.httpServer;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;


public class ClassReader {

	private static ClassLoader classLoader;
	private static String runFilePath;
	private static boolean is_jar;
	private static List<Class<?>> classList = new ArrayList<Class<?>>();
	
	static {
		classLoader = Thread.currentThread().getContextClassLoader();
		//获取main()运行类
		StackTraceElement[] stackTraceArray = Thread.currentThread().getStackTrace();
		StackTraceElement stackTrace = stackTraceArray[stackTraceArray.length-1];
		String mainClassName = stackTrace.getClassName();
		try {
			runFilePath = classLoader.loadClass(mainClassName).getProtectionDomain().getCodeSource().getLocation().getPath();
			//是否jar运行
			is_jar = new File(runFilePath).isFile();
System.out.println(runFilePath);
			if(is_jar) {
				scanJar();
			} else {
				scan("");
			}
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
	}

	public static List<Class<?>> getAllClass() {
		return classList;
	}

	// 扫描文件目录
	private static void scan(String packageName) {
		String baseFilePath = classLoader.getResource("").getFile();
		String filePath = baseFilePath + packageName.replace(".", "/");
		File dir = new File(filePath);
		// 过滤class文件和文件夹
		File[] dirFiles = dir.listFiles(new FileFilter() {
			@Override
			public boolean accept(File file) {
				boolean acceptDir = file.isDirectory();// 接受dir目录
				boolean acceptClass = file.getName().endsWith("class");// 接受class文件
				return acceptDir || acceptClass;
			}
		});
		// 遍历当前目录内的文件和文件夹
		for(File file : dirFiles) {
			String childPath = file.getName();
			// 是文件夹
			if(file.isDirectory()) {
				String childPackageName;
				if(packageName.equals("")) {
					childPackageName = childPath;
				} else {
					childPackageName = packageName + "." + childPath;
				}
				scan(childPackageName);
			}
			// 不是文件夹
			else {
				String fullClazzName = packageName + "." + childPath.substring(0, childPath.length() - 6);
				try {
					classList.add(classLoader.loadClass(fullClazzName));
				} catch(ClassNotFoundException e) {
					e.printStackTrace();
				}
			}
		}
	}

	// 扫描jar包
	private static void scanJar() {
		JarFile jarFile;
		try {
			jarFile = new JarFile(new File(runFilePath));
			Enumeration<JarEntry> entry = jarFile.entries();
			while (entry.hasMoreElements()) {
				JarEntry jarEntry = (JarEntry) entry.nextElement();
				// 排除文件夹
				if(jarEntry.isDirectory()) {
					continue;
				}
				String fileName = jarEntry.getName();
				// 不是class文件
				if(!fileName.endsWith(".class")) {
					continue;
				}
				if(fileName.charAt(0) == '/') {
					fileName = fileName.substring(1);
				}
				String fullClazzName = fileName.substring(0, fileName.length()-6).replace("/", ".");
				try {
					classList.add(classLoader.loadClass(fullClazzName));
				} catch(ClassNotFoundException e) {
					e.printStackTrace();
				}
			}
			jarFile.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
