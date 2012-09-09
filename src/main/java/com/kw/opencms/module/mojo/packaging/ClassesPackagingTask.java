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

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.interpolation.InterpolationException;

import com.kw.opencms.module.mojo.util.ClassesPackager;
import com.kw.opencms.module.mojo.util.PathSet;

/**
 * Handles the classes directory that needs to be packaged in the web application.
 * <p/>
 * Based on the {@link WarPackagingContext#archiveClasses()} flag the resources
 * either copied into to <tt>WEB-INF/classes</tt> directory or archived in a jar
 * within the <tt>WEB-INF/lib</tt> directory.
 *
 * @author Stephane Nicoll
 *
 * @version $Id: ClassesPackagingTask.java 751806 2009-03-09 19:44:08Z dennisl $
 */
public class ClassesPackagingTask
    extends AbstractModulePackagingTask
{

    public void performPackaging( ModulePackagingContext context )
        throws MojoExecutionException
    {
       	File base = context.getModuleSourceTargetDirectory() == null
			? context.getModuleDirectory()
			: new File( context.getModuleDirectory(), context.getModuleSourceTargetDirectory() );

        final File moduleClassesDirectory = new File( base, CLASSES_PATH );

        if ( context.getClassesDirectory().exists()
        		&& context.getClassesDirectory().list().length > 0
        		&& ! context.getClassesDirectory().equals( moduleClassesDirectory ) )
        {
            if ( context.archiveClasses() )
            {
                generateJarArchive( context );
            }
            else
            {
                final PathSet sources = getFilesToIncludes( context.getClassesDirectory(), null, null );
               	moduleClassesDirectory.mkdirs();
                try
                {
                	String prefix = context.getModuleSourceTargetDirectory() == null
    					? CLASSES_PATH
    					: context.getModuleSourceTargetDirectory().endsWith("/")
    						? context.getModuleSourceTargetDirectory().concat(CLASSES_PATH)
    						: context.getModuleSourceTargetDirectory().concat("/").concat(CLASSES_PATH);

                    copyFiles( "currentBuild", context, context.getClassesDirectory(),
                               sources, prefix, false, false );
                }
                catch ( IOException e )
                {
                    throw new MojoExecutionException(
                        "Could not copy module classes[" + context.getClassesDirectory().getAbsolutePath() + "]", e );
                }
            }
        }
    }

    protected void generateJarArchive( ModulePackagingContext context )
        throws MojoExecutionException
    {
        MavenProject project = context.getProject();
        ArtifactFactory factory = context.getArtifactFactory();
        Artifact artifact = factory.createBuildArtifact( project.getGroupId(), project.getArtifactId(),
                                                         project.getVersion(), "jar" );
        String archiveName = null;
        try
        {
            archiveName = getArtifactFinalName( context, artifact );
        }
        catch ( InterpolationException e )
        {
            throw new MojoExecutionException(
                "Could not get the final name of the artifact[" + artifact.getGroupId() + ":" + artifact.getArtifactId()
                    + ":" + artifact.getVersion() + "]", e );
        }
        final String targetFilename = LIB_PATH + archiveName;

        if ( context.getModuleStructure().registerFile( "currentBuild", targetFilename ) )
        {
        	File base = context.getModuleSourceTargetDirectory()==null
				? context.getModuleDirectory()
				: new File( context.getModuleDirectory(), context.getModuleSourceTargetDirectory() );

            final File libDirectory = new File( base, LIB_PATH );
            final File jarFile = new File( libDirectory, archiveName );
            final ClassesPackager packager = new ClassesPackager();
            packager.packageClasses( context.getClassesDirectory(), jarFile, context.getJarArchiver(),
                                     project, context.getArchive() );
            
        }
        else
        {
            context.getLog().warn(
                "Could not generate archive classes file[" + targetFilename + "] has already been copied." );
        }
    }
}
