package com.kw.opencms.module.mojo.util;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;

import org.apache.maven.model.Dependency;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.ReaderFactory;
import org.codehaus.plexus.util.WriterFactory;

import com.thoughtworks.xstream.XStream;

/**
 * Serializes {@link ModuleStructure} back and forth.
 *
 * @author Stephane Nicoll
 * @version $Id: WebappStructureSerializer.java 682914 2008-08-05 20:03:53Z hboutemy $
 */
public class ModuleStructureSerializer
{

    private final XStream xStream;

    /**
     * Creates a new instance of the serializer.
     */
    public ModuleStructureSerializer()
    {
        this.xStream = new XStream();

        // Register aliases
        xStream.alias( "module-structure", ModuleStructure.class );
        xStream.alias( "path-set", PathSet.class );
        xStream.alias( "dependency", Dependency.class );
    }


    /**
     * Reads the {@link ModuleStructure} from the specified file.
     *
     * @param file the file containing the module structure
     * @return the module structure
     * @throws IOException if an error occurred while reading the structure
     */
    public ModuleStructure fromXml( File file )
        throws IOException
    {
        Reader reader = null;

        try
        {
            reader = ReaderFactory.newXmlReader( file );
            return (ModuleStructure) xStream.fromXML( reader );
        }
        finally
        {
            IOUtil.close( reader );
        }
    }

    /**
     * Saves the {@link ModuleStructure} to the specified file.
     *
     * @param moduleStructure the structure to save
     * @param targetFile      the file to use to save the structure
     * @throws IOException if an error occurred while saving the webapp structure
     */
    public void toXml( ModuleStructure moduleStructure, File targetFile )
        throws IOException
    {
        Writer writer = null;
        try
        {
            if ( !targetFile.getParentFile().exists() && !targetFile.getParentFile().mkdirs() )
            {
                throw new IOException(
                    "Could not create parent[" + targetFile.getParentFile().getAbsolutePath() + "]" );
            }

            if ( !targetFile.exists() && !targetFile.createNewFile() )
            {
                throw new IOException( "Could not create file[" + targetFile.getAbsolutePath() + "]" );
            }
            writer = WriterFactory.newXmlWriter( targetFile );
            xStream.toXML( moduleStructure, writer );
        }
        finally
        {
            IOUtil.close( writer );
        }
    }
}
