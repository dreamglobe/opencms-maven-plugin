package com.kw.opencms.module.mojo.packaging;

import java.io.File;
import java.util.List;

import org.apache.maven.archiver.MavenArchiveConfiguration;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.filtering.MavenFileFilter;
import org.codehaus.plexus.archiver.jar.JarArchiver;
import org.codehaus.plexus.archiver.manager.ArchiverManager;
import org.codehaus.plexus.util.FileUtils.FilterWrapper;

import com.kw.opencms.module.mojo.ModuleResource;
import com.kw.opencms.module.mojo.PlainEncodingConfig;
import com.kw.opencms.module.mojo.util.ModuleStructure;

/**
 * The packaging context.
 *
 * @author Stephane Nicoll
 * @version $Id: WarPackagingContext.java 743374 2009-02-11 16:28:01Z dennisl $
 */
public interface ModulePackagingContext
{
    /**
     * Returns the maven project.
     *
     * @return the project
     */
    MavenProject getProject();

    /**
     * Returns the Module directory. Packaging tasks should use this
     * directory to generate the Module.
     *
     * @return the Module directory
     */
    File getModuleDirectory();

    /**
     * Returns the main Module source directory.
     *
     * @return the Module source directory
     */
    File getModuleSourceDirectory();

    /**
     * Returns the Module source includes.
     *
     * @return the Module source includes
     */
    String[] getModuleSourceIncludes();

    /**
     * Returns the Module source excludes.
     *
     * @return the Module source excludes
     */
    String[] getModuleSourceExcludes();

    /**
     * Returns the directory holding generated classes.
     *
     * @return the classes directory
     */
    File getClassesDirectory();

    /**
	 * Whether classes (module/classes or lib directory) should be attached to the project.
	 * @return true if classes should be archived, false otherwise
	 */
    boolean isAttachClasses();
    
    /**
     * Specify whether the classes resources should be archived in
     * the <tt>/lib</tt> of the generated module.
     *
     * @return true if the classes should be archived, false otherwise
     */
    boolean archiveClasses();

    /**
     * Returns the logger to use to output logging event.
     *
     * @return the logger
     */
    Log getLog();

    /**
     * Returns the archiver manager to use.
     *
     * @return the archiver manager
     */
    ArchiverManager getArchiverManager();

    /**
     * The maven archive configuration to use.
     *
     * @return the maven archive configuration
     */
    MavenArchiveConfiguration getArchive();

    /**
     * Returns the Jar archiver needed for archiving classes directory into
     * jar file under WEB-INF/lib.
     *
     * @return the jar archiver to user
     */
    JarArchiver getJarArchiver();

    /**
     * Returns the output file name mapping to use, if any. Returns <tt>null</tt>
     * if no file name mapping is set.
     *
     * @return the output file name mapping or <tt>null</tt>
     */
    String getOutputFileNameMapping();

    /**
     * Returns the list of filter files to use.
     *
     * @return a list of filter files
     */
    List getFilters();

    /**
     * Returns the {@link ModuleStructure}.
     *
     * @return the Module structure
     */
    ModuleStructure getModuleStructure();

    /**
     * Returns the {@link MavenFileFilter} instance to use.
     *
     * @return the maven file filter to use
     * @since 2.1-alpha-2
     */
    MavenFileFilter getMavenFileFilter();

    /**
     * @return {@link List} of {@link FilterWrapper}
     * @since 2.1-alpha-2
     */
    List getFilterWrappers();

    /**
     * Specify if the given <tt>fileName</tt> belongs to the list of extensions
     * that must not be filtered
     *
     * @param fileName the name of file
     * @return <tt>true</tt> if it should not be filtered, <tt>false</tt> otherwise
     * @since 2.1-alpha-2
     */
    boolean isNonFilteredExtension( String fileName );

    boolean isFilteringDeploymentDescriptors();

    ArtifactFactory getArtifactFactory();

    ModuleResource[] getModuleResources();

    String getManifestEncoding();

    File getDescriptorsDirectory();

    void setDescriptorsDirectory(File directory);

    String getDescriptorsEncoding();

	boolean isDescriptorsN2AApply();

	PlainEncodingConfig getDescriptorsN2AConfig();

	File getWorkDirectory();

	ModuleResource getModuleSourceResource();

	ModuleResource getClassesResource();

	ModuleResource getLibResource();

	String getModuleSourceTargetDirectory();

}
