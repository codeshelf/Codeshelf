/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: DomainObjectABC.java,v 1.34 2013/04/14 17:51:29 jeffw Exp $
 *******************************************************************************/
package com.codeshelf.model.domain;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;
import javax.persistence.Version;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.ToString;

import org.atteo.classindex.ClassIndex;
import org.atteo.classindex.IndexSubclasses;
import org.hibernate.Hibernate;
import org.hibernate.annotations.Type;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.model.dao.ITypedDao;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

// --------------------------------------------------------------------------
/**
 *  This is the base persistent object.  It contains the persistence ID and provides
 *  operations around that ID, such as object equivalence (by ID).
 *  
 *  @author jeffw
 */

@MappedSuperclass
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE)
@IndexSubclasses
@JsonTypeInfo(use=JsonTypeInfo.Id.NAME, include=JsonTypeInfo.As.PROPERTY, property = "className")
@JsonPropertyOrder({ "domainId", "fullDomainId" })
@JsonIgnoreProperties({"className"})
@ToString(doNotUseGetters = true, of = { "domainId" })
public abstract class DomainObjectABC implements IDomainObject {

	@SuppressWarnings("unused")
	private static final Logger	LOGGER	= LoggerFactory.getLogger(DomainObjectABC.class);

	// dao classes + instances
	// statically cached so they do not have to be regenerated in between junit tests
	private static Map<Class<? extends IDomainObject>,Class<? extends ITypedDao<? extends IDomainObject>>> daoClasses = null;
	private static Map<Class<? extends IDomainObject>,ITypedDao<?>> daos = null;
	
	// This is the internal GUID
	@Id
	@NonNull
	@Column(name = "persistentid", nullable = false)
	@Getter
	@Setter
	@JsonProperty
	@Type(type="com.codeshelf.platform.persistence.DialectUUIDType")
	private UUID persistentId = UUID.randomUUID();

	// The domain ID
	@NonNull
	@Column(name = "domainid", nullable = false)
	@Getter
	@Setter
	@JsonProperty
	private String				domainId;

	// This is not an application-editable field.
	// It's for the private use of the ORM transaction system.
	@Version
	@Column(nullable = false)
	@Getter
	@Setter
	// @JsonProperty do we need to serialize this?
	private long version;

	public DomainObjectABC() {
		// lastDefaultSequenceId = 0;
	}

	public DomainObjectABC(String inDomainId) {
		this.domainId = inDomainId;
	}

	// --------------------------------------------------------------------------
	/**
	 * @return
	 */
	protected Integer getIdDigits() {
		return 2;
	}

	// --------------------------------------------------------------------------
	/**
	 * @return
	 */
	@JsonProperty
	public final String getClassName() {
		return Hibernate.getClass(this).getSimpleName();
	}

	/* --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	public boolean equals(Object inObject) {
		boolean result = false;

		if (inObject instanceof DomainObjectABC) {
			Class<?> thisClass = Hibernate.getClass(this);
			Class<?> inObjectClass = Hibernate.getClass(inObject);
			return (thisClass.equals(inObjectClass)
					&& Objects.equals(this.persistentId, ((DomainObjectABC) inObject).getPersistentId()));
		} else {
			result = super.equals(inObject);
		}
		return result;
	}

	/* --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	public int hashCode() {
		if (persistentId != null) {
			return persistentId.hashCode();
		} else {
			return 0;
		}
	}

	// --------------------------------------------------------------------------
	/**
	 * Get all of the fields up the tree.
	 * @param inClass
	 * @param inFields
	 */
	@SuppressWarnings("unused")
	private void addDeclaredAndInheritedFields(Class<?> inClass, Collection<Field> inFields) {
		inFields.addAll(Arrays.asList(inClass.getDeclaredFields()));
		Class<?> superClass = inClass.getSuperclass();
		if (superClass != null) {
			addDeclaredAndInheritedFields(superClass, inFields);
		}
	}

	public void store() {
		this.getDao().store(this);		
	}
	
	public static Map<Class<? extends IDomainObject>,ITypedDao<?>> getDaos() {
		// Create new ITypedDao objects for each domain class
		
		if(daos != null) {
			// already initialized
			return daos;
		}
		getDaoClasses(); // ensure dao class list is initialized first
		
		daos = new HashMap<Class<? extends IDomainObject>,ITypedDao<?>>();
		Set<Class<? extends IDomainObject>> domainTypes = daoClasses.keySet();
		
		for(Class<? extends IDomainObject> domainType : domainTypes) {
			Class<? extends ITypedDao<? extends IDomainObject>> daoClass = daoClasses.get(domainType);
			ITypedDao<?> dao = null;
			try {
				dao = daoClass.newInstance();
			} catch (InstantiationException | IllegalAccessException e) {
				throw new RuntimeException("Unexpected exception creating DAOs", e);
			}
			if(dao.getDaoClass().equals(domainType)) {
				daos.put(domainType, dao);
			} else {
				throw new RuntimeException("Could not assign DAO for "
						+dao.getDaoClass().getSimpleName()+" to class "+domainType.getSimpleName());
			}
		}
		//LOGGER.info("created DAOs for: "+daos.keySet());
		return daos;
	}

	public static Map<Class<? extends IDomainObject>,Class<? extends ITypedDao<? extends IDomainObject>>> getDaoClasses() {
		if(daoClasses != null) {
			// already initialized
			return daoClasses;
		}

		Iterable<Class<? extends IDomainObject>> domainTypes = ClassIndex.getSubclasses(IDomainObject.class);
		
		daoClasses = new HashMap<Class<? extends IDomainObject>,Class<? extends ITypedDao<? extends IDomainObject>>>();
		
		// iterate through all classes that implement IDomainObject
		for (Class<? extends IDomainObject> domainType : domainTypes) {
			Class<?>[] memberClasses = domainType.getDeclaredClasses();
			Class<? extends ITypedDao<? extends IDomainObject>> foundDaoClass = null;
			
			// find a single declared static member class extending ITypedDao which is the DAO for that type
			for(int i=0; i < memberClasses.length; i++) {
				Class<?> memberClass = memberClasses[i];
				if(ITypedDao.class.isAssignableFrom(memberClass)) {
					if(Modifier.isStatic(memberClass.getModifiers())) {
						if(foundDaoClass == null) {
							if(!memberClass.getSimpleName().startsWith(domainType.getSimpleName()) 
									|| !memberClass.getSimpleName().endsWith("Dao")) {
								throw new RuntimeException("DAO class did not follow expected naming convention - expected "
									+domainType.getSimpleName()+"[...]Dao but got "+memberClasses[i].getSimpleName()
									+" (remove this if convention has changed)");
							}

							@SuppressWarnings("unchecked")
							Class<? extends ITypedDao<? extends IDomainObject>> dc 
									= (Class<? extends ITypedDao<? extends IDomainObject>>) memberClass; 
							foundDaoClass = dc;
						} else {
							throw new RuntimeException("Failed to uniquely identify DAO class for "
									+domainType.getSimpleName()+". Found "+foundDaoClass.getSimpleName()+" and "+memberClass.getSimpleName());
						}
					} else {
						throw new RuntimeException("DAO class was not declared static - "+memberClass.getCanonicalName());
					}
				} // else ignore not ITypedDao
			}
			if(foundDaoClass != null) {
				daoClasses.put(domainType, foundDaoClass);
			} // else if no declared DAO class, ignore			
		}
		//LOGGER.info("Found DAO classes: "+daoClasses.values());
		return daoClasses;
	}

	
}
