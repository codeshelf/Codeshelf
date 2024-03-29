package com.codeshelf.application;

import java.io.File;
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

import org.apache.tomcat.InstanceManager;
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
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.webapp.WebAppContext;
import org.eclipse.jetty.websocket.jsr356.server.deploy.WebSocketServerContainerInitializer;
import org.jminix.console.servlet.MiniConsoleServlet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.health.HealthCheckRegistry;
import com.codahale.metrics.servlets.HealthCheckServlet;
import com.codahale.metrics.servlets.MetricsServlet;
import com.codahale.metrics.servlets.PingServlet;
import com.codeshelf.device.CsDeviceManager;
import com.codeshelf.device.RadioServlet;
import com.codeshelf.metrics.MetricsService;
import com.codeshelf.metrics.ServiceStatusHealthCheck;
import com.codeshelf.security.AuthFilter;
import com.codeshelf.security.AuthzFilter;
import com.codeshelf.ws.server.CsServerEndPoint;
import com.google.inject.Inject;
import com.google.inject.servlet.GuiceFilter;
import com.sun.jersey.api.core.PackagesResourceConfig;
import com.sun.jersey.api.core.ResourceConfig;
import com.sun.jersey.spi.container.servlet.ServletContainer;

public class WebApiServer {

	private static final Logger	LOGGER	= LoggerFactory.getLogger(WebApiServer.class);

	private Server server;

	@Inject
	public WebApiServer() {
		this.server = new Server();
	}

	public final void start(int port, CsDeviceManager deviceManager, CodeshelfApplication application) {
		boolean enableSchemaManagement = Boolean.getBoolean("adminserver.schemamanagement");
		boolean enableApi = Boolean.getBoolean("adminserver.api.enable");
		boolean enableManagerApi = Boolean.getBoolean("adminserver.manager.enable");
		boolean enableJMX = Boolean.getBoolean("adminserver.jmx.enable");
		boolean enableWebSockets = Boolean.getBoolean("adminserver.websockets.enable");
		String staticContentPath = System.getProperty("webapp.content.path");

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

				if(enableManagerApi) {
					// manager API
					contexts.addHandler(this.createManagerApiHandler());
					contexts.addHandler(this.createAuthHandler());
				}

				if(enableApi) {
					// rest API
					// needs a better name
					contexts.addHandler(this.createRestApiHandler());
				}

				if(enableWebSockets) {
			        // websocket API
					ServletContextHandler wscontext = new ServletContextHandler(ServletContextHandler.SESSIONS);
					wscontext.setContextPath("/ws");
					contexts.addHandler(wscontext);
			        ServerContainer wscontainer = WebSocketServerContainerInitializer.configureContext(wscontext);
			        wscontainer.addEndpoint(CsServerEndPoint.class);
				}

				if(enableJMX) {
					contexts.addHandler(createJMXHandler());
				}
		        // embedded static content web server (enabled in dev environment only)
		        if(staticContentPath != null) {
					ResourceHandler resourceHandler = new ResourceHandler();
					resourceHandler.setDirectoriesListed(false);
					resourceHandler.setWelcomeFiles(new String[] { "index.html", "codeshelf.html" });
					resourceHandler.setResourceBase(staticContentPath);
					ContextHandler resourceContextHandler=new ContextHandler("/");
					resourceContextHandler.setHandler(resourceHandler);
					contexts.addHandler(resourceContextHandler);				
		        }
			} else { //site controller
				contexts.addHandler(this.createWorkerSimApiHandler(deviceManager));
			}

