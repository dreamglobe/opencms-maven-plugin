package com.kw.opencms.module.mojo;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.apache.maven.archiver.MavenArchiveConfiguration;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.apache.maven.settings.Server;
import org.apache.maven.settings.Settings;
import org.apache.maven.shared.filtering.MavenFileFilter;
import org.apache.maven.shared.filtering.MavenFilteringException;
import org.apache.maven.shared.filtering.MavenResourcesExecution;
import org.apache.maven.shared.filtering.MavenResourcesFiltering;
import org.codehaus.plexus.archiver.jar.JarArchiver;
import org.codehaus.plexus.archiver.manager.ArchiverManager;
import org.codehaus.plexus.util.StringUtils;

import com.kw.opencms.module.mojo.packaging.ModulePackagingContext;
import com.kw.opencms.module.mojo.packaging.ModulePackagingTask;
import com.kw.opencms.module.mojo.packaging.ModulePostPackagingTask;
import com.kw.opencms.module.mojo.packaging.ModuleProjectPackagingTask;
import com.kw.opencms.module.mojo.packaging.SaveModuleStructurePostPackagingTask;
import com.kw.opencms.module.mojo.util.ModuleStructure;
import com.kw.opencms.module.mojo.util.ModuleStructureSerializer;

public abstract class AbstractModuleMojo extends AbstractMojo {

	public static final String DEFAULT_FILE_NAME_MAPPING = "@{artifactId}@-@{version}@.@{extension}@";

    public static final String DEFAULT_FILE_NAME_MAPPING_CLASSIFIER =
        "@{artifactId}@-@{version}@-@{classifier}@.@{extension}@";

	private static final String[] EMPTY_STRING_ARRAY = {};

    protected static final String MANIFEST_NAME = "manifest.xml";

	public static final String PACKAGING_OPENCMS_MODULE = "opencms-module";
	public static final String POM_PACKAGING = "pom";

    /**
     * The maven project.
     *
     * @parameter property="project"
     * @required
     * @readonly
     */
    private MavenProject project;

    /**
     * The directory containing generated classes.
     *
     * @parameter property="project.build.outputDirectory"
     * @required
     * @readonly
     */
    private File classesDirectory;

	/**
	 * Whether classes (module/classes or lib directory) should be attached to the project.
	 * @parameter default-value="true"
	 */
	private boolean attachClasses = true;
    
    /**
     * Whether a JAR file will be created for the classes in the module. Using this optional configuration
     * parameter will make the generated classes to be archived into a jar file
     * and the classes directory will then be excluded from the module.
     *
     * @parameter property="archiveClasses" default-value="false"
     */
    private boolean archiveClasses;

    /**
     * The Jar archiver needed for archiving classes directory into jar file under WEB-INF/lib.
     *
     * @component role="org.codehaus.plexus.archiver.Archiver" role-hint="jar"
     * @required
     */
    private JarArchiver jarArchiver;

    /**
     * The directory where the module is built.
     *
     * @parameter property="moduleDirectory" default-value="${project.build.directory}/${project.build.finalName}"
     * @required
     */
    private File moduleDirectory;

    /**
     * Single directory for extra files to include in the module.
     *
     * @parameter property="moduleSourceDirectory" default-value="${basedir}/src/main/module"
     * @required
     */
    private File moduleSourceDirectory;


    /**
     * Relative path where source directory will be placed in the module.
     *
     * @parameter
     */
    private String moduleSourceTargetDirectory;

    /**
     * The list of webResources we want to transfer.
     *
     * @parameter
     */
    private ModuleResource[] moduleResources;

    /**
     * Filters (property files) to include during the interpolation of the pom.xml.
     *
     * @parameter
     */
    private List filters;

    /**
     * The path to the web.xml file to use.
     *
     * @parameter property="maven.module.manifestxml"
     */
    private File manifestXml;

    /**
     * Whether to generate the manifest if it doent exist
     *
     * @parameter property="generate.manifest.xml" default-value="false"
     */
    private boolean generateManifestXml = false;

    /**
     * Charset to encode the generated module manifest;
     *
     * @parameter property="manifest.encoding"
     */
    private String manifestEncoding;

    /**
     *
     * @parameter property="module.descriptors.dir" default-value="${basedir}/src/main/manifest"
     */
    private File descriptorsDir;

