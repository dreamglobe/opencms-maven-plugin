package com.kw.opencms.module.mojo.packaging;


import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.Properties;
import java.util.Set;

import org.antlr.stringtemplate.StringTemplate;
import org.antlr.stringtemplate.StringTemplateErrorListener;
import org.antlr.stringtemplate.StringTemplateGroup;
import org.antlr.stringtemplate.language.DefaultTemplateLexer;
import org.apache.maven.BuildFailureException;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

import com.kw.opencms.module.mojo.ModuleResource;
import com.kw.opencms.module.mojo.util.CmsUUID;
import com.kw.opencms.module.mojo.util.ManifestBean;
import com.kw.opencms.module.mojo.util.ManifestUtils;
import com.kw.opencms.module.mojo.util.ManifestBean.CategoryBean;
import com.kw.opencms.module.mojo.util.ManifestBean.Filetype;
import com.kw.opencms.module.mojo.util.ManifestBean.PermissionSet;
import com.kw.opencms.module.mojo.util.ManifestBean.ResourceFileBean;

public class ManifestGenerationTask extends AbstractModulePackagingTask {

	private static final Map<String, String> descriptorFilePropertyMap;

	private static final String PROPERTIES_DIR_NAME = "__properties";

	private static final String PREFIX_DIR = "__";

	private static final String PROPERTIES_EXT = ".properties";

	private static final String ACCESSCONTROL_EXT = ".acl";
    static {
        Map<String, String> descriptorFiles = new HashMap<String, String>();
        descriptorFiles.put("accounts.xml", 		"manifest.accounts.accounts_str");
        descriptorFiles.put("dependencies.xml", 	"manifest.module.dependencies_str");
        descriptorFiles.put("exportpoints.xml", 	"manifest.module.exportpoints_str");
        descriptorFiles.put("explorertypes.xml", 	"manifest.module.explorertypes_str");
        descriptorFiles.put("parameters.xml", 		"manifest.module.parameters_str");
        descriptorFiles.put("resources.xml", 		"manifest.module.resources_str");
        descriptorFiles.put("resourcetypes.xml", 	"manifest.module.resourcetypes_str");
        descriptorFilePropertyMap = Collections.unmodifiableMap(descriptorFiles);
    }

	private Map<String, String> properties ;
	private Map<String, ResourceFileBean> resourcesByRelativePath = new HashMap<String, ResourceFileBean>();
	private Map<String, ResourceFileBean> categoryByPath = new HashMap<String, ResourceFileBean>();
	private List<ResourceFileBean> siblingsSet = new ArrayList<ResourceFileBean>();
	private Map<String, String> destinationsPath = new HashMap<String, String>();
	private ModulePackagingContext context;
	private int propCounter = 0;
	final private static String module_info = "module.properties"; // properties filename for module part

	public void performPackaging(final ModulePackagingContext context)
			throws MojoExecutionException, MojoFailureException {

		this.context = context;

		try {
			// get module properties from descriptors
			properties = getManifestProperties( context );
			ManifestBean manifestBean = new ManifestBean( properties );

			// List fileBeans =
			buildModuleFileBeans( context, manifestBean );

			// second pass for completing the siblings not resolved
			secondPassSibling(siblingsSet, manifestBean);

			// apply template to properties
			String manifest = applyTemplate(context, manifestBean);

			writeFile( context, manifest );
		}
		catch (IOException e)
		{
			throw new MojoExecutionException( "Error while witring manifest file ", e );
		}

	}

