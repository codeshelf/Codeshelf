/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2011, Jeffrey B. Williams, All rights reserved
 *  $Id: Main.java,v 1.4 2012/03/18 04:12:26 jeffw Exp $
 *******************************************************************************/

package com.gadgetworks.codeshelf.application;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.gadgetworks.codeshelf.model.dao.DaoRegistry;
import com.gadgetworks.codeshelf.model.dao.IDaoRegistry;
import com.gadgetworks.codeshelf.model.dao.domain.AisleDao;
import com.gadgetworks.codeshelf.model.dao.domain.CodeShelfNetworkDao;
import com.gadgetworks.codeshelf.model.dao.domain.ControlGroupDao;
import com.gadgetworks.codeshelf.model.dao.domain.DBPropertyDao;
import com.gadgetworks.codeshelf.model.dao.domain.FacilityDao;
import com.gadgetworks.codeshelf.model.dao.domain.OrganizationDao;
import com.gadgetworks.codeshelf.model.dao.domain.PersistentPropertyDao;
import com.gadgetworks.codeshelf.model.dao.domain.UserDao;
import com.gadgetworks.codeshelf.model.dao.domain.WirelessDeviceDao;
import com.gadgetworks.codeshelf.model.persist.Aisle.IAisleDao;
import com.gadgetworks.codeshelf.model.persist.CodeShelfNetwork.ICodeShelfNetworkDao;
import com.gadgetworks.codeshelf.model.persist.ControlGroup.IControlGroupDao;
import com.gadgetworks.codeshelf.model.persist.DBProperty.IDBPropertyDao;
import com.gadgetworks.codeshelf.model.persist.Facility.IFacilityDao;
import com.gadgetworks.codeshelf.model.persist.Organization.IOrganizationDao;
import com.gadgetworks.codeshelf.model.persist.PersistentProperty.IPersistentPropertyDao;
import com.gadgetworks.codeshelf.model.persist.User.IUserDao;
import com.gadgetworks.codeshelf.model.persist.WirelessDevice.IWirelessDeviceDao;
import com.gadgetworks.codeshelf.web.websession.IWebSessionManager;
import com.gadgetworks.codeshelf.web.websession.WebSessionManager;
import com.gadgetworks.codeshelf.web.websession.command.IWebSessionReqCmdFactory;
import com.gadgetworks.codeshelf.web.websession.command.WebSessionReqCmdFactory;
import com.gadgetworks.codeshelf.web.websocket.IWebSocketListener;
import com.gadgetworks.codeshelf.web.websocket.WebSocketListener;
import com.google.inject.AbstractModule;
import com.google.inject.BindingAnnotation;
import com.google.inject.Guice;
import com.google.inject.Injector;

// --------------------------------------------------------------------------
/**
 *  @author jeffw
 */
public final class Main {

	// See the top of Util to understand why we do the following:
	static {
		Util.initLogging();
	}

	private static final Log	LOGGER	= LogFactory.getLog(Main.class);

	// --------------------------------------------------------------------------
	/**
	 */
	private Main() {
	}

	// --------------------------------------------------------------------------
	/**
	 */
	public static void main(String[] inArgs) {

		// Guice (injector) will invoke log4j, so we need to set some log dir parameters before we call it.
		String appDataDir = Util.getApplicationDataDirPath();
		System.setProperty("app.data.dir", appDataDir);

		// Create and start the application.
		Injector injector = setupInjector();
		ICodeShelfApplication application = injector.getInstance(CodeShelfApplication.class);
		application.startApplication();

		// Handle events until the application exits.
		application.handleEvents();

		LOGGER.info("Exiting Main()");
	}

	// --------------------------------------------------------------------------

	@Retention(RetentionPolicy.RUNTIME)
	@Target({ ElementType.FIELD, ElementType.PARAMETER, ElementType.METHOD })
	@BindingAnnotation
	@interface UserDaoSelector {
	}

	@Retention(RetentionPolicy.RUNTIME)
	@Target({ ElementType.FIELD, ElementType.PARAMETER, ElementType.METHOD })
	@BindingAnnotation
	@interface OrganizationDaoSelector {
	}

	// --------------------------------------------------------------------------
	/**
	 * @return
	 */
	private static Injector setupInjector() {
		Injector injector = Guice.createInjector(new AbstractModule() {
			@Override
			protected void configure() {
				bind(ICodeShelfApplication.class).to(CodeShelfApplication.class);
				bind(IWebSocketListener.class).to(WebSocketListener.class);
				bind(IWebSessionManager.class).to(WebSessionManager.class);
				bind(IOrganizationDao.class).to(OrganizationDao.class);
				bind(IFacilityDao.class).to(FacilityDao.class);
				bind(IAisleDao.class).to(AisleDao.class);
				bind(IPersistentPropertyDao.class).to(PersistentPropertyDao.class);
				bind(IDBPropertyDao.class).to(DBPropertyDao.class);
				bind(IUserDao.class).to(UserDao.class);
				bind(ICodeShelfNetworkDao.class).to(CodeShelfNetworkDao.class);
				bind(IControlGroupDao.class).to(ControlGroupDao.class);
				bind(IWirelessDeviceDao.class).to(WirelessDeviceDao.class);
				bind(IWebSessionReqCmdFactory.class).to(WebSessionReqCmdFactory.class);
				bind(IDaoRegistry.class).to(DaoRegistry.class);
			}
		});

		return injector;
	}
}