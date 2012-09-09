package com.kw.opencms.module.mojo;

import java.io.File;

import org.apache.maven.model.Resource;

public class ModuleResource extends Resource
{
	/**
     * Field moduleTargetPath Ruta dentro del módulo según el manifiesto
     */
    private String moduleTargetPath;

    /**
     * Field systemModule shortcut for setting moduleTargetPath to system/modules/${project.groupId}.${project.artifactId}/
     */
    private boolean systemModule = false;

    /**
	 * Activate parameter whether apply native 2 ascii to the module
     */
	protected Boolean n2aApply;

	/**
	 * Plain encoding config parameter
     */
	protected PlainEncodingConfig n2aConfig;

	/**
	 *
	 */
	protected File moduleWorkingPath;


	/// Getters and Setters

	public String getModuleTargetPath() {
		return moduleTargetPath;
	}

	public void setModuleTargetPath(String moduleTargetPath) {
		this.moduleTargetPath = moduleTargetPath;
	}

	public boolean isSystemModule() {
		return systemModule;
	}

	public void setSystemModule(boolean systemModule) {
		this.systemModule = systemModule;
	}

	public Boolean isN2aApply() {
		if( n2aApply == null && n2aConfig != null )
		{
			n2aApply = true;
		}
		else if(n2aApply == null)
		{
			n2aApply = false;
		}
		return n2aApply;
	}

	public void setN2aApply(Boolean n2aApply) {
		this.n2aApply = n2aApply;
	}

	public PlainEncodingConfig getN2aConfig() {
		return n2aConfig;
	}

	public void setN2aConfig(PlainEncodingConfig n2aConfig) {
		this.n2aConfig = n2aConfig;
	}

	public File getModuleWorkingPath()
	{
		return moduleWorkingPath;
	}

	public void setModuleWorkingPath(File file) {
		this.moduleWorkingPath = file;
	}

}