	public Map<String, String> getManifestProperties(ModulePackagingContext context)
		throws MojoFailureException {

		File descriptorDir = new File( context.getWorkDirectory(), "manifest" );
		if( descriptorDir == null || !descriptorDir.exists() )
		{
			throw new MojoFailureException( "No module manifest descriptors directory found! ");
		}

		// Module properties group, name, version, etc
		Map<String, String> propertiesMap = new HashMap<String, String>();
		String 	resourcesPath = descriptorDir.getAbsolutePath().concat(File.separator);
		try
		{
			File modulePropsFile = new File(resourcesPath.concat(module_info));
			Properties moduleProp = new Properties();
			moduleProp.load(new FileInputStream(modulePropsFile));
			propertiesMap.putAll((Map)moduleProp);
		}
		catch ( FileNotFoundException e )
		{
			throw new MojoFailureException( "Module property file not found!", e );
		}
		catch ( IOException e )
		{
			throw new MojoFailureException( "Error reading module properties", e );
		}

		// Agrega las propiedades leidas a las propiedades del proyecto
		context.getProject().getProperties().putAll(propertiesMap);

		// Manifest specifics parts
		for( String filename : descriptorFilePropertyMap.keySet() )
		{
			String content = ManifestUtils.readFileAsStringNoException(
					resourcesPath.concat( filename ) );

			String property = descriptorFilePropertyMap.get( filename );
			propertiesMap.put( property, content);
		}

		return propertiesMap;
	}

	/**
	 * Builds the beans for every file selected and the directories under they sit.
	 *
	 * @param fileLocations collection of locations to add to the module.
	 * @param manifestBean for building paths inside opencms system.
	 * @return list of resource file beans
	 * @throws IOException
	 * @throws MojoFailureException
	 */
	protected List<ManifestBean.ResourceFileBean> buildModuleFileBeans(
			ModulePackagingContext context, ManifestBean manifestBean) throws IOException, MojoFailureException
	{
		// Register default and custom module resources locations
		ModuleResource[] module_resources = context.getModuleResources();
		if(module_resources == null)
		{
			module_resources = new ModuleResource[0];
		}
		List<ModuleResource> fileLocations = new ArrayList<ModuleResource>(module_resources.length + 3);
		fileLocations.add(context.getModuleSourceResource());
		if(!context.archiveClasses())
		{
			fileLocations.add(context.getClassesResource());
		}
		fileLocations.add(context.getLibResource());
		fileLocations.addAll(Arrays.asList(module_resources));

		// Estimates 50 files per location
		List<ManifestBean.ResourceFileBean> resourceBeanList =
			new ArrayList<ManifestBean.ResourceFileBean>(
					fileLocations.size() * 50);

		// Main location loop
		for( ModuleResource location : fileLocations )
		{
			if(! new File(location.getDirectory()).exists())
			{
				continue;
			}

			// extracts the relative path
			calculateModuleDestinationPath(manifestBean, location);

			// register folder resources
			String[] resources = this.getFilesAndDirectoriesToIncludes( location );

			for( String resourcePath : resources )
			{
				// build resource bean from file and properties
				ResourceFileBean bean = buildResourceFileBean(
						context, location, resourcePath.replace( '\\', '/' ));

				// updates resource bean list or unresolved sibling list
				if(bean != null)
				{
					if( bean.getSource() != null
						&& ( Filetype.folder.name().equals( bean.getType() )
						|| Filetype.sibling.name().equals( bean.getType() ) ) )
					{
						// add the bean for second pass process
						siblingsSet.add( bean );
					}
					else
					{
						resourceBeanList.add( bean );
					}
				}
			}
		}
		// updates manifestBean with the resources bean list
		manifestBean.setFiles( resourceBeanList );

		return resourceBeanList;
	}

	/**
	 *
	 * @param manifestBean
	 * @param location
	 * @return
	 */
	private String calculateModuleDestinationPath(ManifestBean manifestBean,
			ModuleResource location)
	{
		String moduleTargetPath = location.getModuleTargetPath();
		String destinationPath = location.isSystemModule()
			? "system/modules/" + manifestBean.getModule().name + "/"
			: "";

		if (moduleTargetPath != null)
		{
			destinationPath += moduleTargetPath.replace('\\', '/');
			if( !moduleTargetPath.endsWith("/") )
			{
				destinationPath += "/";
			}
		}
		location.setModuleTargetPath( destinationPath );
		return destinationPath;
	}

