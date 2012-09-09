package com.kw.opencms.module.mojo.packaging;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import java.io.File;
import java.io.IOException;
import java.util.Iterator;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.shared.filtering.MavenFilteringException;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.xml.XmlStreamReader;

import com.kw.opencms.module.mojo.ModuleResource;
import com.kw.opencms.module.mojo.native2ascii.Native2Ascii;
import com.kw.opencms.module.mojo.util.PathSet;

/**
 * Handles the project own resources, that is:
 * <ul
 * <li>The list of web resources, if any</li>
 * <li>The content of the module directory if it exists</li>
 * <li>The custom deployment descriptor(s), if any</li>
 * <li>The content of the classes directory if it exists</li>
 * <li>The dependencies of the project</li>
 * </ul>
 *
 * @author Stephane Nicoll
 * @version $Id: WarProjectPackagingTask.java 743319 2009-02-11 13:06:59Z dennisl $
 */
public class ModuleProjectPackagingTask
    extends AbstractModulePackagingTask
{
    private final ModuleResource[] moduleResources;

    private final File manifestXml;

    private final String id;

    private final boolean generateManifestXml;

    public ModuleProjectPackagingTask( ModuleResource[] moduleResources, File manifestXml, boolean generateManifestXml)
    {
        if ( moduleResources != null )
        {
            this.moduleResources = moduleResources;
        }
        else
        {
            this.moduleResources = new ModuleResource[0];
        }
        this.manifestXml = manifestXml;
        this.id = "currentBuild";
        this.generateManifestXml = generateManifestXml;
    }

    public void performPackaging( ModulePackagingContext context )
        throws MojoExecutionException, MojoFailureException
    {

        context.getLog().info( "Processing opencms-module project" );

        if(context.isAttachClasses())
        	handleClassesDirectory( context );

        handleModuleResources( context );

        handeModuleSourceDirectory( context );

        // Debug mode: dump the path set for the current build
        if( context.getLog().isDebugEnabled() )
        {
        	PathSet pathSet = context.getModuleStructure().getStructure( "currentBuild" );
	        context.getLog().debug( "Dump of the current build pathSet content -->" );
	        for ( Iterator iterator = pathSet.iterator(); iterator.hasNext(); )
	        {
	            context.getLog().debug( "" + iterator.next() );
	        }
	        context.getLog().debug( "-- end of dump --" );
        }

        handleManifestDescriptors( context );
        
        if(context.isAttachClasses())
        	handleArtifacts( context );

        handleDeploymentDescriptors( context );
    }

    protected void handleManifestDescriptors( ModulePackagingContext context )
    	throws MojoExecutionException, MojoFailureException
    {
    	if ( !generateManifestXml ){
    		return;
    	}

    	if ( !context.getDescriptorsDirectory().exists() )
        {
            throw new MojoExecutionException( "module manifest descriptors directory[" + context.getDescriptorsDirectory() + " does not exist." );
        }
        else if ( !context.getDescriptorsDirectory().getAbsolutePath().equals( context.getModuleDirectory().getPath() ) )
        {
        	try
            {
        		boolean n2aApply = context.isDescriptorsN2AApply();

        		String copyTargetPrefix = n2aApply? "manifest_native/" : "manifest/";
	        	context.getLog().info( "Copying module manifest descriptors resources[" + context.getDescriptorsDirectory() + "]" );
	        	final PathSet sources = getFilesToIncludes( context.getDescriptorsDirectory(), null, null);

                copyFiles( id, context, context.getDescriptorsDirectory(), sources, copyTargetPrefix, true, true );

                // Manage the native2ascii setting
	        	if(n2aApply)
	        	{
	            	Native2Ascii native2AsciiTask = new Native2Ascii( );
	            	native2AsciiTask.setEncoding(context.getDescriptorsEncoding());

	            	ModuleResource resource = new ModuleResource();
	            	resource.setDirectory( new File(context.getWorkDirectory(), copyTargetPrefix).getAbsolutePath());
	            	resource.setModuleWorkingPath(
	            			new File(context.getWorkDirectory(),"manifest"));
	            	resource.setTargetPath("manifest/");
	            	resource.setN2aApply(true);
	            	resource.setN2aConfig(context.getDescriptorsN2AConfig());
	            	native2AsciiTask.perform(context, resource);
	        	}
            }
            catch ( IOException e )
            {
                throw new MojoExecutionException(
                    "Could not copy module manifest descriptors resources[" + context.getDescriptorsDirectory().getAbsolutePath() + "]", e );
            }
        }
    }

    /**
     * Handles the module resources.
     *
     * @param context the packaging context
     * @throws MojoExecutionException if a resource could not be copied
     * @throws MojoFailureException
     */
    protected void handleModuleResources( ModulePackagingContext context )
        throws MojoExecutionException, MojoFailureException
    {
        for ( int i = 0; i < moduleResources.length; i++ )
        {
            ModuleResource resource = moduleResources[i];
            File resourceFile = new File(resource.getDirectory());

            if ( !resourceFile.isAbsolute() )
            {
            	resourceFile = new File(context.getProject().getBasedir(), resource.getDirectory());
                resource.setDirectory( resourceFile.getAbsolutePath() );
            }

            // Make sure that the resource directory is not the same as the moduleDirectory
            if ( !resource.getDirectory().equals( context.getModuleDirectory().getPath() ) )
            {

                try
                {
                    copyResources( context, resource, resource.isN2aApply() );

                	// Manage the native2ascii setting
                    if( resource.isN2aApply() )
                    {
                    	resource.setDirectory( new File( context.getWorkDirectory(),
                    				resourceFile.getName()).getAbsolutePath() );
                    	Native2Ascii native2AsciiTask = new Native2Ascii( );
                    	native2AsciiTask.setEncoding( resource.getN2aConfig() != null
                    			? resource.getN2aConfig().getEncoding()
                    			: "default");
                    	native2AsciiTask.perform(context, resource);
                    }
                }
                catch ( IOException e )
                {
                    throw new MojoExecutionException( "Could not copy resource[" + resource.getDirectory() + "]", e );
                }
            }
        }
    }

    /**
     * Handles the module sources.
     *
     * @param context the packaging context
     * @throws MojoExecutionException if the sources could not be copied
     */
    protected void handeModuleSourceDirectory( ModulePackagingContext context )
        throws MojoExecutionException
    {
        if ( !context.getModuleSourceDirectory().exists() )
        {
            context.getLog().debug( "module sources directory does not exist - skipping." );
        }
        else
        if ( !context.getModuleSourceDirectory().getAbsolutePath().equals( context.getModuleDirectory().getPath() ) )
        {
            context.getLog().info( "Copying module system resources[" + context.getModuleSourceDirectory() + "]" );
            final PathSet sources = getFilesAndDirectoriesToIncludes(
            		context.getModuleSourceDirectory(),
                    context.getModuleSourceIncludes(),
                    context.getModuleSourceExcludes() );

            try
            {
            	String prefix = context.getModuleSourceTargetDirectory();
            	if ( prefix != null )
                {
                    if ( StringUtils.equals( ".", prefix )
                        || StringUtils.equals( "./", prefix ) )
                    {
                    	prefix = null;
                    }
                    else if(!prefix.endsWith("/"))
                    {
                    	prefix += "/";
                    }

                }
            	copyFilesAndDirs( id, context, context.getModuleSourceDirectory(), sources, prefix, true, false );
            }
            catch ( IOException e )
            {
                throw new MojoExecutionException(
                    "Could not copy webapp sources[" + context.getModuleDirectory().getAbsolutePath() + "]", e );
            }
        }
    }

    /**
     * Handles the webapp artifacts.
     *
     * @param context the packaging context
     * @throws MojoExecutionException if the artifacts could not be packaged
     */
    protected void handleArtifacts( ModulePackagingContext context )
        throws MojoExecutionException
    {
        ArtifactsPackagingTask task = new ArtifactsPackagingTask( context.getProject().getArtifacts() );
        task.performPackaging( context );
    }

    /**
     * Handles the module classes.
     *
     * @param context the packaging context
     * @throws MojoExecutionException if the classes could not be packaged
     */
    protected void handleClassesDirectory( ModulePackagingContext context )
        throws MojoExecutionException
    {
        ClassesPackagingTask task = new ClassesPackagingTask();
        task.performPackaging( context );
    }

    /**
     * Handles the deployment descriptors, if specified. Note that the behavior
     * here is slightly different since the customized entry always win, even if
     * an overlay has already packaged a web.xml previously.
     *
     * @param context    the packaging context
     * @param webinfDir  the web-inf directory
     * @throws MojoFailureException   if the web.xml is specified but does not exist
     * @throws MojoExecutionException if an error occurred while copying the descriptors
     */
    protected void handleDeploymentDescriptors( ModulePackagingContext context)
        throws MojoFailureException, MojoExecutionException
    {
    	File manifestDir = context.getModuleDirectory();
        try
        {
            if ( manifestXml != null && StringUtils.isNotEmpty( manifestXml.getName() ) )
            {
                if ( !manifestXml.exists() )
                {
                    throw new MojoFailureException( "The specified manifest file '" + manifestXml + "' does not exist" );
                }
                if ( context.isFilteringDeploymentDescriptors() )
                {
                    context.getMavenFileFilter().copyFile( manifestXml, new File( manifestDir, "manifest.xml" ), true,
                                                           context.getFilterWrappers(), getEncoding( manifestXml ) );
                }
                else
                {
                    copyFile( context, manifestXml, new File( manifestDir, "manifest.xml" ), "manifest.xml", true );
                }

                context.getModuleStructure().getFullStructure().add( "manifest.xml" );
            }
            else
            {
                // the manifestXml can be the default one
                File defaultManifestbXml = new File( context.getModuleSourceDirectory(), "manifest.xml" );
                // if exists we can filter it
                if ( defaultManifestbXml.exists() )
                {
                	if(context.isFilteringDeploymentDescriptors() )
	                {
	                    context.getMavenFileFilter().copyFile( defaultManifestbXml, new File( manifestDir, "manifest.xml" ), true,
	                                                           context.getFilterWrappers(), getEncoding( defaultManifestbXml ) );
	                    context.getModuleStructure().getFullStructure().add( "manifest.xml" );
	                }
                	// if not, its just copied without filtering and registered
                }
                else if ( generateManifestXml )
                {
                	ManifestGenerationTask task = new ManifestGenerationTask();
                    task.performPackaging( context );
                }
            }
        }
        catch ( IOException e )
        {
            throw new MojoExecutionException( "Failed to copy deployment descriptor", e );
        }
        catch ( MavenFilteringException e )
        {
            throw new MojoExecutionException( "Failed to copy deployment descriptor", e );
        }
    }

    /**
     * Get the encoding from an XML-file.
     *
     * @param webXml the XML-file
     * @return The encoding of the XML-file, or UTF-8 if it's not specified in the file
     * @throws IOException if an error occurred while reading the file
     */
    private String getEncoding( File webXml )
        throws IOException
    {
        XmlStreamReader xmlReader = new XmlStreamReader( webXml );
        return xmlReader.getEncoding();
    }

    /**
     * Copies module moduleResources from the specified directory.
     *
     * @param context  the war packaging context to use
     * @param resource the resource to copy
     * @throws IOException            if an error occurred while copying the resources
     * @throws MojoExecutionException if an error occurred while retrieving the filter properties
     */
    public void copyResources( ModulePackagingContext context, ModuleResource resource, boolean toWorkDir )
        throws IOException, MojoExecutionException
    {
    	File resourceDir = new File(resource.getDirectory());
        if ( !resourceDir.exists() )
        {
            context.getLog().warn("Not copying module moduleResources [" + resource.getDirectory()
                                       + "]: module directory[" + context.getModuleDirectory().getAbsolutePath()
                                       + "] does not exist!" );
            return;
        }


        context.getLog().info("Copying module resources [" + resource.getDirectory() + "] to ["
                                   + context.getModuleDirectory().getAbsolutePath() + "]" );

        String prefix = toWorkDir? resourceDir.getName() + File.separator : "";
        if ( resource.getTargetPath() != null
        	&& ! resource.getTargetPath().isEmpty() )
        {
            //TODO make sure this thing is 100% safe
            // MWAR-129 if targetPath is only a dot <targetPath>.</targetPath> or ./
            // and the Resource is in a part of the warSourceDirectory the file from sources will override this
            // that's we don't have to add the targetPath yep not nice but works
            if ( !StringUtils.equals( ".", resource.getTargetPath() )
                && !StringUtils.equals( "./", resource.getTargetPath() ) )
            {

                prefix = resource.getTargetPath() + File.separator;
            }
        }

        String[] fileNames = getFilesToCopy( resource );
        for ( int i = 0; i < fileNames.length; i++ )
        {
            String targetFileName = fileNames[i];
            targetFileName = prefix.concat(targetFileName);

            if ( resource.isFiltering() && !context.isNonFilteredExtension( fileNames[i] ) )
            {
                copyFilteredFile( id, context, new File( resourceDir, fileNames[i] ), targetFileName, toWorkDir );
            }
            else
            {
                copyFile( id, context, new File( resourceDir, fileNames[i] ), targetFileName, toWorkDir );
            }
        }

        String[] dirNames = getDirectoriesToCopy( resource );
        for ( int i = 0; i < dirNames.length; i++ )
        {
        	String targetFileName = dirNames[i];
        	targetFileName = prefix.concat(targetFileName);

        	File targetDir = new File( toWorkDir
        			? context.getWorkDirectory()
            		: context.getModuleDirectory(), targetFileName );

        	if ( ! targetDir.exists() )
        	{
                targetDir.mkdirs();
            }
        }

        if ( toWorkDir )
//        		&& resource.getTargetPath() != null )
        {
        	resource.setModuleWorkingPath(context.getModuleDirectory());
        	resource.setDirectory(new File(context.getWorkDirectory(),resourceDir.getName()).getAbsolutePath());
        }
    }

}
