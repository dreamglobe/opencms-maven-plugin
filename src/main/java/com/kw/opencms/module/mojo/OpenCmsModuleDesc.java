package com.kw.opencms.module.mojo;

import java.io.File;

/**
 *
 *
 */
public class OpenCmsModuleDesc {

	protected String moduleName;
	protected File moduleFile;
	protected Boolean install;
	protected Boolean uninstall;

	public String getModuleName() {
		return moduleName;
	}
	public void setModuleName(String moduleName) {
		this.moduleName = moduleName;
	}
	public File getModuleFile() {
		return moduleFile;
	}
	public void setModuleFile(File moduleFile) {
		this.moduleFile = moduleFile;
	}
	public Boolean isInstall() {
		return install;
	}
	public void setInstall(Boolean install) {
		this.install = install;
	}
	public Boolean isUninstall() {
		return uninstall;
	}
	public void setUninstall(Boolean uninstall) {
		this.uninstall = uninstall;
	}
	public OpenCmsModuleDesc() {
	}
}