    /**
     *
     * @parameter property="module.descriptors.encoding" default-value="default"
     */
    private String descriptorsEncoding;

    /**
     *
     * @parameter property="module.descriptors.n2aapply" default-value="false"
     */
    private boolean descriptorsN2AApply;

    /**
     * @parameter
     */
    private PlainEncodingConfig descriptorsN2AConfig;

    /**
     * Directory to copy conversed to native resources into if needed
     *
     * @parameter property="workDirectory" default-value="${project.build.directory}/module/work"
     * @required
     */
    private File workDirectory;

    /**
     * The file name mapping to use to copy libraries and tlds. If no file mapping is
     * set (default) the file is copied with its standard name.
     *
     * @parameter
     * @since 2.1-alpha-1
     */
    private String outputFileNameMapping;

    /**
     * The file containing the module structure cache.
     *
     * @parameter property="cacheFile" default-value="${project.build.directory}/module/work/webapp-cache.xml"
     * @required
     */
    private File cacheFile;

    /**
     * Whether the cache should be used to save the status of the module
     * accross multiple runs.
     *
     * @parameter property="useCache" default-value="true"
     * @since 2.1-alpha-1
     */
    private boolean useCache = true;

    /**
     * @component role="org.apache.maven.artifact.factory.ArtifactFactory"
     * @required
     * @readonly
     */
    private ArtifactFactory artifactFactory;

    /**
     * To look up Archiver/UnArchiver implementations
     *
     * @component role="org.codehaus.plexus.archiver.manager.ArchiverManager"
     * @required
     */
    private ArchiverManager archiverManager;

    /**
     *
     * @component role="org.apache.maven.shared.filtering.MavenFileFilter" role-hint="default"
     * @required
     */
    private MavenFileFilter mavenFileFilter;

    /**
     *
     * @component role="org.apache.maven.shared.filtering.MavenResourcesFiltering" role-hint="default"
     * @required
     */
    private MavenResourcesFiltering mavenResourcesFiltering;

    /**
     * The comma separated list of tokens to include when copying content
     * of the moduleSourceDirectory. Default is '**'.
     *
     * @parameter alias="includes"
     */
    private String moduleSourceIncludes = "**";

    /**
     * The comma separated list of tokens to exclude when copying content
     * of the moduleSourceDirectory.
     *
     *
     * @parameter alias="excludes"
     */
    private String moduleSourceExcludes;


    /**
     * The comma separated list of tokens to exclude when copying content
     * of the moduleSourceDirectory.
     *
     *
     * @parameter alias="encoding"
     */
    private String moduleSourceEncoding;

    /**
     * A list of file extensions to not filtering.
     * <b>will be used for webResources and overlay filtering</b>
     *
     * @parameter
     */
    private List nonFilteredFileExtensions;

    /**
     * @parameter property="session"
     * @readonly
     * @required
     */
    private MavenSession session;

    /**
     * To filtering deployment descriptors <b>disabled by default</b>
     *
     * @parameter property="maven.module.filteringManifestDescriptors" default-value="false"
     */
    private boolean filteringDeploymentDescriptors = false;

    /**
     * To escape interpolated value with windows path
     * c:\foo\bar will be replaced with c:\\foo\\bar
     * @parameter property="maven.module.escapedBackslashesInFilePath" default-value="false"
     */
    private boolean escapedBackslashesInFilePath = false;

    /**
     * Expression preceded with the String won't be interpolated
     * \${foo} will be replaced with ${foo}
     * @parameter property="maven.module.escapeString"
     */
    protected String escapeString;

    /**
     * The archive configuration to use.
     * See <a href="http://maven.apache.org/shared/maven-archiver/index.html">Maven Archiver Reference</a>.
     *
     * @parameter
     */
    private MavenArchiveConfiguration archive = new MavenArchiveConfiguration();

	/**
	 * @parameter default-value="${settings}"
	 * @readonly
	 */
	private Settings settings;

	/**
	 * @parameter property="dryrun" default-value="false"
	 */
	private boolean dryRun = false;


    private final ModuleStructureSerializer moduleStructureSerialier = new ModuleStructureSerializer();

	/**
	 * Name of the webapp aplication name for OpenCms.
	 * @parameter property="opencms.webapp.name" defaults=""
	 */
	protected String openCmsWebappName;