	/**
	 *
	 * @param file
	 * @param props
	 * @param base
	 * @param destinationPath
	 * @return
	 * @throws IOException
	 * @throws BuildFailureException
	 */
	protected ResourceFileBean buildResourceFileBean(
			ModulePackagingContext context, ModuleResource moduleResource,
			String resourcePath ) throws IOException, MojoFailureException
	{
		if( context.getLog().isDebugEnabled() )
		{
			context.getLog().debug("Creating Manifest entry for file: " + resourcePath );
		}

		ResourceFileBean bean = new ResourceFileBean();

		// Get Properties
		String moduleResourcePath = getModuleResourcePath( moduleResource, resourcePath );
		File resourceFile = new File(context.getModuleDirectory(), moduleResourcePath );

		Properties props = lookForProperties( resourceFile );
		if( context.getLog().isDebugEnabled() )
		{
			context.getLog().debug("\tProperties: " + props );
		}

		// Destination
		String destination = calculateResourceDestinationPath(props,
				resourcePath, moduleResource.getModuleTargetPath(), resourceFile.isDirectory());
		bean.setDestination( destination );

		// checks duplicate files
		if( resourcesByRelativePath.containsKey(destination))
		{
			// get properties for system module directory from main module directory properties  
			if(!resourcePath.isEmpty())
				context.getLog().warn("Duplicated resource wont be included: "
					+ resourceFile.toString() + " to " + destination );
			return null;
		}

		// Get ACL Properties
		Properties aclProperties = lookForAclProperties( resourceFile );
		if( context.getLog().isDebugEnabled() )
		{
			context.getLog().debug("\tACL properties: " + aclProperties );
		}

		Filetype type = calculateBeanType(resourceFile, props);
		bean.setType( Filetype.custom.equals( type )
				? type.getCustonName()
				: type.name() );

		if( context.getLog().isDebugEnabled() )
		{
			context.getLog().debug("\tType: " + bean.getType() );
		}

		// Calculate CmsUUID
		fillUUIDs(bean, type, resourceFile.isDirectory());

		// Source
		if(!resourceFile.isDirectory())
		{
			bean.setSource(moduleResourcePath.startsWith( "/" )
				? moduleResourcePath.substring( 1 ) // remove initial slash
				: moduleResourcePath );
		}

		// check for categories
		if( Filetype.folder.equals(type)
			&& destination.contains("_categories/") )
		{
			categoryByPath.put("/" + bean.getDestination() + "/", bean);
		}


		// Common properties
		if( Filetype.sibling.equals( type ) )
		{
			resolveSibling( resourceFile, props, bean );
		}
		else
		{
			fillCommonBeanProperties( resourceFile, props, bean, type, destination );
		}

		// Properties
		Map<String, String> vfsProperties = new HashMap<String,String>(props.size()),
							sharedProperties = new HashMap<String,String>(props.size());
		extractVfsProperties(props, sharedProperties, vfsProperties);
		bean.setProperties( vfsProperties.entrySet() );
		bean.setSharedProperties( sharedProperties.entrySet() );

		// Categories
		bean.setRelations(extractRelations(vfsProperties));
		bean.getRelations().addAll(extractRelations(sharedProperties));

		// AccessControl
		bean.setAcl(extractAclProperties(aclProperties, resourcePath).entrySet() );

		// add to relative path
		resourcesByRelativePath.put( destination, bean );

		return bean;
	}

