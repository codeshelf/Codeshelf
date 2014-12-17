package com.gadgetworks.codeshelf.platform.property;

import java.util.List;

import org.hibernate.Query;
import org.hibernate.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gadgetworks.codeshelf.model.domain.DomainObjectProperty;
import com.gadgetworks.codeshelf.model.domain.DomainObjectPropertyDefault;
import com.gadgetworks.codeshelf.model.domain.IDomainObject;
import com.gadgetworks.codeshelf.platform.persistence.PersistenceService;

public class PropertyService {

	private static final Logger LOGGER	= LoggerFactory.getLogger(PropertyService.class);
	
	private static PropertyService theInstance = null;
	
	private PropertyService() {
		setInstance();
	}

	private void setInstance() {
		PropertyService.theInstance = this;
	}

	public final synchronized static PropertyService getInstance() {
		if (theInstance == null) {
			theInstance = new PropertyService();
		}
		return theInstance;
	}
	
	public DomainObjectPropertyDefault store(DomainObjectPropertyDefault propertyDefault) {
		Session session = PersistenceService.getInstance().getCurrentTenantSession();
		session.saveOrUpdate(propertyDefault);
		LOGGER.debug("Config type stored: "+propertyDefault);
		return propertyDefault;
	}

	public void delete(DomainObjectPropertyDefault propertyDefault) {
		Session session = PersistenceService.getInstance().getCurrentTenantSession();
		session.delete(propertyDefault);
		LOGGER.debug("Property default deleted: "+propertyDefault);
	}
		
	@SuppressWarnings("unchecked")
	public List<DomainObjectPropertyDefault> getPropertyDefaults(IDomainObject object) {
		Session session = PersistenceService.getInstance().getCurrentTenantSession();
		String queryString = "from DomainObjectPropertyDefault where objectType = :objectType";
        Query query = session.createQuery(queryString);
        query.setParameter("objectType", object.getClassName());
        List<DomainObjectPropertyDefault> propertyDefaults = query.list();
		return propertyDefaults;
	}

	public DomainObjectPropertyDefault getPropertyDefault(IDomainObject object, String name) {
		Session session = PersistenceService.getInstance().getCurrentTenantSession();
		String queryString = "from DomainObjectPropertyDefault where objectType = :objectType and name = :name";
        Query query = session.createQuery(queryString);
        query.setParameter("objectType", object.getClassName());
        query.setParameter("name", name);
        DomainObjectPropertyDefault propertyDefault = (DomainObjectPropertyDefault) query.uniqueResult();
		return propertyDefault;
	}
	
	public DomainObjectProperty store(DomainObjectProperty config) {
		Session session = PersistenceService.getInstance().getCurrentTenantSession();
		session.saveOrUpdate(config);
		LOGGER.debug("Property stored: "+config);
		return config;
	}

	public void delete(DomainObjectProperty prop) {
		Session session = PersistenceService.getInstance().getCurrentTenantSession();
		session.delete(prop);
		LOGGER.debug("Property deleted: "+prop);
	}

	@SuppressWarnings("unchecked")
	public List<DomainObjectProperty> getProperties(IDomainObject object) {
		Session session = PersistenceService.getInstance().getCurrentTenantSession();
		String queryString = "from DomainObjectProperty as c where c.type.objectType = :objectType and c.objectId = :objectId ";
		Query query = session.createQuery(queryString);
        query.setParameter("objectId", object.getPersistentId());
        query.setParameter("objectType", object.getClassName());
        List<DomainObjectProperty> props = query.list();
		return props;
	}

	public List<DomainObjectProperty> getPropertiesWithDefaults(IDomainObject object) {
		// get object properties and types
        List<DomainObjectProperty> configs = getProperties(object);
        List<DomainObjectPropertyDefault> types = getPropertyDefaults(object);

        // add missing configuration values using defaults
        for (DomainObjectPropertyDefault type : types) {
        	boolean found = false;
        	for (DomainObjectProperty config : configs) {
        		if (config.getType().equals(type)) {
        			found = true;
        			break;
        		}
        	}
        	if (!found) {
        		configs.add(createProperty(object, type));
        	}
        }        
        return configs;
	}

	private static DomainObjectProperty createProperty(IDomainObject object, DomainObjectPropertyDefault propertyDefault) {
		if (!object.getClassName().equals(propertyDefault.getObjectType())) {
			LOGGER.error("Property default "+propertyDefault+" is not applicable to "+object);
			return null;
		}
		DomainObjectProperty prop = new DomainObjectProperty(object,propertyDefault,propertyDefault.getDefaultValue());
		return prop;
	}
	
	@SuppressWarnings("unchecked")
	public List<DomainObjectProperty> getAllProperties() {
		Session session = PersistenceService.getInstance().getCurrentTenantSession();
		String queryString = "from DomainObjectProperty";
        Query query = session.createQuery(queryString);
        List<DomainObjectProperty> props = query.list();
		return props;
	}

	public DomainObjectProperty getProperty(IDomainObject object, String name) {
		Session session = PersistenceService.getInstance().getCurrentTenantSession();

		// String queryString = "from Configuration as c join c.type as t with t.objectType = :objectType and t.name = :name where c.objectId = :objectId ";
		String queryString = "from DomainObjectProperty as c where c.type.objectType = :objectType and c.type.name = :name and c.objectId = :objectId ";
        Query query = session.createQuery(queryString);
		// String queryString = "from Configuration where objectId = :objectId and objectType = :objectType and name = :name";
        // Query query = session.createQuery(queryString);
        query.setParameter("objectId", object.getPersistentId());
        query.setParameter("objectType", object.getClassName());
        query.setParameter("name", name); 
        DomainObjectProperty prop = (DomainObjectProperty) query.uniqueResult();
		return prop;
	}

}