    /**
     * Returns a string array of the excludes to be used
     * when copying the content of the module source directory.
     *
     * @return an array of tokens to exclude
     */
    protected String[] getExcludes()
    {
        List excludeList = new ArrayList();
        if ( StringUtils.isNotEmpty( moduleSourceExcludes ) )
        {
            excludeList.addAll( Arrays.asList( StringUtils.split( moduleSourceExcludes, "," ) ) );
        }

        // if webXML is specified, omit the one in the source directory
        if ( manifestXml != null && StringUtils.isNotEmpty( manifestXml.getName() ) )
        {
            excludeList.add( "**/" + MANIFEST_NAME );
        }

        return (String[]) excludeList.toArray( EMPTY_STRING_ARRAY );
    }

    /**
     * Returns a string array of the includes to be used
     * when assembling/copying the module.
     *
     * @return an array of tokens to include
     */
    protected String[] getIncludes()
    {
        return StringUtils.split( StringUtils.defaultString( moduleSourceIncludes ), "," );
    }

    /**
     *
     * @param serverId
     * @return
     */
    protected Server getCredentialsFromServer(String serverId) {
		Server serverSettings = getSettings() == null
			? null
			: getSettings().getServer( serverId );

		if ( serverSettings != null )
		{
		    getLog().info( "Using authentication information for server: '"
		    		+ serverId + "'." );
		}
		else
		{
		    getLog().warn( "Server authentication entry not found for: '"
		    		+ serverId + "'." );
		}
		return serverSettings;
	}

    /**
     *
     * @param moduleDirectory
     * @throws MojoExecutionException
     * @throws MojoFailureException
     */
    public void buildExplodedModule( File moduleDirectory )
    	throws MojoExecutionException, MojoFailureException
	{
	    moduleDirectory.mkdirs();

	    try
	    {
	        buildModule( project, moduleDirectory );
	    }
	    catch ( IOException e )
	    {
	        throw new MojoExecutionException( "Could not build module", e );
	    }
	}

    /**
     * Builds the module for the specified project with the new packaging task thingy
     * <p/>
     * Classes, libraries and tld files are copied to
     * the <tt>webappDirectory</tt> during this phase.
     *
     * @param project         the maven project
     * @param moduleDirectory the target directory
     * @throws MojoExecutionException if an error occurred while packaging the webapp
     * @throws MojoFailureException   if an unexpected error occurred while packaging the webapp
     * @throws IOException            if an error occurred while copying the files
     */
    public void buildModule( MavenProject project, File moduleDirectory )
        throws MojoExecutionException, MojoFailureException, IOException
    {
        ModuleStructure cache;
        if ( useCache && cacheFile.exists() )
        {
            cache = new ModuleStructure( project.getDependencies(), moduleStructureSerialier.fromXml( cacheFile ) );
        }
        else
        {
            cache = new ModuleStructure( project.getDependencies(), null );
        }

        final long startTime = System.currentTimeMillis();
        getLog().info( "Assembling module [" + project.getArtifactId() + "] in [" + moduleDirectory + "]" );

        List defaultFilterWrappers = null;
        try
        {
            MavenResourcesExecution mavenResourcesExecution = new MavenResourcesExecution();
            mavenResourcesExecution.setEscapeString( escapeString );

            defaultFilterWrappers = mavenFileFilter.getDefaultFilterWrappers(
            		project, filters,
                    escapedBackslashesInFilePath,
                    this.session, mavenResourcesExecution );

        }
        catch ( MavenFilteringException e )
        {
            getLog().error( "fail to build filering wrappers " + e.getMessage() );
            throw new MojoExecutionException( e.getMessage(), e );
        }

        final ModulePackagingContext context = new DefaultModulePackagingContext(
        		moduleDirectory, cache,
        		defaultFilterWrappers,
                getNonFilteredFileExtensions(),
                filteringDeploymentDescriptors,
                this.artifactFactory );

        ModulePackagingTask modulePackagingTask = new ModuleProjectPackagingTask(
        			moduleResources, manifestXml, generateManifestXml);

        modulePackagingTask.performPackaging( context );
        
        // Post packaging
        final List<ModulePostPackagingTask> postPackagingTasks = getPostPackagingTasks();
        final Iterator<ModulePostPackagingTask> it2 = postPackagingTasks.iterator();
        while ( it2.hasNext() )
        {
            ModulePostPackagingTask task = it2.next();
            task.performPostPackaging( context );
        }
        
        getLog().info( "OpenCms Module assembled in [" + ( System.currentTimeMillis() - startTime ) + " msecs]" );
        
    }

