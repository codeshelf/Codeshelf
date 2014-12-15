package com.gadgetworks.codeshelf.application;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jersey.spi.container.servlet.ServletContainer;

public class ApiServer {

	private static final Logger	LOGGER	= LoggerFactory.getLogger(ApiServer.class);

	Server server;
	
	public final void startServer() {
		LOGGER.info("Starting Admin Server");
		
		try {
			Integer port = Integer.getInteger("apiserver.port");
			if(port == null) {
				LOGGER.error("Could not start api server, apiserver.port needs to be specified");
				return;
			}
			
			Server server = new Server(port);
			ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
			context.setContextPath("/*");
			server.setHandler(context);
			
			ServletHolder sh = new ServletHolder(ServletContainer.class);
			sh.setInitParameter("com.sun.jersey.config.property.resourceConfigClass", "com.sun.jersey.api.core.PackagesResourceConfig");
	        sh.setInitParameter("com.sun.jersey.config.property.packages", "com.gadgetworks.codeshelf.application.apiresources");
	        sh.setInitParameter("com.sun.jersey.api.json.POJOMappingFeature", "true");
			context.addServlet(sh, "/api/*");

			server.start();
		} catch (Exception e) {
			LOGGER.error("Failed to start api server", e);
		}
	}
	
	public final void stopServer() {
		try {		
			server.stop();
		} 
		catch (Exception e) {
			LOGGER.error("Failed to stop api server", e);
		}	
	}

}