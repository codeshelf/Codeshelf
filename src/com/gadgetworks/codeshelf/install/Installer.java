package com.gadgetworks.codeshelf.install;

import org.hibernate.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gadgetworks.codeshelf.application.Util;
import com.gadgetworks.codeshelf.model.dao.DaoException;
import com.gadgetworks.codeshelf.model.dao.ITypedDao;
import com.gadgetworks.codeshelf.model.domain.Facility;
import com.gadgetworks.codeshelf.model.domain.Organization;
import com.gadgetworks.codeshelf.model.domain.Organization.OrganizationDao;
import com.gadgetworks.codeshelf.model.domain.Path;
import com.gadgetworks.codeshelf.platform.services.PersistencyService;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.TypeLiteral;

public class Installer {
	
	static {
		System.setProperty("config.properties", "./conf/test.config.properties");
		Util.initLogging();
		Util.loadConfig();
	}
	
	private static final Logger	LOGGER	= LoggerFactory.getLogger(Installer.class);
	
	public Installer() {
		setupInjector();
	}
	
	private static Injector setupInjector() {
		Injector injector = Guice.createInjector(new AbstractModule() {
			@Override
			protected void configure() {
				requestStaticInjection(Organization.class);
				bind(new TypeLiteral<ITypedDao<Organization>>() {
				}).to(OrganizationDao.class);
			}
		});
		return injector;
	}	

	// --------------------------------------------------------------------------
	/**
	 *	Reset some of the persistent object fields to a base state at start-up.
	 */
	protected void doInitializeApplicationData() {
		// Create some demo organizations.
		createOrganizationUser("DEMO1", "a@example.com", "testme"); //view
		createOrganizationUser("DEMO1", "view@example.com", "testme"); //view
		createOrganizationUser("DEMO1", "configure@example.com", "testme"); //all
		createOrganizationUser("DEMO1", "simulate@example.com", "testme"); //simulate + configure
		createOrganizationUser("DEMO1", "che@example.com", "testme"); //view + simulate
		createOrganizationUser("DEMO1", "work@example.com", "testme"); //view + simulate
		
		createOrganizationUser("DEMO1", "view@goodeggs.com", "goodeggs"); //view
		createOrganizationUser("DEMO1", "view@accu-logistics.com", "accu-logistics"); //view
		
		createOrganizationUser("DEMO2", "a@example.com", "testme"); //view
		createOrganizationUser("DEMO2", "view@example.com", "testme"); //view
		createOrganizationUser("DEMO2", "configure@example.com", "testme"); //all
		createOrganizationUser("DEMO2", "simulate@example.com", "testme"); //simulate + configure
		createOrganizationUser("DEMO2", "che@example.com", "testme"); //view + simulate

		// Recompute path positions.
		// TODO: Remove once we have a tool for linking path segments to locations (aisles usually).
		for (Organization organization : Organization.DAO.getAll()) {
			for (Facility facility : organization.getFacilities()) {
				for (Path path : facility.getPaths()) {
					facility.recomputeLocationPathDistances(path);
				}
			}
		}
	}

	// --------------------------------------------------------------------------
	/**
	 * @param inOrganizationId
	 * @param inPassword
	 */
	private void createOrganizationUser(String inOrganizationId, String inDefaultUserId, String inDefaultUserPw) {
		Organization organization = Organization.DAO.findByDomainId(null, inOrganizationId);
		if (organization == null) {
			organization = new Organization();
			organization.setDomainId(inOrganizationId);
			try {
				Organization.DAO.store(organization);
			} catch (DaoException e) {
				e.printStackTrace();
			}

		}
		if (organization.getUser(inDefaultUserId) == null) {
			organization.createUser(inDefaultUserId, inDefaultUserPw);
		}
	}	
	
	public static void main(String[] args) {
		LOGGER.info("Running installer...");
		PersistencyService ps = new PersistencyService();
		ps.start();
		Installer installer = new Installer();
		Session session = ps.getCurrentTenantSession();
		installer.doInitializeApplicationData();
		session.close();
		LOGGER.info("Install completed");
		System.exit(0);
	}

}
