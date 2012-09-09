package com.kw.opencms.module.mojo;

import java.io.File;
import java.util.List;

public class PlainEncodingConfig {

//	public File _src;
//	public File _dest;
//
//	/**
//	 * File extension to use in renaming output files
//	 */
//	private String ext;
	/**
	 * The native encoding the files are in (default is the default encoding for the JVM)
	 */
	private String encoding;
	/**
	 * list of patterns of files that must be included. All files are included when omitted
	 */
	private List<String> includes;
	/**
	 * list of patterns of files that must be excluded. No files (except default excludes) are excluded when omitted.
	 */
	private List<String> excludes;


	public PlainEncodingConfig() {
	}


//	public String getExt() {
//		return ext;
//	}
//
//	public void setExt(String ext) {
//		this.ext = ext;
//	}

	public String getEncoding() {
		return encoding;
	}

	public void setEncoding(String encoding) {
		this.encoding = encoding;
	}

	public List getIncludes() {
		return includes;
	}

	public void setIncludes(List includes) {
		this.includes = includes;
	}


	public List getExcludes() {
		return excludes;
	}


	public void setExcludes(List excludes) {
		this.excludes = excludes;
	}
}
