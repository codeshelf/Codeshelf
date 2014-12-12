package com.gadgetworks.codeshelf.platform.config;

import java.util.List;

import org.hibernate.Query;
import org.hibernate.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gadgetworks.codeshelf.model.domain.Configuration;
import com.gadgetworks.codeshelf.model.domain.IDomainObject;
import com.gadgetworks.codeshelf.platform.persistence.PersistenceService;

public class ConfigurationService {

	private static final Logger LOGGER	= LoggerFactory.getLogger(ConfigurationService.class);
	
	private static ConfigurationService theInstance = null;
	
	private ConfigurationService() {
		setInstance();
	}

	private void setInstance() {
		ConfigurationService.theInstance = this;
	}

	public final synchronized static ConfigurationService getInstance() {
		if (theInstance == null) {
			theInstance = new ConfigurationService();
		}
		return theInstance;
	}
	
	public Configuration store(Configuration config) {
		Session session = PersistenceService.getInstance().getCurrentTenantSession();
		session.saveOrUpdate(config);
		LOGGER.debug("Config stored: "+config);
		return config;
	}

	public void delete(Configuration config) {
		Session session = PersistenceService.getInstance().getCurrentTenantSession();
		session.delete(config);
		LOGGER.debug("Config deleted: "+config);
	}

	@SuppressWarnings("unchecked")
	public List<Configuration> getConfigurations(IDomainObject object) {
		Session session = PersistenceService.getInstance().getCurrentTenantSession();
		String queryString = "from Configuration where objectId = :objectId and objectType = :objectType";
        Query query = session.createQuery(queryString);
        query.setParameter("objectId", object.getPersistentId());
        query.setParameter("objectType", object.getClassName());
        List<Configuration> configs = query.list();
		return configs;
	}

	@SuppressWarnings("unchecked")
	public List<Configuration> getAllConfigurations() {
		Session session = PersistenceService.getInstance().getCurrentTenantSession();
		String queryString = "from Configuration";
        Query query = session.createQuery(queryString);
        List<Configuration> configs = query.list();
		return configs;
	}

	public Configuration getConfiguration(IDomainObject object, String name) {
		Session session = PersistenceService.getInstance().getCurrentTenantSession();
		String queryString = "from Configuration where objectId = :objectId and objectType = :objectType and name = :name";
        Query query = session.createQuery(queryString);
        query.setParameter("objectId", object.getPersistentId());
        query.setParameter("objectType", object.getClassName());
        query.setParameter("name", name);
        Configuration config = (Configuration) query.uniqueResult();
		return config;
	}
	
	public String getConfigAsString(IDomainObject object, String name, String defaultValue) {
		Configuration config = getConfiguration(object, name);
		if (config==null) {
			return defaultValue;
		}
		return config.getValue();
	}

	public String getConfigAsString(IDomainObject object, String name) {
		return this.getConfigAsString(object, name, null);
	}

	public Double getConfigAsDouble(IDomainObject object, String name, Double defaultValue) {
		Configuration config = getConfiguration(object, name);
		if (config==null) {
			return defaultValue;
		}
		double value = Double.parseDouble(config.getValue());
		return value;
	}

	public Double getConfigAsDouble(IDomainObject object, String name) {
		return this.getConfigAsDouble(object, name, null);
	}
	
	public Integer getConfigAsInt(IDomainObject object, String name, Integer defaultValue) {
		Configuration config = getConfiguration(object, name);
		if (config==null) {
			return defaultValue;
		}
		int value = Integer.parseInt(config.getValue());
		return value;
	}

	public Integer getConfigAsInt(IDomainObject object, String name) {
		return this.getConfigAsInt(object, name, null);
	}

}
