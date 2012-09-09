package com.kw.opencms.module.mojo.util;


import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.util.Iterator;
import java.util.StringTokenizer;

public class ManifestUtils {
	static Charset utf8charset = Charset.forName("UTF-8");
	static Charset iso88591charset = Charset.forName("ISO-8859-1");


	public static String readFileAsString(String filePath)
	throws java.io.IOException{
		return readFileAsString(new File( filePath ));
	}

	public static String readFileAsString(File filePath)
	throws java.io.IOException{
        StringBuilder fileData = new StringBuilder(1000);
        BufferedReader reader = new BufferedReader(
                new FileReader(filePath));
        char[] buf = new char[1024];
        int numRead=0;
        while((numRead=reader.read(buf)) != -1){
            fileData.append(buf, 0, numRead);
        }
        reader.close();
        return fileData.toString();
    }

	public static void writeFileAsString(String filePath, String content)
    throws java.io.IOException{
        BufferedWriter writer = new BufferedWriter(
                new FileWriter(filePath));
        writer.write(content);
        writer.close();

    }

	public static void writeFileAsString(File file, String content)
    throws java.io.IOException{
        BufferedWriter writer = new BufferedWriter(
                new FileWriter(file));
        writer.write(content);
        writer.close();

    }

	public static String readFileAsStringNoException(String filePath){
		return readFileAsStringNoException( new File(filePath) );
	}

	public static String readFileAsStringNoException(File filePath){
        StringBuilder fileData = new StringBuilder(1000);
        BufferedReader reader;
		try {
			reader = new BufferedReader(
			        new FileReader(filePath));
		} catch (FileNotFoundException e) {
			return null;
		}
        char[] buf = new char[1024];
        int numRead=0;
        try {
			while((numRead=reader.read(buf)) != -1){
			    fileData.append(buf, 0, numRead);
			}

		} catch (IOException e) {
			return "";
		}finally{
			try{
				reader.close();
			}catch(IOException e){
			}
		}
        return fileData.toString();
    }

	public static char[] readFileAsArrayEncodingNoException(String filePath, String encoding){
		File file = new File(filePath);
		StringBuilder fileData = new StringBuilder(filePath.length());
        BufferedReader breader;

		try {
			breader = new BufferedReader(new InputStreamReader(
					new FileInputStream(file), encoding));
		} catch (FileNotFoundException e) {
			return null;
		} catch (UnsupportedEncodingException e) {
			return null;
		}
        char[] buf = new char[1024];
        int numRead=0;
        try {
			while((numRead=breader.read(buf)) != -1){
			    fileData.append(buf, 0, numRead);
			}

		} catch (IOException e) {
			return null;
		}finally{
			try{
				breader.close();
			}catch(IOException e){
			}
		}
        return fileData.toString().toCharArray();
    }

	public static void copyFile(File sourceFile, File destFile)
			throws IOException {
		if (!destFile.exists()) {
			destFile.createNewFile();
		}

		FileChannel source = null;
		FileChannel destination = null;
		try {
			source = new FileInputStream(sourceFile).getChannel();
			destination = new FileOutputStream(destFile).getChannel();
			destination.transferFrom(source, 0, source.size());
		} finally {
			if (source != null) {
				source.close();
			}
			if (destination != null) {
				destination.close();
			}
		}
	}

	public static String capitalizeFirstLettersTokenizer ( String s ) {

	    final StringTokenizer st = new StringTokenizer( s, " ", true );
	    final StringBuilder sb = new StringBuilder();

	    while ( st.hasMoreTokens() ) {
	        String token = st.nextToken();
	        token = String.format( "%s%s",
	                                Character.toUpperCase(token.charAt(0)),
	                                token.substring(1) );
	        sb.append( token );
	    }

	    return sb.toString();

	}

	public static String capitalizeFirstLetters ( String s ) {

	    for (int i = 0; i < s.length(); i++) {

	        if (i == 0) {
	            // Capitalize the first letter of the string.
	            s = String.format( "%s%s",
	                         Character.toUpperCase(s.charAt(0)),
	                         s.substring(1) );
	        }

	        // Is this character a non-letter or non-digit?  If so
	        // then this is probably a word boundary so let's capitalize
	        // the next character in the sequence.
	        if (!Character.isLetterOrDigit(s.charAt(i))) {
	            if (i + 1 < s.length()) {
	                s = String.format( "%s%s%s",
	                             s.subSequence(0, i+1),
	                             Character.toUpperCase(s.charAt(i + 1)),
	                             s.substring(i+2) );
	            }
	        }

	    }

	    return s;

	}

	public static boolean isNotNullOrZero(Object b) {
		return b!=null && b.toString().length()>0;
	}

	public static InputStream readUTF8toLantin1(String utfcontentPath) throws IOException{

		ByteBuffer inputBuffer = ByteBuffer.wrap(
				readFileAsString(utfcontentPath).getBytes());

		// decode UTF-8
		CharBuffer data = utf8charset.decode(inputBuffer);

		// encode ISO-8559-1
		ByteBuffer outputBuffer = iso88591charset.encode(data);
		//byte[] outputData = outputBuffer.array();
		return new ByteArrayInputStream(outputBuffer.array());
	}

	public static String join(Iterable< ? extends Object > pColl, String separator )
	{
	    Iterator< ? extends Object > oIter;
	    if ( pColl == null || ( !( oIter = pColl.iterator() ).hasNext() ) )
	        return "";
	    StringBuilder oBuilder = new StringBuilder( String.valueOf( oIter.next() ) );
	    while ( oIter.hasNext() )
	        oBuilder.append( separator ).append( oIter.next() );
	    return oBuilder.toString();
	}

}

