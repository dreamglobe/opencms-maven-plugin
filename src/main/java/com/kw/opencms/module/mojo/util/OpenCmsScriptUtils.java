package com.kw.opencms.module.mojo.util;


import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.util.List;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.tools.ant.BuildException;
import org.codehaus.classworlds.ClassRealm;
import org.codehaus.classworlds.ClassWorld;
import org.codehaus.classworlds.DuplicateRealmException;
import org.codehaus.classworlds.NoSuchRealmException;

import com.kw.opencms.module.mojo.OpenCmsModuleDesc;

/**
 *
 * @author joseangel
 *
 */
public class OpenCmsScriptUtils
{
	private static final String DIR_LIB = "lib";

	private static final String REALM_CHILD_OC_NAME = "servletLib_and_OpenCms";

	private static final String REALM_PLUGIN_NAME = "plugin.opencms.maven";

	private static final String METHOD_START = "start";

//	private static final Class[] constructor_parameters =
//		new Class[]{String.class, String.class, String.class, String.class, org.opencms.main.I_CmsShellCommands.class};
//		String webInfPath, String servletMapping, String defaultWebAppName, String prompt, I_CmsShellCommands additionalShellCommands
//    Constructor cmsShellContructor = cmsShellClazz.getConstructor(constructor_parameters);

	/**  */
	private static final Class<?>[] start_parameters = new Class[]{FileInputStream.class};

	/**  */
	private static final ClassWorld world = new ClassWorld();

	public static Log log;


	/**
	 *
	 * @param openCmsWebDir
	 * @param appServerBaseDir
	 * @param openCmsServeltMapping
	 * @param openCmsWebappName
	 * @param prompt
	 * @param installScript
	 * @throws MojoExecutionException
	 */
	public static void executeOpenCmsScript(File openCmsWebDir, File appServerBaseDir,
			String openCmsServeltMapping, String openCmsWebappName, String prompt,
			File installScript)
		throws MojoExecutionException
	{

		// recoge la definición de la clase shell de OpenCms
		Class<?> cmsShellClazz = getOpenCmsShellClass(openCmsWebDir, appServerBaseDir);

		try {
			Constructor<?> cmsShellContructor = cmsShellClazz.getConstructors()[0];
			Object shell = cmsShellContructor.newInstance(openCmsWebDir.toString(),
					openCmsServeltMapping,
					openCmsWebappName,
					prompt, null);
			Method startShell = cmsShellClazz.getDeclaredMethod(METHOD_START, start_parameters);
			startShell.invoke( shell, new FileInputStream( installScript ));
		}
		catch (Exception e)
		{
			// errores en la reflexión -- problema de versiones
			throw new MojoExecutionException(
					"Error durante la invocación del script OpenCms", e );
		}
	}

	/**
	 *
	 * @param moduleName
	 * @param username
	 * @param password
	 * @return
	 * @throws IOException
	 */
	public static File buildUninstallScript(String moduleName,
			String username, String password)
		throws IOException
	{
		StringBuilder sb = new StringBuilder();
		loginCommand(username, password, sb);
		deleteModuleCommand( moduleName, sb );
		exitCommand(sb);

		return createTempFile(moduleName, sb.toString(),
				"uninstallModule_", ".ocsh");
	}

	private static void exitCommand(StringBuilder sb) {
		sb.append("exit\n");
	}

	/**
	 *
	 * @param moduleName
	 * @param moduleFile
	 * @param username
	 * @param password
	 * @param addUninstall
	 * @return
	 * @throws IOException
	 */
	public static File buildInstallScript(String moduleName, File moduleFile,
			String username, String password, boolean addUninstall)
		throws IOException
	{
		return buildInstallScript( moduleName, moduleFile,
				username, password, addUninstall, true );
	}

	/**
	 *
	 * @param moduleName
	 * @param moduleFile
	 * @param username
	 * @param password
	 * @param addUninstall
	 * @param doexit
	 * @return
	 * @throws IOException
	 */
	public static File buildInstallScript(String moduleName, File moduleFile,
			String username, String password, boolean addUninstall, boolean doexit)
		throws IOException
	{
		StringBuilder sb = new StringBuilder();
		loginCommand(username, password, sb);

		if (addUninstall) {
			deleteModuleCommand( moduleName, sb );
		}

		importModuleCommand( moduleFile.getName(), sb);

		if( doexit )
		{
			exitCommand(sb);
		}

		return createTempFile(moduleName, sb.toString(),
				"installModule_", ".ocsh");
	}

	/**
	 *
	 * @param modulesToInstall
	 * @param username
	 * @param password
	 * @param addUninstall
	 * @return
	 * @throws IOException
	 */
	public static File buildInstallScript(List<OpenCmsModuleDesc> modulesToInstall,
			String username, String password, boolean addUninstall)
		throws IOException
	{
		return buildInstallScript(modulesToInstall, username, password, addUninstall, true);
	}

	/**
	 *
	 * @param modulesToInstall
	 * @param username
	 * @param password
	 * @param addUninstall
	 * @param doexit
	 * @return
	 * @throws IOException
	 */
	public static File buildInstallScript(List<OpenCmsModuleDesc> modulesToInstall,
			String username, String password, boolean addUninstall, boolean doexit)
		throws IOException
	{
		StringBuilder sb = new StringBuilder();
		loginCommand(username, password, sb);

		for(OpenCmsModuleDesc module : modulesToInstall)
		{
			if((addUninstall && module.isUninstall() == null)
			||	( module.isUninstall() != null && module.isUninstall() ))
			{
				deleteModuleCommand( module.getModuleName(), sb);
			}
		}

		for(OpenCmsModuleDesc module : modulesToInstall)
		{
			if(module.isInstall() == null || module.isInstall())
			{
				importModuleCommand( module.getModuleFile().getName(), sb );
			}
		}

		if( doexit )
		{
			exitCommand(sb);
		}

		return  createTempFile( "", sb.toString() , "multipleModuleInstall_", ".ocsh");
	}

