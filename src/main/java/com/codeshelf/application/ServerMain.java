/*******************************************************************************
CodeshelfWebSocketServer *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: ServerMain.java,v 1.15 2013/11/11 07:46:30 jeffw Exp $
 *******************************************************************************/

package com.codeshelf.application;

import java.util.Map;

import org.apache.commons.beanutils.ConvertUtilsBean;
import org.apache.shiro.authc.credential.CredentialsMatcher;
import org.apache.shiro.authc.credential.HashedCredentialsMatcher;
import org.apache.shiro.crypto.hash.Md5Hash;
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
import com.codeshelf.edi.OutboundOrderCsvImporter;
import com.codeshelf.metrics.IMetricsService;
import com.codeshelf.metrics.MetricsService;
import com.codeshelf.model.domain.DomainObjectABC;
import com.codeshelf.platform.multitenancy.ITenantManagerService;
import com.codeshelf.platform.multitenancy.TenantManagerService;
import com.codeshelf.platform.persistence.ITenantPersistenceService;
import com.codeshelf.platform.persistence.TenantPersistenceService;
import com.codeshelf.report.IPickDocumentGenerator;
import com.codeshelf.report.PickDocumentGenerator;
import com.codeshelf.security.CodeshelfRealm;
import com.codeshelf.service.IPropertyService;
import com.codeshelf.service.PropertyService;
import com.codeshelf.service.WorkService;
import com.codeshelf.util.ConverterProvider;
import com.codeshelf.ws.jetty.protocol.message.IMessageProcessor;
import com.codeshelf.ws.jetty.server.CsServerEndPoint;
import com.codeshelf.ws.jetty.server.ServerMessageProcessor;
import com.codeshelf.ws.jetty.server.SessionManagerService;
import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Provides;
import com.google.inject.Scopes;
import com.google.inject.Singleton;
import com.google.inject.name.Names;
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

		CsServerEndPoint.setSessionManagerService(dynamicInjector.getInstance(SessionManagerService.class));
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
				bind(ITenantManagerService.class).toInstance(TenantManagerService.getNonRunningInstance());
				
				requestStaticInjection(TenantPersistenceService.class);
				bind(ITenantPersistenceService.class).to(TenantPersistenceService.class).in(Singleton.class);

				requestStaticInjection(MetricsService.class);
				bind(IMetricsService.class).to(MetricsService.class).in(Singleton.class);

				requestStaticInjection(PropertyService.class);
				bind(IPropertyService.class).to(PropertyService.class).in(Singleton.class);

				//bind(EdiProcessor.class).to(EdiProcessor.class).in(Singleton.class);

				bind(GuiceFilter.class);
				
				bind(ICodeshelfApplication.class).to(ServerCodeshelfApplication.class);
	
				bind(IPickDocumentGenerator.class).to(PickDocumentGenerator.class);
				
				bind(ICsvOrderImporter.class).to(OutboundOrderCsvImporter.class);
				bind(ICsvInventoryImporter.class).to(InventoryCsvImporter.class);
				bind(ICsvLocationAliasImporter.class).to(LocationAliasCsvImporter.class);
				bind(ICsvOrderLocationImporter.class).to(OrderLocationCsvImporter.class);
				bind(ICsvAislesFileImporter.class).to(AislesFileCsvImporter.class);
				bind(ICsvCrossBatchImporter.class).to(CrossBatchCsvImporter.class);

				// jetty websocket
				bind(IMessageProcessor.class).to(ServerMessageProcessor.class).in(Singleton.class);
				
				bind(ConvertUtilsBean.class).toProvider(ConverterProvider.class);
				
				// Shiro modules
				bind(Realm.class).to(CodeshelfRealm.class);
				bind(CredentialsMatcher.class).to(HashedCredentialsMatcher.class);
				bind(HashedCredentialsMatcher.class);
				bindConstant().annotatedWith(Names.named("shiro.hashAlgorithmName")).to(Md5Hash.ALGORITHM_NAME);
				
			}
			
			@Provides
			@Singleton
			public WorkService createWorkService() {
				WorkService workService = new WorkService();
				return workService;				
			}

			@Provides
			@Singleton
			public SessionManagerService createSessionManagerService() {
				SessionManagerService sessionManagerService = new SessionManagerService();
				return sessionManagerService;				
			}
			
		}, createGuiceServletModule());

		return injector;
	}
	
	private static ServletModule createGuiceServletModule() {
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
