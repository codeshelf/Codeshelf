/*******************************************************************************
CodeshelfWebSocketServer *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: ServerMain.java,v 1.15 2013/11/11 07:46:30 jeffw Exp $
 *******************************************************************************/

package com.gadgetworks.codeshelf.application;

import org.apache.commons.beanutils.ConvertUtilsBean;
import org.apache.shiro.authc.credential.CredentialsMatcher;
import org.apache.shiro.authc.credential.HashedCredentialsMatcher;
import org.apache.shiro.crypto.hash.Md5Hash;
import org.apache.shiro.realm.Realm;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;
import com.gadgetworks.codeshelf.edi.AislesFileCsvImporter;
import com.gadgetworks.codeshelf.edi.CrossBatchCsvImporter;
import com.gadgetworks.codeshelf.edi.EdiProcessor;
import com.gadgetworks.codeshelf.edi.ICsvAislesFileImporter;
import com.gadgetworks.codeshelf.edi.ICsvCrossBatchImporter;
import com.gadgetworks.codeshelf.edi.ICsvInventoryImporter;
import com.gadgetworks.codeshelf.edi.ICsvLocationAliasImporter;
import com.gadgetworks.codeshelf.edi.ICsvOrderImporter;
import com.gadgetworks.codeshelf.edi.ICsvOrderLocationImporter;
import com.gadgetworks.codeshelf.edi.IEdiProcessor;
import com.gadgetworks.codeshelf.edi.InventoryCsvImporter;
import com.gadgetworks.codeshelf.edi.LocationAliasCsvImporter;
import com.gadgetworks.codeshelf.edi.OrderLocationCsvImporter;
import com.gadgetworks.codeshelf.edi.OutboundOrderCsvImporter;
import com.gadgetworks.codeshelf.model.dao.ITypedDao;
import com.gadgetworks.codeshelf.model.domain.Aisle;
import com.gadgetworks.codeshelf.model.domain.Aisle.AisleDao;
import com.gadgetworks.codeshelf.model.domain.Bay;
import com.gadgetworks.codeshelf.model.domain.Bay.BayDao;
import com.gadgetworks.codeshelf.model.domain.Che;
import com.gadgetworks.codeshelf.model.domain.Che.CheDao;
import com.gadgetworks.codeshelf.model.domain.CodeshelfNetwork;
import com.gadgetworks.codeshelf.model.domain.CodeshelfNetwork.CodeshelfNetworkDao;
import com.gadgetworks.codeshelf.model.domain.Container;
import com.gadgetworks.codeshelf.model.domain.Container.ContainerDao;
import com.gadgetworks.codeshelf.model.domain.ContainerKind;
import com.gadgetworks.codeshelf.model.domain.ContainerKind.ContainerKindDao;
import com.gadgetworks.codeshelf.model.domain.ContainerUse;
import com.gadgetworks.codeshelf.model.domain.ContainerUse.ContainerUseDao;
import com.gadgetworks.codeshelf.model.domain.DropboxService;
import com.gadgetworks.codeshelf.model.domain.DropboxService.DropboxServiceDao;
import com.gadgetworks.codeshelf.model.domain.EdiDocumentLocator;
import com.gadgetworks.codeshelf.model.domain.EdiDocumentLocator.EdiDocumentLocatorDao;
import com.gadgetworks.codeshelf.model.domain.EdiServiceABC;
import com.gadgetworks.codeshelf.model.domain.EdiServiceABC.EdiServiceABCDao;
//import com.gadgetworks.codeshelf.model.domain.EdiServiceABC.EdiServiceABCDao;
import com.gadgetworks.codeshelf.model.domain.Facility;
import com.gadgetworks.codeshelf.model.domain.Facility.FacilityDao;
import com.gadgetworks.codeshelf.model.domain.IronMqService;
import com.gadgetworks.codeshelf.model.domain.IronMqService.IronMqServiceDao;
import com.gadgetworks.codeshelf.model.domain.Item;
import com.gadgetworks.codeshelf.model.domain.Item.ItemDao;
import com.gadgetworks.codeshelf.model.domain.ItemDdcGroup;
import com.gadgetworks.codeshelf.model.domain.ItemDdcGroup.ItemDdcGroupDao;
import com.gadgetworks.codeshelf.model.domain.ItemMaster;
import com.gadgetworks.codeshelf.model.domain.ItemMaster.ItemMasterDao;
import com.gadgetworks.codeshelf.model.domain.LedController;
import com.gadgetworks.codeshelf.model.domain.LedController.LedControllerDao;
//import com.gadgetworks.codeshelf.model.domain.LocationABC.LocationABCDao;
import com.gadgetworks.codeshelf.model.domain.LocationAlias;
import com.gadgetworks.codeshelf.model.domain.LocationAlias.LocationAliasDao;
import com.gadgetworks.codeshelf.model.domain.OrderDetail;
import com.gadgetworks.codeshelf.model.domain.OrderDetail.OrderDetailDao;
import com.gadgetworks.codeshelf.model.domain.OrderGroup;
import com.gadgetworks.codeshelf.model.domain.OrderGroup.OrderGroupDao;
import com.gadgetworks.codeshelf.model.domain.OrderHeader;
import com.gadgetworks.codeshelf.model.domain.OrderHeader.OrderHeaderDao;
import com.gadgetworks.codeshelf.model.domain.OrderLocation;
import com.gadgetworks.codeshelf.model.domain.OrderLocation.OrderLocationDao;
import com.gadgetworks.codeshelf.model.domain.Path;
import com.gadgetworks.codeshelf.model.domain.Path.PathDao;
import com.gadgetworks.codeshelf.model.domain.PathSegment;
import com.gadgetworks.codeshelf.model.domain.PathSegment.PathSegmentDao;
import com.gadgetworks.codeshelf.model.domain.SiteController;
import com.gadgetworks.codeshelf.model.domain.SiteController.SiteControllerDao;
import com.gadgetworks.codeshelf.model.domain.Slot;
import com.gadgetworks.codeshelf.model.domain.Slot.SlotDao;
//import com.gadgetworks.codeshelf.model.domain.SubLocationABC.SubLocationDao;
import com.gadgetworks.codeshelf.model.domain.Tier;
import com.gadgetworks.codeshelf.model.domain.Tier.TierDao;
import com.gadgetworks.codeshelf.model.domain.UnspecifiedLocation;
import com.gadgetworks.codeshelf.model.domain.UnspecifiedLocation.UnspecifiedLocationDao;
import com.gadgetworks.codeshelf.model.domain.UomMaster;
import com.gadgetworks.codeshelf.model.domain.UomMaster.UomMasterDao;
import com.gadgetworks.codeshelf.model.domain.Vertex;
import com.gadgetworks.codeshelf.model.domain.Vertex.VertexDao;
//import com.gadgetworks.codeshelf.model.domain.WirelessDeviceABC.WirelessDeviceDao;
import com.gadgetworks.codeshelf.model.domain.WorkArea;
import com.gadgetworks.codeshelf.model.domain.WorkArea.WorkAreaDao;
import com.gadgetworks.codeshelf.model.domain.WorkInstruction;
import com.gadgetworks.codeshelf.model.domain.WorkInstruction.WorkInstructionDao;
import com.gadgetworks.codeshelf.platform.multitenancy.ITenantManager;
import com.gadgetworks.codeshelf.platform.multitenancy.TenantManagerService;
import com.gadgetworks.codeshelf.platform.persistence.TenantPersistenceService;
import com.gadgetworks.codeshelf.report.IPickDocumentGenerator;
import com.gadgetworks.codeshelf.report.PickDocumentGenerator;
import com.gadgetworks.codeshelf.security.CodeshelfRealm;
import com.gadgetworks.codeshelf.service.PropertyService;
import com.gadgetworks.codeshelf.service.WorkService;
import com.gadgetworks.codeshelf.util.ConverterProvider;
import com.gadgetworks.codeshelf.util.IConfiguration;
import com.gadgetworks.codeshelf.util.JVMSystemConfiguration;
import com.gadgetworks.codeshelf.ws.jetty.protocol.message.MessageProcessor;
import com.gadgetworks.codeshelf.ws.jetty.server.CsServerEndPoint;
import com.gadgetworks.codeshelf.ws.jetty.server.ServerMessageProcessor;
import com.gadgetworks.codeshelf.ws.jetty.server.SessionManager;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Provides;
import com.google.inject.Scopes;
import com.google.inject.Singleton;
import com.google.inject.TypeLiteral;
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
	
	// pre-main static load configuration and set up logging (see Configuration.java)
	static {
		Configuration.loadConfig("server");
	}

	private static final Logger	LOGGER	= LoggerFactory.getLogger(ServerMain.class);

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
		CsServerEndPoint.setSessionManager(dynamicInjector.getInstance(SessionManager.class));
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
				bind(ITenantManager.class).toInstance(TenantManagerService.getInstance());
				
				bind(TenantPersistenceService.class).toInstance(TenantPersistenceService.getInstance());
				bind(GuiceFilter.class);
				
				bind(IConfiguration.class).to(JVMSystemConfiguration.class);
				bind(ICodeshelfApplication.class).to(ServerCodeshelfApplication.class);
				bind(IEdiProcessor.class).to(EdiProcessor.class);
				bind(IPickDocumentGenerator.class).to(PickDocumentGenerator.class);
				bind(ICsvOrderImporter.class).to(OutboundOrderCsvImporter.class);
				bind(ICsvInventoryImporter.class).to(InventoryCsvImporter.class);
				bind(ICsvLocationAliasImporter.class).to(LocationAliasCsvImporter.class);
				bind(ICsvOrderLocationImporter.class).to(OrderLocationCsvImporter.class);
				bind(ICsvAislesFileImporter.class).to(AislesFileCsvImporter.class);
				bind(ICsvCrossBatchImporter.class).to(CrossBatchCsvImporter.class);

				bind(SessionManager.class).toInstance(SessionManager.getInstance());
				
				bind(PropertyService.class).toInstance(new PropertyService());
				
				// jetty websocket
				bind(MessageProcessor.class).to(ServerMessageProcessor.class).in(Singleton.class);
				
				bind(ConvertUtilsBean.class).toProvider(ConverterProvider.class);
				
				// Shiro modules
				bind(Realm.class).to(CodeshelfRealm.class);
				bind(CredentialsMatcher.class).to(HashedCredentialsMatcher.class);
				bind(HashedCredentialsMatcher.class);
				bindConstant().annotatedWith(Names.named("shiro.hashAlgorithmName")).to(Md5Hash.ALGORITHM_NAME);
				
				// Register the DAOs (statically as a singleton).
				

				requestStaticInjection(Aisle.class);
				bind(new TypeLiteral<ITypedDao<Aisle>>() {
				}).to(AisleDao.class);

				requestStaticInjection(LedController.class);
				bind(new TypeLiteral<ITypedDao<LedController>>() {
				}).to(LedControllerDao.class);

				requestStaticInjection(Bay.class);
				bind(new TypeLiteral<ITypedDao<Bay>>() {
				}).to(BayDao.class);

				requestStaticInjection(Che.class);
				bind(new TypeLiteral<ITypedDao<Che>>() {
				}).to(CheDao.class);

				requestStaticInjection(SiteController.class);
				bind(new TypeLiteral<ITypedDao<SiteController>>() {
				}).to(SiteControllerDao.class);

				requestStaticInjection(CodeshelfNetwork.class);
				bind(new TypeLiteral<ITypedDao<CodeshelfNetwork>>() {
				}).to(CodeshelfNetworkDao.class);

				requestStaticInjection(Container.class);
				bind(new TypeLiteral<ITypedDao<Container>>() {
				}).to(ContainerDao.class);

				requestStaticInjection(ContainerKind.class);
				bind(new TypeLiteral<ITypedDao<ContainerKind>>() {
				}).to(ContainerKindDao.class);

				requestStaticInjection(ContainerUse.class);
				bind(new TypeLiteral<ITypedDao<ContainerUse>>() {
				}).to(ContainerUseDao.class);

				requestStaticInjection(EdiServiceABC.class);
				bind(new TypeLiteral<ITypedDao<EdiServiceABC>>() {
				}).to(EdiServiceABCDao.class);

				requestStaticInjection(DropboxService.class);
				bind(new TypeLiteral<ITypedDao<DropboxService>>() {
				}).to(DropboxServiceDao.class);

				requestStaticInjection(EdiDocumentLocator.class);
				bind(new TypeLiteral<ITypedDao<EdiDocumentLocator>>() {
				}).to(EdiDocumentLocatorDao.class);

				requestStaticInjection(IronMqService.class);
				bind(new TypeLiteral<ITypedDao<IronMqService>>() {
				}).to(IronMqServiceDao.class);

				requestStaticInjection(Facility.class);
				bind(new TypeLiteral<ITypedDao<Facility>>() {
				}).to(FacilityDao.class);

				requestStaticInjection(Item.class);
				bind(new TypeLiteral<ITypedDao<Item>>() {
				}).to(ItemDao.class);

				requestStaticInjection(ItemMaster.class);
				bind(new TypeLiteral<ITypedDao<ItemMaster>>() {
				}).to(ItemMasterDao.class);

				requestStaticInjection(ItemDdcGroup.class);
				bind(new TypeLiteral<ITypedDao<ItemDdcGroup>>() {
				}).to(ItemDdcGroupDao.class);
