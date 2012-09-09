package com.kw.opencms.module.mojo;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import org.apache.maven.archiver.MavenArchiver;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProjectHelper;
import org.codehaus.plexus.archiver.ArchiverException;
import org.codehaus.plexus.archiver.jar.JarArchiver;
import org.codehaus.plexus.archiver.jar.ManifestException;
import org.codehaus.plexus.util.StringUtils;

import com.kw.opencms.module.mojo.util.ClassesPackager;

/**
 * Build opencms module. The process includes the manifest generation if <code>${generate.manifest}</code> 
 * and module properties descriptor are set. This goal is binded to <code>package</code> building phase for the 
 * opencms-module projects.
 *
 * @author <a href="joseangel.garcia@vdos.com">José Ángel García</a>
 * @goal module
 * @phase package
 * @requiresDependencyResolution runtime
 */
public class ModuleMojo
    extends AbstractModuleMojo
{

	/**
     * The directory for the generated module.
     * @parameter expression="${project.build.directory}"
     * @required
     */
    private String outputDirectory;

    /**
     * The name of the generated module.
     * @parameter expression="${project.build.finalName}"
     * @required
     */
    private String moduleName;

    /**
     * Classifier to add to the generated module file. If given, the artifact will be an attachment instead.
     * The classifier will not be applied to the jar file of the project - only to the module file.
     * @parameter
     */
    private String classifier;

    /**
     * The comma separated list of tokens to exclude from the module before
     * packaging. 
     * @parameter alias="packagingExcludes"
     */
    private String packagingExcludes;

    /**
     * The comma separated list of tokens to include in the module before
     * packaging. By default everything is included. 
     * @parameter alias="packagingIncludes"
     */
    private String packagingIncludes;

    /**
     * Whether this is the main artifact being built. Set to <code>false</code> if you don't want to install or
     * deploy it to the local repository instead of the default one in an execution.
     * @parameter expression="${primaryArtifact}" default-value="true"
     */
    private boolean primaryArtifact = true;

    /**
     * Whether or not to fail the build is the <code>manifest.xml</code> file is missing. 
     * Set to <code>false</code> if you want you module built without a <code>manifest.xml</code> file, 
     * besides the module won't be installable on OpenCms system.
     * @parameter expression="${failOnMissingManifestXml}" default-value="true"
     */
    private boolean failOnMissingManifestXml = true;

    /**
     * The classifier to use for the attached classes artifact.
     * @parameter default-value=""
     */
    private String classesClassifier = "";

    /**
     * The archiver.
     * @component role="org.codehaus.plexus.archiver.Archiver" roleHint="jar"
     */
    private JarArchiver moduleArchiver;

    /**
     * @component
     */
    private MavenProjectHelper projectHelper;

    /**
     * Executes the ModuleMojo on the current project.
     *
     * @throws MojoExecutionException if an error occurred while building the module
     */
	public void execute() throws MojoExecutionException, MojoFailureException {
		 File moduleFile = getTargetModuleFile();

	        try
	        {
	            performPackaging( moduleFile );
	        }
	        catch ( DependencyResolutionRequiredException e )
	        {
	            throw new MojoExecutionException( "Error assembling Module: " + e.getMessage(), e );
	        }
	        catch ( ManifestException e )
	        {
	            throw new MojoExecutionException( "Error assembling Module", e );
	        }
	        catch ( IOException e )
	        {
	            throw new MojoExecutionException( "Error assembling Module", e );
	        }
	        catch ( ArchiverException e )
	        {
	            throw new MojoExecutionException( "Error assembling Module: " + e.getMessage(), e );
	        }
	}

	/**
     * Generates the module according to the <tt>mode</tt> attribute.
     *
     * @param moduleFile the target module file
     * @throws IOException            if an error occurred while copying files
     * @throws ArchiverException      if the archive could not be created
     * @throws ManifestException      if the manifest could not be created
     * @throws DependencyResolutionRequiredException
     *                                if an error occurred while resolving the dependencies
     * @throws MojoExecutionException if the execution failed
     * @throws MojoFailureException   if a fatal exception occurred
     */
    private void performPackaging( File moduleFile )
        throws IOException, ArchiverException, ManifestException, DependencyResolutionRequiredException,
        MojoExecutionException, MojoFailureException
    {
        getLog().info( "Packaging OpenCms Module" );

        buildExplodedModule( getModuleDirectory() );

        MavenArchiver archiver = new MavenArchiver();

        archiver.setArchiver( moduleArchiver );

        archiver.setOutputFile( moduleFile );

        getLog().debug(
            "Excluding " + Arrays.asList( getPackagingExcludes() ) + " from the generated webapp archive." );
        getLog().debug(
            "Including " + Arrays.asList( getPackagingIncludes() ) + " in the generated webapp archive." );

        moduleArchiver.addDirectory( getModuleDirectory(), getPackagingIncludes(), getPackagingExcludes() );

        final File manifestXmlFile = new File( getModuleDirectory(), MANIFEST_NAME );
        if ( !manifestXmlFile.exists() )
        {
        	if(!failOnMissingManifestXml )
        	{
        		getLog().warn( "Build won't fail if manifest.xml file is missing." );
//            // The flag is wrong in plexus-archiver so it will need to be fixed at some point
//            zipArchiver.setIgnoreWebxml( false );
	        }
	        else
	        {
	        	throw new ManifestException("Module Manifest missing.");
	        }
        }

        // create archive
        archiver.createArchive( getProject(), getArchive() );

        // create the classes to be attached if necessary
        ClassesPackager packager = new ClassesPackager();
        final File classesDirectory = getClassesDirectory();
        if ( classesDirectory.exists() )
        {
            getLog().info( "Packaging classes" );
            packager.packageClasses( classesDirectory, getTargetClassesFile(), getModuleArchiver(), getProject(),
                                     getArchive() );
            projectHelper.attachArtifact( getProject(), "jar", getClassesClassifier(), getTargetClassesFile() );
        }

        String classifier = this.classifier;
        if ( classifier != null )
        {
            projectHelper.attachArtifact( getProject(), "zip", classifier, moduleFile );
        }
        else
        {
            Artifact artifact = getProject().getArtifact();
            if ( primaryArtifact )
            {
                artifact.setFile( moduleFile );
            }
            else if ( artifact.getFile() == null || artifact.getFile().isDirectory() )
            {
                artifact.setFile( moduleFile );
            }
        }
    }

    protected static File getTargetFile( File basedir, String finalName, String classifier, String type )
    {
        if ( classifier == null )
        {
            classifier = "";
        }
        else if ( classifier.trim().length() > 0 && !classifier.startsWith( "-" ) )
        {
            classifier = "-" + classifier;
        }

        return new File( basedir, finalName + classifier + "." + type );
    }

	protected File getTargetModuleFile()
    {
        return getTargetFile( new File( getOutputDirectory() ), getModuleName(), getClassifier(), "zip" );

    }

    protected File getTargetClassesFile()
    {
        return getTargetFile( new File( getOutputDirectory() ), getModuleName(), getClassesClassifier(), "jar" );
    }

    // Getters and Setters

    public String getClassifier()
    {
        return classifier;
    }

    public void setClassifier( String classifier )
    {
        this.classifier = classifier;
    }

    public String[] getPackagingExcludes()
    {
        if ( StringUtils.isEmpty( packagingExcludes ) )
        {
        	return new String[]{"**/__properties","**/__properties/**"};
        }
        else
        {
            return StringUtils.split( packagingExcludes, "," );
        }
    }

    public void setPackagingExcludes( String packagingExcludes )
    {
        this.packagingExcludes = packagingExcludes;
    }

    public String[] getPackagingIncludes()
    {
        if ( StringUtils.isEmpty( packagingIncludes ) )
        {
            return new String[]{"**"};
        }
        else
        {
            return StringUtils.split( packagingIncludes, "," );
        }
    }

    public void setPackagingIncludes( String packagingIncludes )
    {
        this.packagingIncludes = packagingIncludes;
    }

    public String getOutputDirectory()
    {
        return outputDirectory;
    }

    public void setOutputDirectory( String outputDirectory )
    {
        this.outputDirectory = outputDirectory;
    }

    public String getModuleName()
    {
        return moduleName;
    }

    public void setModuleName( String moduleName )
    {
        this.moduleName = moduleName;
    }

    public JarArchiver getModuleArchiver()
    {
        return moduleArchiver;
    }

    public void setModuleArchiver( JarArchiver zipArchiver )
    {
        this.moduleArchiver = zipArchiver;
    }

    public MavenProjectHelper getProjectHelper()
    {
        return projectHelper;
    }

    public void setProjectHelper( MavenProjectHelper projectHelper )
    {
        this.projectHelper = projectHelper;
    }

    public boolean isPrimaryArtifact()
    {
        return primaryArtifact;
    }

    public void setPrimaryArtifact( boolean primaryArtifact )
    {
        this.primaryArtifact = primaryArtifact;
    }

    public String getClassesClassifier()
    {
        return classesClassifier;
    }

    public void setClassesClassifier( String classesClassifier )
    {
        this.classesClassifier = classesClassifier;
    }

    public boolean isFailOnMissingManifestXml()
    {
        return failOnMissingManifestXml;
    }

    public void setFailOnMissingManifestXml( boolean failOnMissingWebXml )
    {
        this.failOnMissingManifestXml = failOnMissingWebXml;
    }
}