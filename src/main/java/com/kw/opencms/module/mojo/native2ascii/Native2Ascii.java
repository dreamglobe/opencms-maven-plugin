/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package com.kw.opencms.module.mojo.native2ascii;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.codehaus.plexus.util.DirectoryScanner;
import org.codehaus.plexus.util.FileUtils;

import com.kw.opencms.module.mojo.ModuleResource;
import com.kw.opencms.module.mojo.PlainEncodingConfig;
import com.kw.opencms.module.mojo.native2ascii.factory.Native2AsciiAdapter;
import com.kw.opencms.module.mojo.native2ascii.factory.SunNative2Ascii;
import com.kw.opencms.module.mojo.packaging.ModulePackagingContext;
import com.kw.opencms.module.mojo.util.Commandline.Argument;

/**
 * Converts files from native encodings to ASCII.
 *
 * @since Ant 1.2
 */
public class Native2Ascii {
	public static final String[] DEFAULT_INCLUDES = {"**/**"};

    private boolean reverse = false;  // convert from ascii back to native
    private String encoding = null;   // encoding to convert to/from

    private Native2AsciiAdapter nestedAdapter = null;
	private List<String> args = new ArrayList<String>();

    public Native2Ascii()
    {
    	nestedAdapter = new SunNative2Ascii();
    }

    /**
     * Flag the conversion to run in the reverse sense,
     * that is Ascii to Native encoding.
     *
     * @param reverse True if the conversion is to be reversed,
     *                otherwise false;
     */
    public void setReverse(boolean reverse) {
        this.reverse = reverse;
    }

    /**
     * The value of the reverse attribute.
     * @return the reverse attribute.
     * @since Ant 1.6.3
     */
    public boolean getReverse() {
        return reverse;
    }

    /**
     * Set the encoding to translate to/from.
     * If unset, the default encoding for the JVM is used.
     *
     * @param encoding String containing the name of the Native
     *                 encoding to convert from or to.
     */
    public void setEncoding(String encoding) {
        this.encoding = encoding;
    }

    /**
     * The value of the encoding attribute.
     * @return the encoding attribute.
     * @since Ant 1.6.3
     */
    public String getEncoding() {
        return encoding;
    }

    /**
     * Choose the implementation for this particular task.
     * @param impl the name of the implemenation
     * @since Ant 1.6.3
     */
//    public void setImplementation(String impl) {
//        if ("default".equals(impl)) {
//            facade.setImplementation(Native2AsciiAdapterFactory.getDefault());
//        } else {
//            facade.setImplementation(impl);
//        }
//    }

    /**
     * Defines the FileNameMapper to use (nested mapper element).
     *
     * @return the mapper to use for file name translations.
     *
     * @throws BuildException if more than one mapper is defined.
     */
//    public Mapper createMapper() throws BuildException {
//        if (mapper != null) {
//            throw new BuildException("Cannot define more than one mapper",
//                                     getLocation());
//        }
//        mapper = new Mapper(getProject());
//        return mapper;
//    }

    /**
     * A nested filenamemapper
     * @param fileNameMapper the mapper to add
     * @since Ant 1.6.3
     */
//    public void add(FileNameMapper fileNameMapper) {
//        createMapper().add(fileNameMapper);
//    }

    /**
     * Adds an implementation specific command-line argument.
     * @return a ImplementationSpecificArgument to be configured
     *
     * @since Ant 1.6.3
     */
//    public ImplementationSpecificArgument createArg() {
//        ImplementationSpecificArgument arg =
//            new ImplementationSpecificArgument();
//        facade.addImplementationArgument(arg);
//        return arg;
//    }

    /**
     * The classpath to use when loading the native2ascii
     * implementation if it is not a built-in one.
     *
     * @since Ant 1.8.0
     */
//    public Path createImplementationClasspath() {
//        return facade.getImplementationClasspath(getProject());
//    }

    /**
     * Set the adapter explicitly.
     * @throws MojoExecutionException
     * @since Ant 1.8.0
     */
    public void add(Native2AsciiAdapter adapter) throws MojoExecutionException {
        if (nestedAdapter != null) {
            throw new MojoExecutionException("Can't have more than one native2ascii"
                                     + " adapter");
        }
        nestedAdapter = adapter;
    }


    /**
     *
     * @param context
     * @param resource
     * @throws MojoExecutionException
     * @throws MojoFailureException
     */
    public void perform(ModulePackagingContext context, ModuleResource resource)
    	throws MojoExecutionException, MojoFailureException
    {

    	File src = new File(resource.getDirectory());
    	if(!src.exists()) return;
		// Set defaults where no user input
		PlainEncodingConfig n2aConfig = fillDefaultConfig(resource.getN2aConfig());
		File targetPath = resource.getModuleWorkingPath() != null
			? resource.getModuleWorkingPath()
			: new File(context.getWorkDirectory(), src.getName());
    	logPerform(context.getLog(), n2aConfig, src, targetPath);

    	// Decode files in the resource
    	String[] filesToDecode = getFilesToIncludes(n2aConfig, src);

    	for (String filePath : filesToDecode)
    	{
    		convert(filePath, filePath, src, targetPath);
    	}

    	// Copy the rest of the files
    	String[] filesToCopy = getFilesAndDirectoriesToExclude(n2aConfig, src);
    	for( String filePath : filesToCopy )
    	{

    		File source = new File( src,filePath );
    		File destination = new File( targetPath, filePath );
    		if( source.isDirectory() )
    		{
    			destination.mkdirs();
    		}
    		else
    		{

                try {
					FileUtils.copyFile( source.getCanonicalFile(), destination );
				}
                catch (IOException e)
				{
					throw new MojoFailureException( "Error copying " + destination.getAbsolutePath(), e );
				}

                // preserve timestamp
                destination.setLastModified( source.lastModified() );
                context.getLog().debug( " + " + destination.getAbsolutePath() + " has been copied." );
            }
    	}
	}

