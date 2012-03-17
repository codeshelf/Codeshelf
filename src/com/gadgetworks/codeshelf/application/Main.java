/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2011, Jeffrey B. Williams, All rights reserved
 *  $Id: Main.java,v 1.3 2012/03/17 23:49:23 jeffw Exp $
 *******************************************************************************/

package com.gadgetworks.codeshelf.application;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.gadgetworks.codeshelf.model.dao.DaoRegistry;
import com.gadgetworks.codeshelf.model.dao.GenericDao;
import com.gadgetworks.codeshelf.model.dao.IDaoRegistry;
import com.gadgetworks.codeshelf.model.dao.IGenericDao;
import com.gadgetworks.codeshelf.model.persist.Organization.OrganizationDao;
import com.gadgetworks.codeshelf.model.persist.User;
import com.gadgetworks.codeshelf.web.websession.IWebSessionManager;
import com.gadgetworks.codeshelf.web.websession.WebSessionManager;
import com.gadgetworks.codeshelf.web.websession.command.IWebSessionReqCmdFactory;
import com.gadgetworks.codeshelf.web.websession.command.WebSessionReqCmdFactory;
import com.gadgetworks.codeshelf.web.websocket.IWebSocketListener;
import com.gadgetworks.codeshelf.web.websocket.WebSocketListener;
import com.google.inject.AbstractModule;
import com.google.inject.BindingAnnotation;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.PrivateModule;

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
	
	public interface IUserDao extends IGenericDao<User> {		
	}
	
	public class UserDao extends GenericDao<User> implements IUserDao {
		@Inject
		public UserDao(final IDaoRegistry inDaoRegistry) {
			super(User.class, inDaoRegistry);
		}
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
				bind(IUserDao.class).to(UserDao.class);
				bind(IWebSessionReqCmdFactory.class).to(WebSessionReqCmdFactory.class);
				bind(IDaoRegistry.class).to(DaoRegistry.class);
				//bind(IUserDao.class).toProvider(DaoProviderFactory.createProvider(IUserDao.class)); 
				//install(new XmlBeanModule(xmlUrl));
			}
		}, new PrivateModule() {
			@Override
			protected void configure() {
				// private Module is different story
				// Bind car annotated with blue and expose it
				bind(IGenericDao.class).annotatedWith(UserDaoSelector.class).to(IGenericDao.class);
				expose(IGenericDao.class).annotatedWith(UserDaoSelector.class);

				// What we bind in here only applies to the exposed stuff
				// i.e. the exposed car from this module will get this injected
				// where stuff in regular module (Engine,Driveline) is "inherited" - it is global
				bind(IGenericDao.class).to(UserDao.class);
			}
		}, new PrivateModule() {
			@Override
			protected void configure() {
				bind(IGenericDao.class).annotatedWith(OrganizationDaoSelector.class).to(IGenericDao.class);
				expose(IGenericDao.class).annotatedWith(OrganizationDaoSelector.class);

				bind(IGenericDao.class).to(OrganizationDao.class);
			}
		});

		IGenericDao blueCar = injector.getInstance(Key.get(IGenericDao.class, UserDaoSelector.class));
        IGenericDao redCar = injector.getInstance(Key.get(IGenericDao.class, OrganizationDaoSelector.class));
        
        return injector;
	}
}