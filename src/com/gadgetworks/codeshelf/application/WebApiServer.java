package com.gadgetworks.codeshelf.application;

import javax.websocket.server.ServerContainer;

import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.NetworkTrafficServerConnector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.websocket.jsr356.server.deploy.WebSocketServerContainerInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.health.HealthCheckRegistry;
import com.codahale.metrics.servlets.HealthCheckServlet;
import com.codahale.metrics.servlets.MetricsServlet;
import com.codahale.metrics.servlets.PingServlet;
import com.gadgetworks.codeshelf.device.ICsDeviceManager;
import com.gadgetworks.codeshelf.device.RadioServlet;
import com.gadgetworks.codeshelf.metrics.MetricsService;
import com.gadgetworks.codeshelf.ws.jetty.server.CsServerEndPoint;
import com.sun.jersey.spi.container.servlet.ServletContainer;

public class WebApiServer {

	private static final Logger	LOGGER	= LoggerFactory.getLogger(WebApiServer.class);

	Server server = new Server();
	
	public WebApiServer() {		
	}
	
	public final void start(int port, ICsDeviceManager deviceManager, ApplicationABC application, boolean enableSchemaManagement, String staticContentPath) {
		try {	
			NetworkTrafficServerConnector connector = new NetworkTrafficServerConnector(server);
			connector.setPort(port);
		    server.addConnector(connector);		
			
		    ContextHandlerCollection contexts = new ContextHandlerCollection();
		    server.setHandler(contexts);

			contexts.addHandler(createAdminApiHandler(deviceManager,application,enableSchemaManagement));
			
			if(deviceManager == null) {
				// server only:
				
				// rest API
				contexts.addHandler(this.createRestApiHandler());
				
		        // websocket API
				ServletContextHandler wscontext = new ServletContextHandler(ServletContextHandler.SESSIONS);
				wscontext.setContextPath("/ws");
				contexts.addHandler(wscontext);
		        ServerContainer wscontainer = WebSocketServerContainerInitializer.configureContext(wscontext);
		        wscontainer.addEndpoint(CsServerEndPoint.class);
		        
		        // embedded static content web server
				ResourceHandler resourceHandler = new ResourceHandler();
				resourceHandler.setDirectoriesListed(false);
				resourceHandler.setWelcomeFiles(new String[] { "codeshelf.html" });
				resourceHandler.setResourceBase(staticContentPath);								
				ContextHandler resourceContextHandler=new ContextHandler("/");
				resourceContextHandler.setHandler(resourceHandler);
				contexts.addHandler(resourceContextHandler);
			}
		    
			server.start();
		} 
		catch (Exception e) {
			LOGGER.error("Failed to start admin server", e);
		}
	}
	
	private Handler createAdminApiHandler(ICsDeviceManager deviceManager, ApplicationABC application, boolean enableSchemaManagement) {
		ServletContextHandler adminApiContext = new ServletContextHandler(ServletContextHandler.SESSIONS);
		adminApiContext.setContextPath("/adm");
		
		// add metrics servlet
		MetricRegistry metricsRegistry = MetricsService.getRegistry();
		adminApiContext.setAttribute(MetricsServlet.METRICS_REGISTRY, metricsRegistry);
		adminApiContext.addServlet(new ServletHolder(new MetricsServlet()),"/metrics");

		// add health check servlet
		HealthCheckRegistry hcReg = MetricsService.getHealthCheckRegistry();
		adminApiContext.setAttribute(HealthCheckServlet.HEALTH_CHECK_REGISTRY, hcReg);
		adminApiContext.addServlet(new ServletHolder(new HealthCheckServlet()),"/healthchecks");
		
		// add ping servlet
		adminApiContext.addServlet(new ServletHolder(new PingServlet()),"/ping");
		
		// log level runtime change servlet
		adminApiContext.addServlet(new ServletHolder(new LoggingServlet()),"/loglevel");

		// service control (stop service etc)
		adminApiContext.addServlet(new ServletHolder(new ServiceControlServlet(application, enableSchemaManagement)),"/service");

		if(deviceManager != null) {
			// only for site controller
			
			// radio packet capture interface
			adminApiContext.addServlet(new ServletHolder(new RadioServlet(deviceManager)),"/radio");
		} else {
			// only for server
			
			// user manager sql generator (temporary)
			adminApiContext.addServlet(new ServletHolder(new UsersServlet()),"/users");

		}
		
		return adminApiContext;
	}
	
	private Handler createRestApiHandler() {
		ServletContextHandler restApiContext = new ServletContextHandler(ServletContextHandler.SESSIONS);
		restApiContext.setContextPath("/api");
		
		// REST API for UI
		ServletHolder sh = new ServletHolder(ServletContainer.class);
		sh.setInitParameter("com.sun.jersey.config.property.resourceConfigClass", "com.sun.jersey.api.core.PackagesResourceConfig");
        sh.setInitParameter("com.sun.jersey.config.property.packages", "com.gadgetworks.codeshelf.api.resources");
        sh.setInitParameter("com.sun.jersey.api.json.POJOMappingFeature", "true");
        restApiContext.addServlet(sh, "/*");
		
		return restApiContext;
	}
	
	public final void stop() {
		try {		
			server.stop();
		} 
		catch (Exception e) {
			LOGGER.error("Failed to stop admin server", e);
		}	
	}

}
