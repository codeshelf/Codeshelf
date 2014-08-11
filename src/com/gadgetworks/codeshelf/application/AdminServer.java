package com.gadgetworks.codeshelf.application;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.servlets.MetricsServlet;
import com.gadgetworks.codeshelf.metrics.MetricsService;

public class AdminServer {

	private static final Logger	LOGGER	= LoggerFactory.getLogger(AdminServer.class);

	Server server;
	
	int port = 8088;
	
	public AdminServer() {
	}
	
	public final void startServer() {
		try {
			MetricRegistry metricsRegistry = MetricsService.getRegistry();
			Server server = new Server(port);
			ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
			context.setContextPath("/");
			context.setAttribute(MetricsServlet.METRICS_REGISTRY, metricsRegistry);
			server.setHandler(context);
			context.addServlet(new ServletHolder(new MetricsServlet()),"/*");
			server.start();
			// server.join();
		} 
		catch (Exception e) {
			LOGGER.error("Failed to start admin server", e);
		}
	}
	
	public final void stopServer() {
		try {		
			server.stop();
		} 
		catch (Exception e) {
			LOGGER.error("Failed to stop admin server", e);
		}	
	}

}