    /**
     * Returns a <tt>List</tt> of the {@link org.apache.maven.plugin.ModulePostPackagingTask.packaging.WarPostPackagingTask}
     * instances to invoke to perform the post-packaging.
     *
     * @return the list of post packaging tasks
     */
    private List<ModulePostPackagingTask> getPostPackagingTasks()
    {
        final List<ModulePostPackagingTask> postPackagingTasks = new ArrayList<ModulePostPackagingTask>(1);
        if ( useCache )
        {
            postPackagingTasks.add( new SaveModuleStructurePostPackagingTask( cacheFile ) );
        }
        // TODO add lib scanning to detect duplicates
        return postPackagingTasks;
    }

    /**
     * ModulePackagingContext default implementation
     */
    protected class DefaultModulePackagingContext
        implements ModulePackagingContext
    {

        private final ArtifactFactory artifactFactory;

        private final ModuleStructure moduleStructure;

        private final File moduleDirectory;

        private final List filterWrappers;

        private List nonFilteredFileExtensions;

        private boolean filteringDeploymentDescriptors;

        public DefaultModulePackagingContext()
        {
        	this.moduleDirectory = null;
            this.moduleStructure = null;
            this.filterWrappers = null;
            this.artifactFactory = null;
        }

        public DefaultModulePackagingContext( File moduleDirectory,
			final ModuleStructure moduleStructure,
			List filterWrappers,
            List nonFilteredFileExtensions,
            boolean filteringDeploymentDescriptors,
            ArtifactFactory artifactFactory )
        {
            this.moduleDirectory = moduleDirectory;
            this.moduleStructure = moduleStructure;
            this.filterWrappers = filterWrappers;
            this.artifactFactory = artifactFactory;
            this.filteringDeploymentDescriptors = filteringDeploymentDescriptors;
            this.nonFilteredFileExtensions = nonFilteredFileExtensions == null
            	? Collections.EMPTY_LIST
            	: nonFilteredFileExtensions;
        }

        public MavenProject getProject()
        {
            return project;
        }

        public File getModuleDirectory()
        {
            return moduleDirectory;
        }

        public File getClassesDirectory()
        {
            return classesDirectory;
        }

        public Log getLog()
        {
            return AbstractModuleMojo.this.getLog();
        }

        public String getOutputFileNameMapping()
        {
            return outputFileNameMapping;
        }

        public File getModuleSourceDirectory()
        {
            return moduleSourceDirectory;
        }

        public String getModuleSourceTargetDirectory()
        {
        	return moduleSourceTargetDirectory;
        }

        public String[] getModuleSourceIncludes()
        {
            return getIncludes();
        }

        public String[] getModuleSourceExcludes()
        {
            return getExcludes();
        }

        public boolean archiveClasses()
        {
            return archiveClasses;
        }

        public boolean isAttachClasses() {
    	    return attachClasses;
    	}

        public ArchiverManager getArchiverManager()
        {
            return archiverManager;
        }

        public MavenArchiveConfiguration getArchive()
        {
            return archive;
        }

        public JarArchiver getJarArchiver()
        {
            return jarArchiver;
        }

        public List getFilters()
        {
            return filters;
        }

        public ModuleStructure getModuleStructure()
        {
            return moduleStructure;
        }

        public MavenFileFilter getMavenFileFilter()
        {
            return mavenFileFilter;
        }

        public List getFilterWrappers()
        {
            return filterWrappers;
        }

        public boolean isNonFilteredExtension( String fileName )
        {
            return !mavenResourcesFiltering.filteredFileExtension( fileName, nonFilteredFileExtensions );
        }

        public boolean isFilteringDeploymentDescriptors()
        {
            return filteringDeploymentDescriptors;
        }

        public ArtifactFactory getArtifactFactory()
        {
            return this.artifactFactory;
        }

		public ModuleResource[] getModuleResources() {
			return moduleResources;
		}

		public String getManifestEncoding()
		{
			return manifestEncoding;
		}

		public File getDescriptorsDirectory()
		{
			return descriptorsDir;
		}

		public void setDescriptorsDirectory(File directory)
		{
			setDescriptorsDirectory(directory);
		}

		public String getDescriptorsEncoding()
		{
			return descriptorsEncoding;
		}

		public boolean isDescriptorsN2AApply()
		{
			return descriptorsN2AApply;
		}

		public PlainEncodingConfig getDescriptorsN2AConfig()
		{
			return descriptorsN2AConfig;
		}

		public File getWorkDirectory()
		{
			return workDirectory;
		}

		public ModuleResource getModuleSourceResource() {
			ModuleResource defaultModuleLocation = new ModuleResource();
			defaultModuleLocation.setDirectory(moduleSourceDirectory.getAbsolutePath());
			defaultModuleLocation.setExcludes(new ArrayList(Arrays.asList(getModuleSourceExcludes())));
			defaultModuleLocation.setFiltering(false);
			defaultModuleLocation.setIncludes(new ArrayList(Arrays.asList(getModuleSourceIncludes())));
//			defaultModuleLocation.setModuleTargetPath(moduleSourceTargetDirectory);
			defaultModuleLocation.setTargetPath(moduleSourceTargetDirectory);
//			defaultModuleLocation.setModelEncoding(getModuleSourceEncoding());
			defaultModuleLocation.setN2aApply(true);
//			defaultModuleLocation.setN2aConfig(null);
			defaultModuleLocation.setSystemModule(true);
//			defaultModuleLocation.setTargetPath(null);
			return defaultModuleLocation;
		}

		public ModuleResource getClassesResource() {
			ModuleResource defaultModuleLocation = new ModuleResource();
			String targetDir  = new File(moduleSourceTargetDirectory, "classes").getPath();

			defaultModuleLocation.setDirectory(classesDirectory.getAbsolutePath());
//			defaultModuleLocation.setExcludes(null);
			defaultModuleLocation.setFiltering(false);
//			defaultModuleLocation.setIncludes(null);
			defaultModuleLocation.setModuleTargetPath("classes");
			defaultModuleLocation.setTargetPath(targetDir);
//			defaultModuleLocation.setModelEncoding(getModuleSourceEncoding());
//			defaultModuleLocation.setN2aApply(true);
//			defaultModuleLocation.setN2aConfig(null);
			defaultModuleLocation.setSystemModule(true);
//			defaultModuleLocation.setTargetPath(null);
			return defaultModuleLocation;
		}

		public ModuleResource getLibResource() {
			ModuleResource defaultModuleLocation = new ModuleResource();

			String targetDir  = new File(moduleSourceTargetDirectory, "lib").getPath();
			String libDirectory;
			if( moduleSourceTargetDirectory != null )
			{
				libDirectory = new File(
						new File(getModuleDirectory(),
								moduleSourceTargetDirectory),
						"lib").getAbsolutePath();
			}
			else
			{
				libDirectory = new File(getModuleDirectory(),
					"lib").getAbsolutePath();
			}

			defaultModuleLocation.setDirectory(libDirectory);
//			defaultModuleLocation.setExcludes(null);
			defaultModuleLocation.setFiltering(false);
//			defaultModuleLocation.setIncludes(null);
			defaultModuleLocation.setModuleTargetPath("lib");
			defaultModuleLocation.setTargetPath(targetDir);
//			defaultModuleLocation.setModelEncoding(getModuleSourceEncoding());
//			defaultModuleLocation.setN2aApply(true);
//			defaultModuleLocation.setN2aConfig(null);
			defaultModuleLocation.setSystemModule(true);
//			defaultModuleLocation.setTargetPath(null);
			return defaultModuleLocation;
		}
    }

