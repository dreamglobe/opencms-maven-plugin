package com.kw.opencms.module.mojo;

import java.io.File;
import java.io.IOException;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.settings.Server;

import com.kw.opencms.module.mojo.packaging.ManifestGenerationTask;
import com.kw.opencms.module.mojo.packaging.ModulePackagingContext;
import com.kw.opencms.module.mojo.util.ManifestUtils;
import com.kw.opencms.module.mojo.util.OpenCmsScriptUtils;

/**
 * Generates and execute a script for deleting the module indicate by property <code>manifest.module.name</code>.
 * If no property as this exist searches for the manifest properties descriptor in the working dir.
 *
 * @goal uninstall-module
 * @requiresProject false
 */
public class OpenCmsModuleUninstallMojo extends AbstractModuleMojo
{

	public static final String MANIFEST_MODULE_VERSION_PROPERTY 	= "manifest.module.version";
	public static final String MANIFEST_MODULE_NAME_PROPERTY 	 	= "manifest.module.name";
	public static final String MANIFEST_MODULE_NICENAME_PROPERTY 	= "manifest.module.nicename";
	public static final String ZERO_STRING = "";
	public static final String MODULES_RELATIVE_PATH = "packages" + File.separator + "modules" + File.separator;

	/**
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
	 * Base dir for OpenCms installation.
	 * Defaults to <code>${catalina.home}/webapps/ROOT"</code>.
	 * @parameter property="opencms.home" default-value="${catalina.home}/webapps/ROOT"
	 * @required
	 */
	protected String openCmsBaseDir;

	/**
	 * Mapping for the OpenCms dispatcher servlet.
	 * Defaults to "opencms/*".
	 * @parameter property="opencms.servlet.mapping"
	 */
	protected String openCmsServetMapping;

	/**
	 * User that installs the module on the OpenCms instance.
	 * @parameter property="server.user.name"
	 */
	protected String appServerUserName;

	/**
	 * Credentials for the OpenCms user.
	 * @parameter property="server.user.pass"
	 */
	protected String appServerUserPass;

	/**
	 * Base dir for servlet container installation (tomcat).
	 * Defaults to <code>${catalina.base}"</code>.
	 * @parameter property="catalina.base}" default-value="${catalina.base"
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


		try
		{
			// Generate install script
			File uninstallScript = OpenCmsScriptUtils.buildUninstallScript(
					moduleName, openCmsUserName, openCmsUserPass);

			// Log script content
			if( getLog().isDebugEnabled() )
			{
				getLog().debug("Executing instalation script: " + uninstallScript);
				getLog().debug( ManifestUtils.readFileAsStringNoException( uninstallScript ));
			}

	       // Execute the script
			OpenCmsScriptUtils.executeOpenCmsScript( openCmsWebInfDir, appServerBaseDir,
					openCmsServetMapping, openCmsWebappName, "opencms/> ", uninstallScript);
		}
		catch (IOException e)
		{
			throw new MojoExecutionException(
					"Error generando el script de instalación", e);
		}

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