	private static void deleteModuleCommand( String module, StringBuilder sb )
	{
		sb.append( "deleteModule \"").append( correctModuleName(module)).append("\"\n");
	}

	private static Object correctModuleName(String module) {
		return module.replace('-', '_');
	}

	private static void importModuleCommand( String module, StringBuilder sb )
	{
		sb.append("importModuleFromDefault ")
			.append( module ).append("\n");
	}

	private static void loginCommand(String username, String password, StringBuilder sb)
	{
		sb.append( "login ").append(username).append(" ")
			.append(password).append("\n");
	}

	/**
	 *
	 * @param moduleName
	 * @param content
	 * @param prefix
	 * @param suffix
	 * @return
	 * @throws IOException
	 */
	private static File createTempFile(String moduleName, String content,
			String prefix, String suffix) throws IOException
	{
		File script = null;
		FileWriter writer = null;
		try
		{
			script = File.createTempFile( prefix +moduleName + "_", suffix);
			script.deleteOnExit();
			writer = new FileWriter( script );
			writer.write( content.toString());
		}
		finally
		{
			try
			{
				writer.close();
			}
			catch (IOException e)
			{
				e.printStackTrace();
			}
		}
		return script;
	}

	/**
	 *
	 * @param openCmsWebDir directorio base de OpenCms
	 * @param appServerBaseDir directorio padre de la biblioteca de del servidor de aplicaciones
	 * @return la definición de la clase buscada
	 * @throws MojoExecutionException
	 */
	private static Class<?> getOpenCmsShellClass(File openCmsWebDir,
			File appServerBaseDir) throws MojoExecutionException
	{
		// create the classes realms for OpenCms
		getOrCreateClassRealm(REALM_PLUGIN_NAME);
		ClassRealm ocRealm = getOrCreateClassRealm(REALM_CHILD_OC_NAME, REALM_PLUGIN_NAME);
		try
		{
			// make the child realm the ContextClassLoader
			Thread.currentThread().setContextClassLoader( ocRealm.getClassLoader());
			// create shell class definition from the realm
			return ocRealm.loadClass( "org.opencms.main.CmsShell" );
		}
		catch ( ClassNotFoundException e )
		{
			File appServerLibDir = new File( appServerBaseDir, DIR_LIB);
			if(!appServerLibDir.exists())
				throw new BuildException("Server library dir does not exist! Check appServerBaseDir property: " + appServerBaseDir);

			File opencmsLibDir = new File( openCmsWebDir, DIR_LIB);
			if(!opencmsLibDir.exists())
				throw new BuildException("OpenCms library dir does not exist! Check openCmsBaseDir property " + openCmsWebDir);
			
			//add all the jars we just downloaded to the new child realm
			log.info("Cargando bibliotecas de servidor desde "+ appServerBaseDir);
			loadJarInClassRealm(ocRealm, appServerLibDir);

			log.info("Cargando bibliotecas de servidor y OpenCms desde "+ appServerLibDir);
			loadJarInClassRealm(ocRealm, opencmsLibDir);
			
			try 
			{
				return ocRealm.loadClass( "org.opencms.main.CmsShell" );
			}
			catch ( ClassNotFoundException e1)
			{
				throw new MojoExecutionException(
						"No se encuentra la clase \"org.opencms.main.CmsShell\"", e1);
			}
		}
		
	}

	private static void loadJarInClassRealm(ClassRealm ocRealm,
			File appServerLibDir) 
	{
		for (File jar : appServerLibDir.listFiles())
		{
			try
			{
				ocRealm.addConstituent(jar.toURI().toURL());
			}
			catch (MalformedURLException e)
			{ // nunca ocurre
			}
		}
	}

	private static ClassRealm getOrCreateClassRealm(String classLoaderName) throws MojoExecutionException 
	{
		return getOrCreateClassRealm(classLoaderName, null);
	}
	
	private static ClassRealm getOrCreateClassRealm(String classRealmName, String parentClassRealmName) throws MojoExecutionException 
	{
		try
		{
			// lookup
			if(existsRealm(classRealmName)) 
			{
				return world.getRealm(classRealmName);
			}
			// creation
			if(existsRealm(parentClassRealmName)) 
			{
				return world.getRealm(parentClassRealmName).createChildRealm(classRealmName);
			} else 
			{
				return world.newRealm(classRealmName,Thread.currentThread().getContextClassLoader());
			}
		}
		catch (NoSuchRealmException e)
		{
			throw new MojoExecutionException("Error inicializando los Classloaders ", e);
		}
		catch (DuplicateRealmException e1)
		{
			throw new MojoExecutionException("Error inicializando los Classloaders ", e1);
		}
	}
	
	@SuppressWarnings("unchecked")
	private static ClassRealm findRealm(String realmName) 
	{
		for(ClassRealm realm : ((Iterable<ClassRealm>)(world.getRealms()))) 
		{
			if(realm.getId().equals(realmName)) 
			{
				return realm;
			}
		}
		return null;
	}
	
	private static boolean existsRealm(String realmName) 
	{
		return realmName!=null && findRealm(realmName)!=null;
	}
}