	protected String calculateResourceDestinationPath(Properties props,
			String resourcePath, String moduleTargetPath, boolean isDir) 
	{
		String destination = null; 
		int posName = resourcePath.lastIndexOf('/');
		String resourceName = posName!=-1 
			? resourcePath.substring(posName+1)
			: resourcePath; 
		if( props.containsKey( "manifest.destination.i" ) 
			&& !((String)props.get( "manifest.destination.i" )).isEmpty())
		{
			String destProp = (String)props.get( "manifest.destination.i" );
			destination = destProp.endsWith("/")
				? destProp.concat(resourceName)
				: destProp ;
			
			if(isDir)
			{
				destinationsPath.put(resourcePath, destination);
			}
		}
		else
		{
			//checks if an ancestor has destination.i
			boolean found = false;
			while(posName != -1 )
			{
				String pathName = resourcePath.substring(0, posName);
				String middlePath = resourcePath.substring(posName);
				if(destinationsPath.containsKey(pathName))
				{
					destination = ( destinationsPath.get( pathName ))
						.concat(middlePath);
					found = true;
					if(isDir)
					{
						destinationsPath.put(resourcePath, destination);
					}
					break;
				}
				
				posName = pathName.lastIndexOf('/');
			}
			
			if(!found)
			{
				destination = moduleTargetPath.concat(resourcePath);
			}
			
		}
		
		if( destination.startsWith("/") )
		{
				 destination.substring(1);
		}
		return destination;
	}

	private String getModuleResourcePath(ModuleResource moduleResource,
			String resourcePath)
	{
		if( !File.separator.equals("/") )
		{
			resourcePath.replace( File.separator, "/" );
			if(moduleResource.getTargetPath() != null)
			{
				moduleResource.setTargetPath(
						moduleResource.getTargetPath()
							.replace( File.separator, "/" ));
			}
		}

		String moduleResourcePath = moduleResource.getTargetPath() != null
				&& !moduleResource.getTargetPath().isEmpty()
				&& !moduleResource.getTargetPath().equals(".")
				&& !moduleResource.getTargetPath().equals("./")
			? moduleResource.getTargetPath() + "/" + resourcePath
			: resourcePath;
		return moduleResourcePath;
	}

	protected Map<String,PermissionSet> extractAclProperties(Properties properties, String path)
	{
		Map<String,PermissionSet> map = new HashMap<String,PermissionSet>(properties.size());
		Iterator it = properties.keySet().iterator();
		while(it.hasNext())
		{
			String key = (String)it.next();
			if(key.contains("#"))
			{
				continue;
			}
			String values = (String)properties.get(key);
			String permission;
			String flags;
			if(values.startsWith("/") || values.length()==0)
			{
				permission = "";
				flags = values.substring(1);
			}
			else if(values.contains("/"))
			{
				String[] splited = values.split("/");
				permission = splited[0]!=null?splited[0]:"";
				flags = splited[1]!=null?splited[1]:"0";
			}
			else
			{
				permission = values;
				flags = "0";
			}
			try
			{
				map.put(key, new PermissionSet(permission,flags));
			}
			catch (NoSuchElementException e){
				context.getLog().warn(String.format("ACL Definition error on %s: [%s,%s]",path, key, values) );
			}
		}
		return map;
	}

	protected Set<CategoryBean> extractRelations(Map<String, String> properties)
	{
		// TOADD: system wide system categories done in second pass
		// Only content relative categories
		Set<CategoryBean> categories = new HashSet<CategoryBean>();
		for(Entry<String,String> entry: properties.entrySet())
		{
			String key = entry.getKey();
			if( (key.contains("category") || key.contains("categories"))
				&& categoryByPath.containsKey(entry.getValue()))
			{
				ResourceFileBean categoryBean = categoryByPath.get(entry.getValue());
				CategoryBean category = new CategoryBean();
				category.setId(categoryBean.getUuidstructure());
				category.setPath(entry.getValue());
				categories.add(category);
			}
		}
		return categories;
	}

	protected void extractVfsProperties(Properties properties, Map<String,String> shared, Map<String,String> individual)
	{
		Iterator it = properties.keySet().iterator();
		while(it.hasNext())
		{
			String key = (String)it.next();
			String value = (String)properties.get(key);
			if(value.trim().length()>0 && !key.contains("#")){ // Bug reading non ISO-8859-1
				if(key.endsWith(".s")){
					shared.put(key.substring(0, key.length()-2), value);
				} else if(key.endsWith(".i")){
					individual.put(key.substring(0, key.length()-2), value);
				} else { // add the custom properties to the manifest
					individual.put(key, value);
				}
			}
		}
	}