    public MavenProject getProject()
    {
        return project;
    }

    public void setProject( MavenProject project )
    {
        this.project = project;
    }

    public File getClassesDirectory()
    {
        return classesDirectory;
    }

    public void setClassesDirectory( File classesDirectory )
    {
        this.classesDirectory = classesDirectory;
    }

    public File getModuleDirectory()
    {
        return moduleDirectory;
    }

    public void setModuleDirectory( File moduleDirectory )
    {
        this.moduleDirectory = moduleDirectory;
    }

    public File getModuleSourceDirectory()
    {
        return moduleSourceDirectory;
    }

    public void setModuleSourceDirectory( File moduleSourceDirectory )
    {
        this.moduleSourceDirectory = moduleSourceDirectory;
    }

    public String getModuleSourceTargetDirectory() {
		return moduleSourceTargetDirectory;
	}

	public void setModuleSourceTargetDirectory(String moduleSourceTargetDirectory) {
		if(moduleSourceTargetDirectory!=null
			&& moduleSourceTargetDirectory.contains("\\"))
		{
			moduleSourceTargetDirectory = moduleSourceTargetDirectory.replace('\\', '/');
		}

		this.moduleSourceTargetDirectory = moduleSourceTargetDirectory;
	}

	public File getManifestXml()
    {
        return manifestXml;
    }