    /**
     * Convert a single file.
     *
     * @param srcName name of the input file.
     * @param destName name of the input file.
     */
    private void convert(String srcName, String destName, File srcDir, File destDir)
        throws MojoExecutionException {
        File srcFile;                         // File to convert
        File destFile;                        // where to put the results

        // Build the full file names
        srcFile = new File(srcDir, srcName);
        destFile = new File(destDir, destName);

        // Make sure we're not about to clobber something
        if (srcFile.equals(destFile)) {
            throw new MojoExecutionException("file " + srcFile
                                     + " would overwrite its self");
        }

        // Make intermediate directories if needed
        // XXX JDK 1.1 doesn't have File.getParentFile,
        String parentName = destFile.getParent();
        if (parentName != null) {
            File parentFile = new File(parentName);

            if ((!parentFile.exists()) && (!parentFile.mkdirs())) {
                throw new MojoExecutionException("cannot create parent directory "
                                         + parentName);
            }
        }

//        log("converting " + srcName, Project.MSG_VERBOSE);

        if (!nestedAdapter.convert(this, srcFile, destFile)) {
            throw new MojoExecutionException("conversion failed");
        }
    }

	private void logPerform(Log log, PlainEncodingConfig n2aConfig, File src, File target)
	{
		log.info( new StringBuilder().append("Converting for ")
				.append(n2aConfig.getEncoding() == null? " default" : n2aConfig.getEncoding())
				.append(" [")
				.append(src.getAbsolutePath())
				.append("] to[")
				.append(target.getAbsolutePath())
				.append("] (")
				.append("Includes: ")
				.append(n2aConfig.getIncludes())
				.append(n2aConfig.getExcludes()!=null
						?" / Excludes: " + n2aConfig.getExcludes()
						:"")
				.append(")").toString());
	}

	private PlainEncodingConfig fillDefaultConfig(PlainEncodingConfig n2aConfig)
	{
		if(n2aConfig==null){
			n2aConfig = new PlainEncodingConfig();
		}
		if(n2aConfig.getIncludes()==null){
			List<String> includes = new ArrayList<String>(2);
			includes.add("**/*.properties");
			includes.add("**/*.acl");
			n2aConfig.setIncludes(includes);
		}
		if(n2aConfig.getExcludes()!= null && !n2aConfig.getExcludes().isEmpty())
    	{
    		if(n2aConfig.getIncludes().isEmpty())
    		{
    			n2aConfig.getIncludes().add("**/*.*");
    		}
    	}
		return n2aConfig;
	}

	/**
     * Retrieves the command line arguments enabled for the current
     * facade implementation.
     * @return an array of command line arguements.
     */
    public String[] getArgs() {
        List tmp = new ArrayList(args.size());
        for (Iterator e = args.iterator(); e.hasNext();) {
            Argument arg =
                ((Argument) e.next());
            String[] curr = arg.getParts();
            for (int i = 0; i < curr.length; i++) {
                tmp.add(curr[i]);
            }
        }
        String[] res = new String[tmp.size()];
        return (String[]) tmp.toArray(res);
    }

    protected String[] getFilesToIncludes(PlainEncodingConfig n2aConfig, File src)
    {
    	DirectoryScanner scanner = new DirectoryScanner();
        scanner.setBasedir( src );
        scanner.setIncludes( (String[]) n2aConfig.getIncludes()
        		.toArray( new String[n2aConfig.getIncludes().size()] ) );

        if ( n2aConfig.getExcludes() != null && !n2aConfig.getExcludes().isEmpty() )
        {
            scanner.setExcludes(
                (String[]) n2aConfig.getExcludes().toArray( new String[n2aConfig.getExcludes().size()] ) );
        }

        scanner.addDefaultExcludes();
        scanner.scan();
        return scanner.getIncludedFiles();

    }

    protected String[] getFilesAndDirectoriesToExclude(PlainEncodingConfig n2aConfig, File src)
    {
    	DirectoryScanner scanner = new DirectoryScanner();
        scanner.setBasedir( src );
        scanner.setExcludes( (String[]) n2aConfig.getIncludes()
        		.toArray( new String[n2aConfig.getIncludes().size()] ) );

        if ( n2aConfig.getExcludes() != null && !n2aConfig.getExcludes().isEmpty() )
        {
            scanner.setIncludes(
                (String[]) n2aConfig.getExcludes().toArray( new String[n2aConfig.getExcludes().size()] ) );
        }

        scanner.addDefaultExcludes();
        scanner.scan();

        String[] includedDirs = scanner.getIncludedDirectories();
        String[] includedFiles = scanner.getIncludedFiles();
        String[] includes = new String[includedDirs.length + includedFiles.length];

        // dirs and files union using arraycopy
        System.arraycopy(includedDirs, 0, includes, 0, includedDirs.length);
        System.arraycopy(includedFiles, 0, includes,includedDirs.length, includedFiles.length);
        return includes;
    }
}