	private void fillUUIDs(ResourceFileBean bean, Filetype type, boolean isDirectory) throws MojoFailureException
	{
		switch( type )
		{
		case binary:
		case image:
		case jsp:
		case plain:
		case custom:
			if( !isDirectory )
			{
				bean.setUuidresource( new CmsUUID().getStringValue() );
			}
		case folder:
		case downloadgallery:
		case imagegallery:
		case linkgallery:
		case sibling:
		default:
			bean.setUuidstructure( new CmsUUID().getStringValue() );
		}
	}

	private void fillCommonBeanProperties(File file, Properties props,
			ResourceFileBean bean, Filetype type, String destination)
	{
		// Dates
		String defaultDate = ManifestUtils.capitalizeFirstLettersTokenizer(
				ManifestBean.dateFormat.format( new Date(file.lastModified() )));

		bean.setDatecreated( props.containsKey( "manifest.datecreated.i" )
				? (String) props.get( "manifest.datecreated.i" )
				: defaultDate );
		bean.setDatelastmodified(props.containsKey( "manifest.datelastmodified.i")
				? (String) props.get( "manifest.datelastmodified.i")
				: defaultDate);

		// Users
		bean.setUsercreated(
				props.containsKey("manifest.usercreated.i")
					? (String)props.get("manifest.usercreated.i")
					: "Admin");
		bean.setUserlastmodified(
				props.containsKey("manifest.userlastmodified.i")
					? (String)props.get("manifest.userlastmodified.i")
					: "Admin");
	}

	private void resolveSibling(File file, Properties props,
			ResourceFileBean bean)
	{
		try
		{
			String siblingPath;
			if(props.containsKey("sibling.path.i")){
				siblingPath = props.getProperty("sibling.path.i");
			}
			else
			{
				siblingPath = ManifestUtils.readFileAsString( file.getAbsolutePath() );
			}
			if(siblingPath.indexOf( '\\' ) != -1)
			{
				siblingPath.replace( '\\', '/' );
			}
			if(siblingPath.startsWith("/"))
			{
				siblingPath = siblingPath.substring(1);
			}
			bean.setSource(siblingPath);
			props.put("sibling.path.i", siblingPath );
		}
		catch ( FileNotFoundException e )
		{
			e.printStackTrace();
		}
		catch ( IOException e )
		{
			e.printStackTrace();
		}

		// add the type property
		if(!props.containsKey("manifest.type.i") || props.get("manifest.type.i").equals("sibling"))
		{
			props.put("manifest.type.i", "sibling");
		}

		// check if the sibling is already defined
		if( resourcesByRelativePath.containsKey( bean.getSource() ))
		{
			buildSibling(resourcesByRelativePath.get( bean.getSource() ), bean);
		}
	}

	protected Filetype calculateBeanType( File file, Properties props )
	{
		String typeStr = ManifestUtils.isNotNullOrZero(
					props.get( "manifest.type.i" ))
				? (String) props.get( "manifest.type.i" )
				: (String) props.get( "manifest.type.s" );

		Filetype type = ManifestUtils.isNotNullOrZero( typeStr )
			? Filetype.newCustomFiletype( typeStr )
			: file.isDirectory()
				? Filetype.folder
				: Filetype.calculateType( file );

		return type;
	}

	protected void buildSibling(ResourceFileBean source, ResourceFileBean target)
	{
		target.setDatecreated(source.getDatecreated());
		target.setDatelastmodified(source.getDatelastmodified());
		target.setUsercreated(source.getUsercreated());
		target.setUserlastmodified(source.getUserlastmodified());
		String destination = target.getDestination();
		target.setDestination(
				destination.endsWith(".sibling")
				? destination.substring(0, destination.length()-".sibling".length())
				: destination );

		target.setUuidresource(source.getUuidresource());
		target.setType(source.getType());
		target.setSource(null);
	}