			server.start();
		}
		catch (Exception e) {
			LOGGER.error("Failed to start admin server on port:" + port, e);
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
    
	private Handler createAdminApiHandler(CsDeviceManager deviceManager, CodeshelfApplication application, boolean enableSchemaManagement) throws FileNotFoundException, URISyntaxException {
		ServletContextHandler contextHandler = new ServletContextHandler(ServletContextHandler.SESSIONS);
		
		contextHandler.setContextPath("/adm");
		
		// add metrics servlet
		MetricRegistry metricsRegistry = MetricsService.getInstance().getMetricsRegistry();
		if(metricsRegistry != null) { // skip if using dummy metrics service
			contextHandler.setAttribute(MetricsServlet.METRICS_REGISTRY, metricsRegistry);
			contextHandler.addServlet(new ServletHolder(new MetricsServlet()),"/metrics");
		}

		// add health check servlet
		HealthCheckRegistry hcReg = MetricsService.getInstance().getHealthCheckRegistry();
		if(hcReg != null) { // skip if using dummy metrics service
			contextHandler.setAttribute(HealthCheckServlet.HEALTH_CHECK_REGISTRY, hcReg);
			contextHandler.addServlet(new ServletHolder(new HealthCheckServlet()),"/healthchecks");
		}

		// + default app service health check
		ServiceStatusHealthCheck svcCheck = new ServiceStatusHealthCheck();
		MetricsService.getInstance().registerHealthCheck(svcCheck);

		// add ping servlet
		contextHandler.addServlet(new ServletHolder(new PingServlet()),"/ping");

		// log level runtime change servlet
		contextHandler.addServlet(new ServletHolder(new LoggingServlet()),"/loglevel");
		
		if(application != null) {
			// service control (stop service etc)
			contextHandler.addServlet(new ServletHolder(new ServiceControlServlet(application, enableSchemaManagement)),"/service");
		}

		if (deviceManager != null) {
			//////////////////////////
			// only for site controller
			//////////////////////////

			// radio packet capture interface
			contextHandler.addServlet(new ServletHolder(new RadioServlet(deviceManager)),"/radio");
		} 
		else {
			//////////////////////////
			// only for server
			//////////////////////////

			// admin JSP handler
			try {
				final String WEBROOT_INDEX = "/web/";
		        URL indexUri = this.getClass().getResource(WEBROOT_INDEX);
		        if (indexUri == null) {
		            throw new FileNotFoundException("Unable to find resource " + WEBROOT_INDEX);
		        }
		        URI baseUri = indexUri.toURI();
		        ClassLoader jspClassLoader = new URLClassLoader(new URL[0], this.getClass().getClassLoader());
		        File tempDir = new File(System.getProperty("java.io.tmpdir"));	        
		        contextHandler.setResourceBase(baseUri.toASCIIString());
		        contextHandler.setAttribute("org.eclipse.jetty.containerInitializers", jspInitializers());
		        contextHandler.setAttribute("javax.servlet.context.tempdir", tempDir);
		        contextHandler.setAttribute(InstanceManager.class.getName(), new SimpleInstanceManager());
		        contextHandler.addBean(new ServletContainerInitializersStarter(new WebAppContext()), true);
		        contextHandler.setClassLoader(jspClassLoader);
		        contextHandler.addServlet(jspServletHolder(), "*.jsp");
			}
			catch (Exception e) {
				LOGGER.error("Failed to start admin JSP servlet",e);
			}
		}		

		return contextHandler;
	}

	private Handler createRestApiHandler() {
		ServletContextHandler restApiContext = new ServletContextHandler(ServletContextHandler.SESSIONS); // why sessions? -ivan
		restApiContext.setContextPath("/api");
		FilterHolder jerseyGuiceFilter = new FilterHolder(new GuiceFilter());
		restApiContext.addFilter(CORSFilter.class, "/*", EnumSet.allOf(DispatcherType.class));
		restApiContext.addFilter(AuthFilter.class, "/*", EnumSet.allOf(DispatcherType.class));
		restApiContext.addFilter(APICallFilter.class, "/*", EnumSet.allOf(DispatcherType.class));
		restApiContext.addFilter(TransactionFilter.class , "/*", EnumSet.allOf(DispatcherType.class));
		restApiContext.addFilter(jerseyGuiceFilter , "/*", EnumSet.allOf(DispatcherType.class));
		restApiContext.addServlet(DefaultServlet.class, "/");  //filter needs to front an actual servlet so put a basic servlet in place
		return restApiContext;
	}
	
	private Handler createWorkerSimApiHandler(CsDeviceManager deviceManager) {
		ServletContextHandler workerSimContext = new ServletContextHandler(ServletContextHandler.NO_SESSIONS);
		workerSimContext.setContextPath("/sim");
		ResourceConfig rc = new PackagesResourceConfig("com.codeshelf.sim.worker.api");
		ServletContainer container = new ServletContainer(rc);
		workerSimContext.addServlet(new ServletHolder(container), "/*");
		return workerSimContext;
	}


	
	private Handler createManagerApiHandler() {
		ServletContextHandler context = new ServletContextHandler(ServletContextHandler.NO_SESSIONS);
		
		context.setContextPath("/mgr");
		// can't seem to inject both APIs in different handlers, Guice gets confused.. hm
		//FilterHolder jerseyGuiceFilter = new FilterHolder(new GuiceFilter());
		context.addFilter(CORSFilter.class, "/*", EnumSet.allOf(DispatcherType.class));
		context.addFilter(AuthFilter.class, "/*", EnumSet.allOf(DispatcherType.class));
		context.addFilter(APICallFilter.class, "/*", EnumSet.allOf(DispatcherType.class));
		//context.addFilter(jerseyGuiceFilter , "/*", EnumSet.allOf(DispatcherType.class));
		//context.addServlet(DefaultServlet.class, "/");  //filter needs to front an actual servlet so put a basic servlet in place

		ResourceConfig rc = new PackagesResourceConfig("com.codeshelf.manager.api");
		String[] filters = new String[]{"org.secnod.shiro.jersey.ShiroResourceFilterFactory"};
		rc.getProperties().put(ResourceConfig.PROPERTY_RESOURCE_FILTER_FACTORIES, filters);
		ServletContainer container = new ServletContainer(rc);
		context.addServlet(new ServletHolder(container), "/*");
		return context;
	}
	
	private Handler createJMXHandler() {
		ServletContextHandler context = new ServletContextHandler(ServletContextHandler.NO_SESSIONS);
		
		context.setContextPath("/jmx");
		// can't seem to inject both APIs in different handlers, Guice gets confused.. hm
		//FilterHolder jerseyGuiceFilter = new FilterHolder(new GuiceFilter());
		context.addFilter(CORSFilter.class, "/*", EnumSet.allOf(DispatcherType.class));
		context.addFilter(AuthFilter.class, "/*", EnumSet.allOf(DispatcherType.class));
		FilterHolder authzFilter = new FilterHolder(AuthzFilter.class);
		authzFilter.setInitParameter("permission", "jmx:edit");
		context.addFilter(authzFilter, "/*", EnumSet.allOf(DispatcherType.class));
		context.addServlet(new ServletHolder(new MiniConsoleServlet()), "/*");
		return context;
	}
	
	private Handler createAuthHandler() {
		ServletContextHandler context = new ServletContextHandler(ServletContextHandler.NO_SESSIONS);		
		context.setContextPath("/auth");
		context.addFilter(APICallFilter.class, "/*", EnumSet.allOf(DispatcherType.class));
		context.addFilter(CORSFilter.class, "/*", EnumSet.allOf(DispatcherType.class));

		ResourceConfig rc = new PackagesResourceConfig("com.codeshelf.security.api");
		ServletContainer container = new ServletContainer(rc);
		context.addServlet(new ServletHolder(container), "/*");
		return context;
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
