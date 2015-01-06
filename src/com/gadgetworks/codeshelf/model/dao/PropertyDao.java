package com.gadgetworks.codeshelf.model.dao;

import java.util.List;

import org.hibernate.Query;
import org.hibernate.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gadgetworks.codeshelf.model.domain.DomainObjectProperty;
import com.gadgetworks.codeshelf.model.domain.DomainObjectPropertyDefault;
import com.gadgetworks.codeshelf.model.domain.IDomainObject;
import com.gadgetworks.codeshelf.platform.persistence.PersistenceService;

public class PropertyDao extends GenericDaoABC<DomainObjectProperty> implements ITypedDao<DomainObjectProperty> {

	private static final Logger LOGGER	= LoggerFactory.getLogger(PropertyDao.class);
	
	private static PropertyDao theInstance = null;
	
	private PropertyDao(PersistenceService instance) {
		super(instance);
		setInstance();
	}

	private void setInstance() {
		PropertyDao.theInstance = this;
	}

	public final synchronized static PropertyDao getInstance() {
		if (theInstance == null) {
			theInstance = new PropertyDao(PersistenceService.getInstance());
		}
		return theInstance;
	}
	
	public DomainObjectPropertyDefault store(DomainObjectPropertyDefault propertyDefault) {
		Session session = PersistenceService.getInstance().getCurrentTenantSession();
		session.saveOrUpdate(propertyDefault);
		LOGGER.debug("Config type stored: "+propertyDefault);
		return propertyDefault;
	}

	/**
	 * Deletes property default value for a specific tenant.
	 */
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
	
	/**
	 * Gets the specific objects in the database for the parent domain objects. Used by higher level API call that includes default values.
	 */
	@SuppressWarnings("unchecked")
	public List<DomainObjectProperty> getProperties(IDomainObject object) {
		Session session = PersistenceService.getInstance().getCurrentTenantSession();
		String queryString = "from DomainObjectProperty as c where c.propertyDefault.objectType = :objectType and c.objectId = :objectId ";
		Query query = session.createQuery(queryString);
        query.setParameter("objectId", object.getPersistentId());
        query.setParameter("objectType", object.getClassName());
        List<DomainObjectProperty> props = query.list();
		return props;
	}

	/**
	 * This is the most useful API. Gets the few in the database, usually for the facility, and creates missing ones to match the defaults. Does not save the new ones. 
	 * New ones have persistentIds, but they are not meaningful.
	 */
	public List<DomainObjectProperty> getPropertiesWithDefaults(IDomainObject object) {
		// get object properties and types
        List<DomainObjectProperty> configs = getProperties(object);
        List<DomainObjectPropertyDefault> types = getPropertyDefaults(object);

        // add missing configuration values using defaults
        for (DomainObjectPropertyDefault type : types) {
        	boolean found = false;
        	for (DomainObjectProperty config : configs) {
        		if (config.getPropertyDefault().equals(type)) {
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
	
	/**
	 * Gets the specific objects in the database, for all parent domain objects. No known use case for this.
	 */
	@SuppressWarnings("unchecked")
	public List<DomainObjectProperty> getAllProperties() {
		Session session = PersistenceService.getInstance().getCurrentTenantSession();
		String queryString = "from DomainObjectProperty";
        Query query = session.createQuery(queryString);
        List<DomainObjectProperty> props = query.list();
		return props;
	}

	/**
	 * Get object if it exists. Often it will not.
	 */
	public DomainObjectProperty getProperty(IDomainObject object, String name) {
		Session session = PersistenceService.getInstance().getCurrentTenantSession();
		String queryString = "from DomainObjectProperty as c where c.propertyDefault.objectType = :objectType and c.propertyDefault.name = :name and c.objectId = :objectId ";
        Query query = session.createQuery(queryString);
        query.setParameter("objectId", object.getPersistentId());
        query.setParameter("objectType", object.getClassName());
        query.setParameter("name", name); 
        DomainObjectProperty prop = (DomainObjectProperty) query.uniqueResult();
		return prop;
	}

	/**
	 * Get object if it exists. Otherwise create one from the default. Not saved in this routine.
	 */
	public DomainObjectProperty getPropertyWithDefault(IDomainObject object, String name) {
        DomainObjectProperty prop = getProperty(object, name);
        if (prop != null)
        	return prop;
        DomainObjectPropertyDefault theDefault = getPropertyDefault(object, name);
        prop = createProperty(object, theDefault);
        return prop;
	}

	@Override
	public Class<DomainObjectProperty> getDaoClass() {
		return DomainObjectProperty.class;
	}
}
