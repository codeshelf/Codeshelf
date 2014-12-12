package com.gadgetworks.codeshelf.application;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
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

public class AdminServer {

	private static final Logger	LOGGER	= LoggerFactory.getLogger(AdminServer.class);

	Server server;
	
	public AdminServer() {
	}
	
	public final void startServer(int port, ICsDeviceManager deviceManager) {
		try {
			Server server = new Server(port);
			ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
			context.setContextPath("/*");
			server.setHandler(context);

			// add metrics servlet
			MetricRegistry metricsRegistry = MetricsService.getRegistry();
			context.setAttribute(MetricsServlet.METRICS_REGISTRY, metricsRegistry);
			context.addServlet(new ServletHolder(new MetricsServlet()),"/metrics");

			// add health check servlet
			HealthCheckRegistry hcReg = MetricsService.getHealthCheckRegistry();
			context.setAttribute(HealthCheckServlet.HEALTH_CHECK_REGISTRY, hcReg);
			context.addServlet(new ServletHolder(new HealthCheckServlet()),"/healthchecks");
			
			// add ping servlet
			context.addServlet(new ServletHolder(new PingServlet()),"/ping");
			
			// log level runtime change servlet
			context.addServlet(new ServletHolder(new LoggingServlet()),LoggingServlet.urlPath);
			
			if(deviceManager != null) {
				context.addServlet(new ServletHolder(new RadioServlet(deviceManager)),"/radio");
			}

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
