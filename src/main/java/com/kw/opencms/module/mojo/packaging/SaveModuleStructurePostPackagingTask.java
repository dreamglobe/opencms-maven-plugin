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

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

import com.kw.opencms.module.mojo.util.ModuleStructureSerializer;

/**
 * Saves the webapp structure cache.
 *
 * @author Stephane Nicoll
 * 
 * @version $Id: SaveWebappStructurePostPackagingTask.java 659222 2008-05-22 19:33:48Z olamy $
 */
public class SaveModuleStructurePostPackagingTask
    implements ModulePostPackagingTask
{

    private final File targetFile;

    private final ModuleStructureSerializer serialier;


    public SaveModuleStructurePostPackagingTask( File targetFile )
    {
        this.targetFile = targetFile;
        this.serialier = new ModuleStructureSerializer();
    }

    public void performPostPackaging( ModulePackagingContext context )
        throws MojoExecutionException, MojoFailureException
    {
        if ( targetFile == null )
        {
            context.getLog().debug( "Cache usage is disabled, not saving webapp structure." );
        }
        else
        {
            try
            {
                serialier.toXml( context.getModuleStructure(), targetFile );
                context.getLog().debug( "Cache saved successfully." );
            }
            catch ( IOException e )
            {
                throw new MojoExecutionException( "Could not save webapp structure", e );
            }
        }
    }
}