	protected Properties lookForProperties( File resource )
		throws IOException
	{
		String propFilename = resource.getParent()
				.concat(File.separator).concat(PROPERTIES_DIR_NAME)
				.concat(File.separator).concat(resource.isDirectory()?PREFIX_DIR:"")
				.concat(resource.getName()).concat(PROPERTIES_EXT);

		File propFile = new File(propFilename);
		Properties props = new Properties();
		InputStream in = null;
		try
		{
			in = new FileInputStream(propFile);
			props.load(in);
			propCounter++;
		}
		catch ( FileNotFoundException e )
		{
			// Doesn't have to have a properties file
		}
		finally
		{
			if (in != null)
				in.close();
		}
		return props;

	}

	protected Properties lookForAclProperties( File resource)
		throws IOException
	{
		String propFilename = resource.getParent()
				.concat(File.separator).concat(PROPERTIES_DIR_NAME)
				.concat(File.separator).concat(resource.isDirectory()
							?PREFIX_DIR+resource.getName()
							:resource.getName())
				.concat(ACCESSCONTROL_EXT);

		File propFile = new File(propFilename);
		Properties prop = new Properties();
		InputStream in = null;
		try
		{
			in = new FileInputStream(propFile);
			prop.load(in);
			propCounter++;
		}
		catch ( FileNotFoundException e )
		{
			// Doesn't have to have a properties file
		}
		finally
		{
			if (in != null)
				in.close();
		}
		return prop;
	}

	protected static Map<String, String> extractProperties(Properties prop)
	{
		assert(prop!=null);

		HashMap<String,String> map = new HashMap<String,String>();

		for(Entry<Object,Object> entry : prop.entrySet())
		{
			if(entry.getValue().toString().trim().length()>0
					&& !entry.getKey().toString().contains("#"))// Bug reading non ISO-8859-1
			{
				map.put((String)entry.getKey(),(String)entry.getValue());
			}
		}
		return map;
	}

	public static void printProperties(Properties prop)
	{
		if(prop!=null){
			prop.list(System.out);
		}
	}

	protected void secondPassSibling(List<ResourceFileBean> siblingsSet, ManifestBean manifestBean)
	{
		for( ResourceFileBean sibling : siblingsSet )
		{
			if( resourcesByRelativePath.containsKey( sibling.getSource() ) )
			{
				buildSibling( resourcesByRelativePath.get(sibling.getSource() ), sibling );
				manifestBean.getFiles().add( sibling );
			}
			else
			{
				this.context.getLog().warn("Sibling bean [" + sibling.getDestination()
						+ "] source not found [" + sibling.getSource() + "]. Won't be added!");
			}
		}
	}

	protected String applyTemplate(final ModulePackagingContext context,
			ManifestBean manifestBean) {
		InputStream is = ManifestGenerationTask.class.getResourceAsStream( "/manifest.stg" );
		StringTemplateGroup stg = new StringTemplateGroup( new InputStreamReader( is ), DefaultTemplateLexer.class );
		stg.setErrorListener(new StringTemplateErrorListener(){
			public void error(String msg, Throwable e)
			{
				context.getLog().error( msg, e );
			}
			public void warning( String msg )
			{
				context.getLog().warn( msg );
			}
		});

		String manifest = applyTemplate(
				stg.getInstanceOf("manifest"), manifestBean,
				context.getManifestEncoding(), null);
		return manifest;
	}

	protected String applyTemplate(StringTemplate template, Object data,
			String encoding, String xmlversion){

		template.setAttribute("export", data);

		if(encoding!=null)
		{
			template.setAttribute("encoding", encoding);
		}

		if(xmlversion!=null)
		{
			template.setAttribute("xmlversion", xmlversion);
		}

		return template.toString();
	}

	protected void writeFile( ModulePackagingContext context, String manifest) throws IOException{
		File manifestFile = new File( context.getModuleDirectory(), "manifest.xml" );
		if( manifestFile.exists() )
		{
			manifestFile.delete();
		}
		FileWriter writer = null;
		try
		{
			writer = new FileWriter( manifestFile );
			writer.write( manifest );
		}
		finally
		{
			if( writer != null)
			{
				writer.close();
			}
		}
	}

}