/*
				requestStaticInjection(LocationABC.class);
				bind(new TypeLiteral<ITypedDao<LocationABC>>() {
				}).to(LocationABCDao.class);
*/
				requestStaticInjection(LocationAlias.class);
				bind(new TypeLiteral<ITypedDao<LocationAlias>>() {
				}).to(LocationAliasDao.class);

				requestStaticInjection(OrderDetail.class);
				bind(new TypeLiteral<ITypedDao<OrderDetail>>() {
				}).to(OrderDetailDao.class);

				requestStaticInjection(OrderHeader.class);
				bind(new TypeLiteral<ITypedDao<OrderHeader>>() {
				}).to(OrderHeaderDao.class);

				requestStaticInjection(OrderGroup.class);
				bind(new TypeLiteral<ITypedDao<OrderGroup>>() {
				}).to(OrderGroupDao.class);

				requestStaticInjection(OrderLocation.class);
				bind(new TypeLiteral<ITypedDao<OrderLocation>>() {
				}).to(OrderLocationDao.class);

				requestStaticInjection(Path.class);
				bind(new TypeLiteral<ITypedDao<Path>>() {
				}).to(PathDao.class);

				requestStaticInjection(PathSegment.class);
				bind(new TypeLiteral<ITypedDao<PathSegment>>() {
				}).to(PathSegmentDao.class);

				requestStaticInjection(Slot.class);
				bind(new TypeLiteral<ITypedDao<Slot>>() {
				}).to(SlotDao.class);

				requestStaticInjection(Tier.class);
				bind(new TypeLiteral<ITypedDao<Tier>>() {
				}).to(TierDao.class);
				
				requestStaticInjection(UnspecifiedLocation.class);
				bind(new TypeLiteral<ITypedDao<UnspecifiedLocation>>() {
				}).to(UnspecifiedLocationDao.class);

				requestStaticInjection(UomMaster.class);
				bind(new TypeLiteral<ITypedDao<UomMaster>>() {
				}).to(UomMasterDao.class);
				requestStaticInjection(Vertex.class);
				bind(new TypeLiteral<ITypedDao<Vertex>>() {
				}).to(VertexDao.class);

				requestStaticInjection(WorkArea.class);
				bind(new TypeLiteral<ITypedDao<WorkArea>>() {
				}).to(WorkAreaDao.class);

				requestStaticInjection(WorkInstruction.class);
				bind(new TypeLiteral<ITypedDao<WorkInstruction>>() {
				}).to(WorkInstructionDao.class);
			}
			
			@Provides
			public WorkService createWorkService() {
				WorkService workService = new WorkService();
				workService.start();
				return workService;
				
			}
		}, createGuiceServletModule());

		return injector;
	}
	
	private static ServletModule createGuiceServletModule() {
		return new ServletModule() {
		    @Override
		    protected void configureServlets() {
		        // bind resource classes here
		    	ResourceConfig rc = new PackagesResourceConfig( "com.gadgetworks.codeshelf.api.resources" );
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
