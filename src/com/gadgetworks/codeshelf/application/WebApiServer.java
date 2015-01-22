package com.gadgetworks.codeshelf.application;

import java.io.FileNotFoundException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import javax.servlet.DispatcherType;
import javax.websocket.server.ServerContainer;

import org.apache.tomcat.SimpleInstanceManager;
import org.eclipse.jetty.annotations.ServletContainerInitializersStarter;
import org.eclipse.jetty.apache.jsp.JettyJasperInitializer;
import org.eclipse.jetty.jsp.JettyJspServlet;
import org.eclipse.jetty.plus.annotation.ContainerInitializer;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.NetworkTrafficServerConnector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletContextHandler.Context;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.webapp.WebAppClassLoader;
import org.eclipse.jetty.webapp.WebAppContext;
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
import com.gadgetworks.codeshelf.metrics.ServiceStatusHealthCheck;
import com.gadgetworks.codeshelf.ws.jetty.server.CsServerEndPoint;
import com.google.inject.Inject;
import com.google.inject.servlet.GuiceFilter;

public class WebApiServer {

	private static final Logger	LOGGER	= LoggerFactory.getLogger(WebApiServer.class);

	private Server server;

	@Inject
	public WebApiServer() {
		this.server = new Server();
	}

	public final void start(int port, ICsDeviceManager deviceManager, ApplicationABC application, boolean enableSchemaManagement, String staticContentPath) {
		try {
			NetworkTrafficServerConnector connector = new NetworkTrafficServerConnector(server);
			connector.setPort(port);
		    server.addConnector(connector);

		    ContextHandlerCollection contexts = new ContextHandlerCollection();
		    server.setHandler(contexts);

			contexts.addHandler(createAdminApiHandler(deviceManager,application,enableSchemaManagement));

			// bhe: would be preferable to define process roles and work off them, instead of guessing 
			// what kind of role it is.  it could be tied to the application object.
			if (deviceManager == null) {
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
				resourceHandler.setWelcomeFiles(new String[] { "codeshelf.html", "index.html" });
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

	private List<ContainerInitializer> jspInitializers() {
    	JettyJasperInitializer sci = new JettyJasperInitializer();
        ContainerInitializer initializer = new ContainerInitializer(sci, null);
        List<ContainerInitializer> initializers = new ArrayList<ContainerInitializer>();
        initializers.add(initializer);
        return initializers;
	}
	
    private ServletHolder jspServletHolder() {
        ServletHolder holderJsp = new ServletHolder("jsp", JettyJspServlet.class);
        holderJsp.setInitOrder(0);
        holderJsp.setInitParameter("logVerbosityLevel", "DEBUG");
        holderJsp.setInitParameter("fork", "false");
        holderJsp.setInitParameter("xpoweredBy", "false");
        holderJsp.setInitParameter("compilerTargetVM", "1.7");
        holderJsp.setInitParameter("compilerSourceVM", "1.7");
        holderJsp.setInitParameter("keepgenerated", "true");
        return holderJsp;
    }
    
	private Handler createAdminApiHandler(ICsDeviceManager deviceManager, ApplicationABC application, boolean enableSchemaManagement) throws FileNotFoundException, URISyntaxException {
		ServletContextHandler contextHandler = new ServletContextHandler(ServletContextHandler.SESSIONS);
		
		contextHandler.setContextPath("/adm");
		
		// add metrics servlet
		MetricRegistry metricsRegistry = MetricsService.getRegistry();
		contextHandler.setAttribute(MetricsServlet.METRICS_REGISTRY, metricsRegistry);
		contextHandler.addServlet(new ServletHolder(new MetricsServlet()),"/metrics");

		// add health check servlet
		HealthCheckRegistry hcReg = MetricsService.getHealthCheckRegistry();
		contextHandler.setAttribute(HealthCheckServlet.HEALTH_CHECK_REGISTRY, hcReg);
		contextHandler.addServlet(new ServletHolder(new HealthCheckServlet()),"/healthchecks");
		// + default app service health check
		ServiceStatusHealthCheck svcCheck = new ServiceStatusHealthCheck();
		MetricsService.registerHealthCheck(svcCheck);

		// add ping servlet
		contextHandler.addServlet(new ServletHolder(new PingServlet()),"/ping");

		// log level runtime change servlet
		contextHandler.addServlet(new ServletHolder(new LoggingServlet()),"/loglevel");
		
		// service control (stop service etc)
		contextHandler.addServlet(new ServletHolder(new ServiceControlServlet(application, enableSchemaManagement)),"/service");

		if (deviceManager != null) {
			// only for site controller

			// radio packet capture interface
			contextHandler.addServlet(new ServletHolder(new RadioServlet(deviceManager)),"/radio");
		} 
		else {
			// only for server

			// user manager sql generator (temporary)
			contextHandler.addServlet(new ServletHolder(new UsersServlet()),"/users");

			// admin JSP handler
			final String WEBROOT_INDEX = "/com/gadgetworks/codeshelf/web/";
	        URL indexUri = this.getClass().getResource(WEBROOT_INDEX);
	        if (indexUri == null) {
	            throw new FileNotFoundException("Unable to find resource " + WEBROOT_INDEX);
	        }
	        URI baseUri = indexUri.toURI();
	        ClassLoader jspClassLoader = new URLClassLoader(new URL[0], this.getClass().getClassLoader());
	        
	        contextHandler.setResourceBase(baseUri.toASCIIString());
	        contextHandler.setAttribute("org.eclipse.jetty.containerInitializers", jspInitializers());
	        contextHandler.addBean(new ServletContainerInitializersStarter(new WebAppContext()), true);
	        contextHandler.setClassLoader(jspClassLoader);
	        contextHandler.addServlet(jspServletHolder(), "*.jsp");
		}		

		return contextHandler;
	}

	private Handler createRestApiHandler() {
		ServletContextHandler restApiContext = new ServletContextHandler(ServletContextHandler.SESSIONS);
		restApiContext.setContextPath("/api");
		FilterHolder jerseyGuiceFilter = new FilterHolder(new GuiceFilter());
		restApiContext.addFilter(CORSFilter.class, "/*", EnumSet.allOf(DispatcherType.class));
		restApiContext.addFilter(jerseyGuiceFilter , "/*", EnumSet.allOf(DispatcherType.class));
		restApiContext.addServlet(DefaultServlet.class, "/");  //filter needs to front an actual servlet so put a basic servlet in place
		//restApiContext.setSecurityHandler(createRestApiSecurityHandler());

		return restApiContext;
	}
	
	/*
	private SecurityHandler createRestApiSecurityHandler() {
		ConstraintSecurityHandler security = new ConstraintSecurityHandler();

		
        Constraint constraint = new Constraint();
        constraint.setName("auth");
        constraint.setAuthenticate( true );
        constraint.setRoles(new String[]{"user", "admin"});
 
        // Binds a url pattern with the previously created constraint. The roles for this constraing mapping are
        // mined from the Constraint itself although methods exist to declare and bind roles separately as well.
        ConstraintMapping mapping = new ConstraintMapping();
        mapping.setPathSpec( "/*" );
        mapping.setConstraint( constraint );
 
        // First you see the constraint mapping being applied to the handler as a singleton list,
        // however you can passing in as many security constraint mappings as you like so long as they follow the
        // mapping requirements of the servlet api. Next we set a BasicAuthenticator instance which is the object
        // that actually checks the credentials followed by the LoginService which is the store of known users, etc.
        security.setConstraintMappings(Collections.singletonList(mapping));
		
		HashLoginService loginService = new HashLoginService();
		loginService.update("a@example.com", new Password("testme"), new String[]{"user"});
		security.setLoginService(loginService);

		security.setAuthenticator(new BasicAuthenticator());
		return security;
	}*/

	public final void stop() {
		try {
			server.stop();
		}
		catch (Exception e) {
			LOGGER.error("Failed to stop admin server", e);
		}
	}

}
