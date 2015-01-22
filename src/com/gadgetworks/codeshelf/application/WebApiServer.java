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
import com.gadgetworks.codeshelf.metrics.DatabaseConnectionHealthCheck;
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

				// admin JSP handler
				contexts.addHandler(this.createAdminJspHandler());

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
	
	private Handler createAdminJspHandler() throws FileNotFoundException, URISyntaxException {
		// com.gadgetworks.codeshelf.platform.performance.web
		final String WEBROOT_INDEX = "/com/gadgetworks/codeshelf/web/";
        URL indexUri = this.getClass().getResource(WEBROOT_INDEX);
        if (indexUri == null) {
            throw new FileNotFoundException("Unable to find resource " + WEBROOT_INDEX);
        }
        URI baseUri = indexUri.toURI();

        WebAppContext context = new WebAppContext();
        context.setContextPath("/web");
        // context.setAttribute("javax.servlet.context.tempdir", scratchDir);
        //context.setAttribute("org.eclipse.jetty.server.webapp.ContainerIncludeJarPattern",
        //  ".*/[^/]*servlet-api-[^/]*\\.jar$|.*/javax.servlet.jsp.jstl-.*\\.jar$|.*/.*taglibs.*\\.jar$");
        context.setResourceBase(baseUri.toASCIIString());
        context.setAttribute("org.eclipse.jetty.containerInitializers", jspInitializers());
        context.addBean(new ServletContainerInitializersStarter(context), true);
        context.setClassLoader(getUrlClassLoader());

        context.addServlet(jspServletHolder(), "*.jsp");
        context.addServlet(defaultServletHolder(baseUri), "/");
        return context;
	}
	
	private ServletHolder defaultServletHolder(URI baseUri) {
        ServletHolder holderDefault = new ServletHolder("default", DefaultServlet.class);
        holderDefault.setInitParameter("resourceBase", baseUri.toASCIIString());
        return holderDefault;
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
    
    private ClassLoader getUrlClassLoader() {
        ClassLoader jspClassLoader = new URLClassLoader(new URL[0], this.getClass().getClassLoader());
        return jspClassLoader;
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
		// + default app service health check
		ServiceStatusHealthCheck svcCheck = new ServiceStatusHealthCheck();
		MetricsService.registerHealthCheck(svcCheck);

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
		FilterHolder jerseyGuiceFilter = new FilterHolder(new GuiceFilter());
		restApiContext.addFilter(jerseyGuiceFilter , "/*", EnumSet.allOf(DispatcherType.class));
		restApiContext.addServlet(DefaultServlet.class, "/");  //filter needs front an actual servlet so put a basic servlet in place


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