    public void setManifestXml( File manifestXml )
    {
        this.manifestXml = manifestXml;
    }

    public String getDescriptorsEncoding() {
		return descriptorsEncoding;
	}

	public void setDescriptorsEncoding(String descriptorsEncoding) {
		this.descriptorsEncoding = descriptorsEncoding;
	}

	public boolean isDescriptorsN2AApply() {
		return descriptorsN2AApply;
	}

	public void setDescriptorsN2AApply(boolean descriptorsN2AApply) {
		this.descriptorsN2AApply = descriptorsN2AApply;
	}

	public PlainEncodingConfig getDescriptorsN2AConfig() {
		return descriptorsN2AConfig;
	}

	public void setDescriptorsN2AConfig(PlainEncodingConfig descriptorsN2AConfig) {
		this.descriptorsN2AConfig = descriptorsN2AConfig;
	}

	public boolean isGenerateManifestXml() {
		return generateManifestXml;
	}

	public void setGenerateManifestXml(boolean generateManifestXml) {
		this.generateManifestXml = generateManifestXml;
	}

	public String getOutputFileNameMapping()
    {
        return outputFileNameMapping;
    }

    public void setOutputFileNameMapping( String outputFileNameMapping )
    {
        this.outputFileNameMapping = outputFileNameMapping;
    }

    public boolean isAttachClasses() {
	    return attachClasses;
	}

	public void setAttachClasses(boolean attachClasses) {
	    this.attachClasses = attachClasses;
	}
    
    public boolean isArchiveClasses()
    {
        return archiveClasses;
    }

    public void setArchiveClasses( boolean archiveClasses )
    {
        this.archiveClasses = archiveClasses;
    }

    public JarArchiver getJarArchiver()
    {
        return jarArchiver;
    }

    public void setJarArchiver( JarArchiver jarArchiver )
    {
        this.jarArchiver = jarArchiver;
    }

    public ModuleResource[] getModuleResources()
    {
        return moduleResources;
    }

    public void setModulebResources( ModuleResource[] moduleResources )
    {
        this.moduleResources = moduleResources;
    }

    public List getFilters()
    {
        return filters;
    }

    public void setFilters( List filters )
    {
        this.filters = filters;
    }

    public File getWorkDirectory()
    {
        return workDirectory;
    }

    public void setWorkDirectory( File workDirectory )
    {
        this.workDirectory = workDirectory;
    }

    public String getModuleSourceIncludes()
    {
        return moduleSourceIncludes;
    }

    public void setModuleSourceIncludes( String moduleSourceIncludes )
    {
        this.moduleSourceIncludes = moduleSourceIncludes;
    }

    public String getModuleSourceExcludes()
    {
        return moduleSourceExcludes;
    }

    public void setModuleSourceExcludes( String moduleSourceExcludes )
    {
        this.moduleSourceExcludes = moduleSourceExcludes;
    }

    public String getModuleSourceEncoding() {
		return moduleSourceEncoding;
	}

	public String getEncoding() {
		return this.moduleSourceEncoding;
	}

	public MavenArchiveConfiguration getArchive()
    {
        return archive;
    }

    public List getNonFilteredFileExtensions()
    {
        return nonFilteredFileExtensions;
    }

    public void setNonFilteredFileExtensions( List nonFilteredFileExtensions )
    {
        this.nonFilteredFileExtensions = nonFilteredFileExtensions;
    }

    public ArtifactFactory getArtifactFactory()
    {
        return this.artifactFactory;
    }

    public void setArtifactFactory( ArtifactFactory artifactFactory )
    {
        this.artifactFactory = artifactFactory;
    }

	public Settings getSettings() {
		return settings;
	}

	public void setSettings(Settings settings) {
		this.settings = settings;
	}

	public boolean isDryRun() {
		return dryRun;
	}

	public void setDryRun(boolean dryRun) {
		this.dryRun = dryRun;
	}
}