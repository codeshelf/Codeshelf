/*******************************************************************************
CodeshelfWebSocketServer *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: ServerMain.java,v 1.15 2013/11/11 07:46:30 jeffw Exp $
 *******************************************************************************/

package com.codeshelf.application;

import org.apache.commons.beanutils.ConvertUtilsBean;
import org.apache.shiro.guice.aop.ShiroAopModule;
import org.apache.shiro.mgt.SecurityManager;
import org.apache.shiro.realm.Realm;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.edi.AislesFileCsvImporter;
import com.codeshelf.edi.CrossBatchCsvImporter;
import com.codeshelf.edi.ICsvAislesFileImporter;
import com.codeshelf.edi.ICsvCrossBatchImporter;
import com.codeshelf.edi.ICsvInventoryImporter;
import com.codeshelf.edi.ICsvLocationAliasImporter;
import com.codeshelf.edi.ICsvOrderImporter;
import com.codeshelf.edi.ICsvOrderLocationImporter;
import com.codeshelf.edi.InventoryCsvImporter;
import com.codeshelf.edi.LocationAliasCsvImporter;
import com.codeshelf.edi.OrderLocationCsvImporter;
import com.codeshelf.edi.OutboundOrderPrefetchCsvImporter;
import com.codeshelf.manager.ITenantManagerService;
import com.codeshelf.manager.TenantManagerService;
import com.codeshelf.metrics.IMetricsService;
import com.codeshelf.metrics.MetricsService;
import com.codeshelf.persistence.TenantPersistenceService;
import com.codeshelf.report.IPickDocumentGenerator;
import com.codeshelf.report.PickDocumentGenerator;
import com.codeshelf.security.AuthProviderService;
import com.codeshelf.security.CodeshelfRealm;
import com.codeshelf.security.CodeshelfSecurityManager;
import com.codeshelf.security.HmacAuthService;
import com.codeshelf.service.IPropertyService;
import com.codeshelf.service.PropertyService;
import com.codeshelf.service.WorkService;
import com.codeshelf.util.ConverterProvider;
import com.codeshelf.ws.protocol.message.IMessageProcessor;
import com.codeshelf.ws.server.CsServerEndPoint;
import com.codeshelf.ws.server.ServerMessageProcessor;
import com.codeshelf.ws.server.WebSocketManagerService;
import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Scopes;
import com.google.inject.Singleton;
import com.google.inject.servlet.GuiceFilter;
import com.google.inject.servlet.ServletModule;
import com.sun.jersey.api.core.PackagesResourceConfig;
import com.sun.jersey.api.core.ResourceConfig;
import com.sun.jersey.guice.spi.container.servlet.GuiceContainer;

// --------------------------------------------------------------------------
/**
 *  @author jeffw
 */
public final class ServerMain {
	
	// pre-main static load configuration and set up logging (see JvmProperties.java)
	private static final Logger	LOGGER;
	static {
		JvmProperties.load("server");
		LOGGER = LoggerFactory.getLogger(ServerMain.class);
	}

	// --------------------------------------------------------------------------
	/**
	 */
	private ServerMain() {
	}

	// --------------------------------------------------------------------------
	/**
	 */
	public static void main(String[] inArgs) throws Exception {

		// Create and start the application.
		Injector dynamicInjector = setupInjector();
		
		ICodeshelfApplication application = dynamicInjector.getInstance(ServerCodeshelfApplication.class);

		application.startServices(); // this includes persistence and such, probably has to start before anything else

		CsServerEndPoint.setWebSocketManagerService(dynamicInjector.getInstance(WebSocketManagerService.class));
		CsServerEndPoint.setMessageProcessor(dynamicInjector.getInstance(ServerMessageProcessor.class));
		
		application.startApplication();

		// Handle events until the application exits.
		application.handleEvents();

		LOGGER.info("Exiting Main()");
		System.exit(0);
	}

	// --------------------------------------------------------------------------
	/**
	 * @return
	 */
	private static Injector setupInjector() {
		Injector injector = Guice.createInjector(new AbstractModule() {
			@Override
			protected void configure() {
				requestStaticInjection(TenantManagerService.class);
				bind(ITenantManagerService.class).to(TenantManagerService.class).in(Singleton.class);
				
				requestStaticInjection(TenantPersistenceService.class);
				bind(TenantPersistenceService.class).in(Singleton.class);

				requestStaticInjection(MetricsService.class);
				bind(IMetricsService.class).to(MetricsService.class).in(Singleton.class);

				requestStaticInjection(PropertyService.class);
				bind(IPropertyService.class).to(PropertyService.class).in(Singleton.class);
				
				bind(WorkService.class).in(Singleton.class);

				requestStaticInjection(WebSocketManagerService.class);
				bind(WebSocketManagerService.class).in(Singleton.class);
				
				bind(GuiceFilter.class);
				
				bind(ICodeshelfApplication.class).to(ServerCodeshelfApplication.class);
	
				bind(IPickDocumentGenerator.class).to(PickDocumentGenerator.class);
				
				//bind(ICsvOrderImporter.class).to(OutboundOrderCsvImporter.class);
				bind(ICsvOrderImporter.class).to(OutboundOrderPrefetchCsvImporter.class);
				bind(ICsvInventoryImporter.class).to(InventoryCsvImporter.class);
				bind(ICsvLocationAliasImporter.class).to(LocationAliasCsvImporter.class);
				bind(ICsvOrderLocationImporter.class).to(OrderLocationCsvImporter.class);
				bind(ICsvAislesFileImporter.class).to(AislesFileCsvImporter.class);
				bind(ICsvCrossBatchImporter.class).to(CrossBatchCsvImporter.class);

				// jetty websocket
				bind(IMessageProcessor.class).to(ServerMessageProcessor.class).in(Singleton.class);
				
				bind(ConvertUtilsBean.class).toProvider(ConverterProvider.class);
				
				// Shiro modules
				bind(SecurityManager.class).to(CodeshelfSecurityManager.class);
				bind(Realm.class).to(CodeshelfRealm.class);

				requestStaticInjection(HmacAuthService.class);
				bind(AuthProviderService.class).to(HmacAuthService.class).in(Singleton.class);
			}
					
		}, 
			new ShiroAopModule(), // enable shiro annotations on guice-jersey api 
			createGuiceServletModuleForApi());

		return injector;
	}
	
	private static ServletModule createGuiceServletModuleForApi() {
		return new ServletModule() {
		    @Override
		    protected void configureServlets() {
		        // bind resource classes here
		    	ResourceConfig rc = new PackagesResourceConfig( "com.codeshelf.api.resources" );

				for ( Class<?> resource : rc.getClasses() ) {
		    		bind( resource );	
		    	}
		    	
		        // hook JerseyContainer into Guice Servlet
		        bind(GuiceContainer.class);

		        // hook Jackson into Jersey as the POJO <-> JSON mapper
		        bind(JacksonJsonProvider.class).in(Scopes.SINGLETON);

		        serve("/*").with(GuiceContainer.class);
		    }
		};
	}

}
