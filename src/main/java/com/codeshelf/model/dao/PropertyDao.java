package com.codeshelf.model.dao;

import java.io.InputStream;
import java.util.List;
import java.util.Scanner;

import org.hibernate.Query;
import org.hibernate.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.model.domain.DomainObjectProperty;
import com.codeshelf.model.domain.DomainObjectPropertyDefault;
import com.codeshelf.model.domain.IDomainObject;
import com.codeshelf.persistence.TenantPersistenceService;

public class PropertyDao extends GenericDaoABC<DomainObjectProperty> {

	private static final Logger LOGGER	= LoggerFactory.getLogger(PropertyDao.class);
	
	private static final String PROPERTY_DEFAULTS_FILENAME = "property-defaults.csv";
	
	private static PropertyDao theInstance = null;
	
	private PropertyDao() {
		super();
		setInstance();
	}

	private void setInstance() {
		PropertyDao.theInstance = this;
	}

	public final synchronized static PropertyDao getInstance() {
		if (theInstance == null) {
			theInstance = new PropertyDao();
		}
		return theInstance;
	}
	
	public DomainObjectPropertyDefault store(DomainObjectPropertyDefault propertyDefault) {
		Session session = TenantPersistenceService.getInstance().getSession();
		session.saveOrUpdate(propertyDefault);
		LOGGER.debug("Config type stored: "+propertyDefault);
		return propertyDefault;
	}

	/**
	 * Deletes property default value for a specific tenant.
	 */
	public void delete(DomainObjectPropertyDefault propertyDefault) {
		Session session = TenantPersistenceService.getInstance().getSession();
		session.delete(propertyDefault);
		LOGGER.debug("Property default deleted: "+propertyDefault);
	}
		
	@SuppressWarnings("unchecked")
	public List<DomainObjectPropertyDefault> getPropertyDefaults(IDomainObject object) {
		Session session = TenantPersistenceService.getInstance().getSession();
		String queryString = "from DomainObjectPropertyDefault where objectType = :objectType";
        Query query = session.createQuery(queryString);
        query.setParameter("objectType", object.getClassName());
        List<DomainObjectPropertyDefault> propertyDefaults = query.list();
		return propertyDefaults;
	}

	public DomainObjectPropertyDefault getPropertyDefault(IDomainObject object, String name) {
		return getPropertyDefault(object.getClassName(), name);
	}
	
	public DomainObjectPropertyDefault getPropertyDefault(String objectType, String name) {
		Session session = TenantPersistenceService.getInstance().getSession();
		String queryString = "from DomainObjectPropertyDefault where objectType = :objectType and name = :name";
        Query query = session.createQuery(queryString);
        query.setParameter("objectType", objectType);
        query.setParameter("name", name);
        DomainObjectPropertyDefault propertyDefault = (DomainObjectPropertyDefault) query.uniqueResult();        
		return propertyDefault;
	}	
	
	/**
	 * Gets the specific objects in the database for the parent domain objects. Used by higher level API call that includes default values.
	 */
	@SuppressWarnings("unchecked")
	public List<DomainObjectProperty> getProperties(IDomainObject object) {
		Session session = TenantPersistenceService.getInstance().getSession();
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
		Session session = TenantPersistenceService.getInstance().getSession();
		String queryString = "from DomainObjectProperty";
        Query query = session.createQuery(queryString);
        List<DomainObjectProperty> props = query.list();
		return props;
	}

	/**
	 * Get object if it exists. Often it will not.
	 */
	public DomainObjectProperty getProperty(IDomainObject object, String name) {
		Session session = TenantPersistenceService.getInstance().getSession();
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
        if (theDefault==null) {
        	LOGGER.warn("Failed to create property "+object.getClassName()+":"+name+": Default does not exist.");
        	//List<DomainObjectPropertyDefault> all = getAllDefaults();
        	return null;
        }
        prop = createProperty(object, theDefault);
        return prop;
	}

	@Override
	public Class<DomainObjectProperty> getDaoClass() {
		return DomainObjectProperty.class;
	}

	@SuppressWarnings("unchecked")
	public List<DomainObjectPropertyDefault> getAllDefaults() {
		Session session = TenantPersistenceService.getInstance().getSession();
		String queryString = "from DomainObjectPropertyDefault";
        Query query = session.createQuery(queryString);
        List<DomainObjectPropertyDefault> props = query.list();
		return props;
	}
	
	public void syncPropertyDefaults() {
		LOGGER.trace("Checking property defaults...");
		PropertyDao dao = PropertyDao.getInstance();
		List<DomainObjectPropertyDefault> currentProperties = dao.getAllDefaults();
		InputStream is = this.getClass().getResourceAsStream(PROPERTY_DEFAULTS_FILENAME);
		if (is==null) {
			is = this.getClass().getClassLoader().getResourceAsStream(PROPERTY_DEFAULTS_FILENAME);
		}
		if (is==null) {
			LOGGER.error("Failed to load property defaults");
			return;
		}
		
		int createdPropertyDefaults = 0;
		
		try (Scanner scanner = new Scanner(is)) {
			while (scanner.hasNextLine()) {
				String line = scanner.nextLine();
				String[] data = line.split(";");
				if (data.length!=4) {
					LOGGER.warn("Skipping default line does not contain four elements: "+line);
				}
				else {
					String objectType = data[0];
					String name = data[1];
					String value = data[2];
					String description = data[3];
					DomainObjectPropertyDefault def = dao.getPropertyDefault(objectType, name);
					if (def==null) {
						// insert default
						createdPropertyDefaults++;
						LOGGER.trace("Adding property default "+objectType+":"+name+"="+value);
						def = new DomainObjectPropertyDefault(name, objectType, value, description);
						dao.store(def);
					}
					else {
						// validate value and description
						if (def.getDefaultValue().equals(value) && def.getDescription().equals(description)) {
							// default already up-to-date
						}
						else {
							// update default with new values in database
							LOGGER.info("Updating property default "+objectType+":"+name+"="+value);
							def.setDefaultValue(value);
							def.setDescription(description);
							dao.store(def);
						}
						// remove item from current defaults
						for (DomainObjectPropertyDefault d : currentProperties) {
							if (d.getName().equals(def.getName()) && d.getObjectType().equals(def.getObjectType())) {
								currentProperties.remove(def);
								break;
							}
						}
					}
				}
			}
			scanner.close();
		} catch (Exception e) {
			LOGGER.error("Failed to sync up property defaults",e);
			return;
		}
		if(createdPropertyDefaults>0) {
			LOGGER.info("Created "+ createdPropertyDefaults+" property defaults");
		}
		// delete properties that are not included in resource file
		for(DomainObjectPropertyDefault def : currentProperties) {
			LOGGER.info("Deleting obsolete property default "+def.getObjectType()+":"+def.getName()+"="+def.getDefaultValue()+" and its instances.");
			dao.delete(def);
		}
	}

}
