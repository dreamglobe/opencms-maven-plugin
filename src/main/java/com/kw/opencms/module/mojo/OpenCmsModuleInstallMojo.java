package com.kw.opencms.module.mojo;

/*
 * Copyright 2001-2005 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.settings.Server;

import com.kw.opencms.module.mojo.packaging.ManifestGenerationTask;
import com.kw.opencms.module.mojo.packaging.ModulePackagingContext;
import com.kw.opencms.module.mojo.util.ManifestUtils;
import com.kw.opencms.module.mojo.util.OpenCmsScriptUtils;

/**
 * Goal which copies the module file to the OpenCms application configured in the project and then installs it.
 * The module file is copied to <code>${opencms.home}/WEB-INF/packages/modules</code> directory. The installation
 * is done via generated script by CmsShell. 
 *
 * @goal install-module
 * @requiresProject false
 */
public class OpenCmsModuleInstallMojo extends AbstractModuleMojo
{

	public static final String MANIFEST_MODULE_VERSION_PROPERTY 	= "manifest.module.version";
	public static final String MANIFEST_MODULE_NAME_PROPERTY 	 	= "manifest.module.name";
	public static final String MANIFEST_MODULE_NICENAME_PROPERTY 	= "manifest.module.nicename";
	public static final String ZERO_STRING = "";
	public static final String MODULES_RELATIVE_PATH = "packages" + File.separator + "modules" + File.separator;


	/**
	 * Server identification for settings credentials 
	 * @parameter property="opencms.server.id"
	 */
	private String openCmsServerAuthId;

	/**
	 * User that installs the module on the OpenCms instance.
	 * @parameter property="opencms.user.name"
	 */
	protected String openCmsUserName;

	/**
	 * Credentials for the OpenCms user.
	 * @parameter property="opencms.user.pass"
	 */
	protected String openCmsUserPass;

	/**
	 * BaseDir for OpenCms installation.
	 * Defaults to <code>${catalina.home}/webapps/ROOT"</code>.
	 * @parameter property="opencms.home" default-value="${catalina.home}/webapps/ROOT"
	 * @required
	 */
	protected String openCmsBaseDir;

	/**
	 * Mapping for the OpenCms dispatcher servlet.
	 * Defaults to "opencms/*".
	 * @parameter property="opencms.servlet.mapping" default-value="opencms/*"
	 */
	protected String openCmsServetMapping;


	/**
	 * BaseDir for Tomcat installation.
	 * @parameter property="catalina.base" default-value="${catalina.base}"
	 * @required
	 */
	protected File appServerBaseDir;

	/**
     * Location of the module file to install.
     * Defaults to <code>target/&lt;artifactId&gt;-&lt;version&gt;.zip</code>.
     * @parameter property="module.file" default-value="${project.build.directory}/${project.artifactId}-${project.version}.zip"
     * @required
     */
    private File moduleFile;

    /**
	 * Selects to update the module or a fresh install, for installing previous module version
	 * Defaults to <code>"true"</code>
	 * @parameter property="fresh.install" default-value="true"
	 */
	private boolean freshInstall;


    /**
	 * Punto de entrada del mojo
	 */
    public void execute() throws MojoExecutionException
    {
    	// Get the module name for previous deletion
    	String moduleName = getModuleName();

    	// check directories exits
       	checkConditions();

       	// get OpenCms Credentials
       	fillOpenCmsCredentials();

       	// Compose WEB-INF file for opencms
       	File openCmsWebInfDir = new File( openCmsBaseDir, "WEB-INF" );

       	// Copy the module to the install directory
    	copyFileToModulesDir(openCmsWebInfDir);

    	// Generate install script
    	File installScript;
		try
		{
			installScript = OpenCmsScriptUtils.buildInstallScript(
					moduleName, moduleFile, openCmsUserName,
					openCmsUserPass, freshInstall);
		}
		catch (IOException e)
		{
			throw new MojoExecutionException(
					"Error generando el script de instalación", e);
		}

		// Log script content
		if( getLog().isDebugEnabled() )
		{
			getLog().debug("Executing instalation script: " + installScript);
			getLog().debug( ManifestUtils.readFileAsStringNoException( installScript ));
		}

       // Execute the script
		OpenCmsScriptUtils.executeOpenCmsScript( openCmsWebInfDir, appServerBaseDir,
				openCmsServetMapping, openCmsWebappName, "opencms/> ", installScript);
    }

    /**
     * Calcula el nombre del módulo a instalar a partir de las propiedades del sistema
     * @return el nombre del módulo tal y como aparece en el manifiesto
     * @throws MojoExecutionException error durante la lectura de de las propiedades del módulo
     */
	protected String getModuleName() throws MojoExecutionException {
		// Calculate module name
    	if( !getProject().getProperties().containsKey(MANIFEST_MODULE_NAME_PROPERTY))
    	{
    		final ModulePackagingContext context = new DefaultModulePackagingContext();
    		try
    		{
				new ManifestGenerationTask().getManifestProperties(context);
			}
    		catch (MojoFailureException e)
    		{
				throw new MojoExecutionException(
						"Error recogiendo propiedades del módulo", e);
			}
    	}
    	return getProject().getProperties().getProperty(MANIFEST_MODULE_NAME_PROPERTY);
	}

	/**
	 * Rellena las credenciales de usuario desde la configuración
	 * usando el servidor indicado por la propiedad openCmsServerAuthId
	 */
	protected void fillOpenCmsCredentials()
	{
		if( openCmsServerAuthId != null
				&& !openCmsServerAuthId.isEmpty() )
		{
			Server serverAuth = getCredentialsFromServer( openCmsServerAuthId );
			if(serverAuth != null)
			{
				openCmsUserName = serverAuth.getUsername();
				openCmsUserPass = serverAuth.getPassword();
			}
		}
	}

	/**
	 * Comprueba que se cumplen las condiciones para instalar el módulo
	 */
	protected void checkConditions( ) throws MojoExecutionException
	{
		// check if module file exist
        if( PACKAGING_OPENCMS_MODULE.equals( getProject().getPackaging() ))
        {
        	if( !moduleFile.exists() )
	        {
	        	throw new MojoExecutionException(
	        		"El fichero de módulo " + moduleFile + "no existe!" );
	        }
        }
        else
        {
        	this.getLog().warn("El proyecto no es un módulo de OpenCms");
        }

        // Compose WEB-INF file for opencms
    	File openCmsWebDir = new File( openCmsBaseDir, "WEB-INF" );

        // check base dir for openCms
        if(!openCmsWebDir.exists())
        {
        	throw new MojoExecutionException(
        			"Directorio WEB-INF de OpenCms no existe " + openCmsWebDir );
        }
	}

	/**
     * Calcula la ruta del fichero destino e invoca la operacion de copia
     * @param openCmsWebDir directorio WEB-INF de OpenCms
     * @throws MojoExecutionException error durante la copia del fichero
     */
	private void copyFileToModulesDir(File openCmsWebDir)
			throws MojoExecutionException
	{
        File moduleInstallFile =  new File( openCmsWebDir,
        		MODULES_RELATIVE_PATH + moduleFile.getName() );

        getLog().info("Copiando módulo [" + moduleFile
        	+ "] a directorio de paquetes [" + moduleInstallFile + "]" );
        try
        {
			ManifestUtils.copyFile(moduleFile, moduleInstallFile);
		}
        catch (IOException e1)
		{
			throw new MojoExecutionException(
					"Error copiando el módulo a la carpeta de paquetes", e1);
		}
	}
}

