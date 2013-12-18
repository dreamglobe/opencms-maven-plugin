package com.kw.opencms.module.mojo;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Properties;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.settings.Server;

/**
 * Only applicable to Apache Tomcat Server with manager application installed. 
 * This goal restart the OpenCms application identified by ${opencms.webapp.name} property.
 *    
 * @goal reload-app
 * @requiresProject false
 */
public class OpenCmsTomcatReloadAppMojo extends AbstractModuleMojo {

	/**
	 * @parameter property="tomcat.manager.url" default-value="http://localhost:8080/manager/"
	 */
	private URL urlManager;

	/**
	 * Take the credentials from a server definition in settings.xml.
	 * @parameter property="app.server.id"
	 */
	private String appServerAuthId;

	/**
	 * User that installs the module on the OpenCms instance.
	 * @parameter property="server.user.name"
	 */
	protected String appServerUserName;

	/**
	 * Credentials for the OpenCms user.
	 * @parameter property="server.user.pass"
	 */
	protected String appServerUserPass;


	/**
	 * Punto de entrada del mojo
	 */
	public void execute() throws MojoExecutionException, MojoFailureException
	{
		// configuracion
    	fillTomcaCredentials();
    	checkConditions();

    	// calcula la url de lo operacion
    	String opUrl = urlManager.toString().concat("/reload?path=".concat(openCmsWebappName));
    	getLog().debug("URL de operación: " + opUrl);

    	try
    	{
    		// obtiene cliente configurado (seguridad) y método get de la operación
    		DefaultHttpClient httpclient = new DefaultHttpClient();
    		httpclient.getCredentialsProvider().setCredentials(
                    new AuthScope(urlManager.getHost(), urlManager.getPort()),
                    new UsernamePasswordCredentials(appServerUserName, appServerUserPass));
    		HttpGet  getMethod = new HttpGet( opUrl );
    		getLog().info("Invoking url: " + opUrl);
    		// realiza la invocación y comprueba la respuesta
	    	HttpResponse response = httpclient.execute(getMethod);

	    	if( response.getStatusLine().getStatusCode() != 200 )
	    	{
	    		getLog().error("Error response form manager: " + response.getStatusLine());
	    	}
	    	HttpEntity entity = response.getEntity();

	        // If the response does not enclose an entity, there is no need
	        // to bother about connection release
	        if (entity != null) {
	            BufferedReader reader = new BufferedReader(
	                    new InputStreamReader(entity.getContent()));
	            try {

	                // do something useful with the response
	               getLog().info(reader.readLine());

	            } catch (IOException ex) {

	                // In case of an IOException the connection will be released
	                // back to the connection manager automatically
	            	throw new MojoFailureException(
	    					"Error en la lectura/escritura", ex);

	            } catch (RuntimeException ex) {

	                // In case of an unexpected exception you may want to abort
	                // the HTTP request in order to shut down the underlying
	                // connection and release it back to the connection manager.
	            	getMethod.abort();
	                throw ex;

	            } finally {

	                // Closing the input stream will trigger connection release
	                reader.close();
	                
	                // When HttpClient instance is no longer needed,
	    	        // shut down the connection manager to ensure
	    	        // immediate deallocation of all system resources
	                httpclient.getConnectionManager().shutdown();
	            }
	        }
		} catch (IOException ex) {

            // In case of an IOException the connection will be released
            // back to the connection manager automatically
        	throw new MojoFailureException(
					"Error en la lectura/escritura", ex);

        }
	}

	protected boolean checkPackaging()
	{
		if( PACKAGING_OPENCMS_MODULE.equals(getProject().getPackaging()) )
		{
			return false;
		}
		return true;
	}

	protected void fillTomcaCredentials()
	{
		if( appServerAuthId != null)
    	{
    		Server serverAuth = getCredentialsFromServer(appServerAuthId);
    		if(serverAuth != null)
    		{
    			appServerUserName = serverAuth.getUsername();
    			appServerUserPass = serverAuth.getPassword();
    		}
    	}
	}

	protected void checkConditions() throws MojoExecutionException
	{
		boolean fail = false;
		String msg = null;
		if(appServerUserName==null || appServerUserPass == null)
		{
			fail = true;
			msg = "Tomcat server credentials ('server.user.name'/'server.user.pass') missing!";
		}

		if( urlManager==null )
		{
			fail = true;
			msg = "Tomcat manager URL missing or incorrect!";
		}

		// check the flag to mark the cancel of reload
		Properties properties = getProject().getProperties();
		if(properties.contains("tomcat.reload")
				&& properties.get("tomcat.reload").equals("false"))
		{
			fail = true;
			msg = "Tomcat reload cancelled previouslly.";
			if(properties.contains("tomcat.reload.comment"))
			{
				msg = "Tomcat reload cancelled: " +
				properties.get("tomcat.reload.comment");
			}
		}

		if( openCmsWebappName == null )
		{
			getLog().warn("Application context not selected: using ROOT by default!");
			openCmsWebappName = "/";
		}
		else if( !openCmsWebappName.startsWith( "/" ) )
		{
			openCmsWebappName = "/".concat(openCmsWebappName);
		}

		if(fail)
		{
			throw new MojoExecutionException(msg);
		}
	}
}