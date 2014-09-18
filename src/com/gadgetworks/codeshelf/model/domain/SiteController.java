package com.gadgetworks.codeshelf.model.domain;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

import lombok.Getter;
import lombok.Setter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.avaje.ebean.annotation.CacheStrategy;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.gadgetworks.codeshelf.model.dao.GenericDaoABC;
import com.gadgetworks.codeshelf.model.dao.ISchemaManager;
import com.gadgetworks.codeshelf.model.dao.ITypedDao;
import com.google.inject.Inject;
import com.google.inject.Singleton;

@Entity
@CacheStrategy(useBeanCache = true)
@Table(name = "site_controller")
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE)
public class SiteController extends WirelessDeviceABC {
	
	public final static String defaultLocationDescription = "Unknown";
	
	@Inject
	public static ITypedDao<SiteController>	DAO;

	@Singleton
	public static class SiteControllerDao extends GenericDaoABC<SiteController> implements ITypedDao<SiteController> {
		@Inject
		public SiteControllerDao(final ISchemaManager inSchemaManager) {
			super(inSchemaManager);
		}
		
		public final Class<SiteController> getDaoClass() {
			return SiteController.class;
		}
	}

	@SuppressWarnings("unused")
	private static final Logger		LOGGER	= LoggerFactory.getLogger(SiteController.class);

	// Monitor/alert enabled for this equipment's online status (for equipment that is expected to be left on)
	@Column(nullable = false)
	@Getter
	@Setter
	private Boolean				monitor;

	// Text describing how to find the hardware installed in the warehouse (should be required non-blank when registering)
	@Column(nullable = false)
	@Getter
	@Setter
	private String				describeLocation;

	public SiteController () {
		this.monitor = true; // maybe this should be false, once there is a UI to set it, as it can cause bogus alerts when setting up sites
		this.describeLocation = defaultLocationDescription;
	}
	
	@Override
	public String getDefaultDomainIdPrefix() {
		return "SC";
	}

	@SuppressWarnings("unchecked")
	@Override
	public ITypedDao<SiteController> getDao() {
		return SiteController.DAO;
	}
	
	@Override
	public String toString() {
		return this.getDomainId();
	}

}
