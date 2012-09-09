package com.kw.opencms.module.mojo.packaging;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

/**
 * Defines tasks that should be performed after the packaging.
 *
 * @author Stephane Nicoll
 * @version $Id: WarPostPackagingTask.java 682908 2008-08-05 19:57:49Z hboutemy $
 */
public interface ModulePostPackagingTask
{

    /**
     * Executes the post packaging task.
     * <p/>
     * The packaging context hold all information regarding the module that
     * has been packaged.
     *
     * @param context the packaging context
     * @throws MojoExecutionException if an error occurred
     * @throws MojoFailureException   if a failure occurred
     */
    void performPostPackaging( ModulePackagingContext context )
        throws MojoExecutionException, MojoFailureException;

}
