package com.kw.opencms.module.mojo.util;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;

public class ClassPathHacker2 {

	public static void addFile(String s) throws IOException {
		File f = new File(s);
		addFile(f);
	}// end method

	public static void addFile(File f) throws IOException {
		addURL(f.toURI().toURL());
	}// end method

	public static void addURL(URL u) throws IOException {
		URL urls[] = new URL[]{u};
		ClassLoader aCL = Thread.currentThread().getContextClassLoader();
		URLClassLoader aUrlCL = new URLClassLoader(urls, aCL);

		Thread.currentThread().setContextClassLoader(aUrlCL);
	}
	
	public static void addURL(URL[] urls) throws IOException {
		ClassLoader aCL = Thread.currentThread().getContextClassLoader();
		URLClassLoader aUrlCL = new URLClassLoader(urls, aCL);

		Thread.currentThread().setContextClassLoader(aUrlCL);
	}
}