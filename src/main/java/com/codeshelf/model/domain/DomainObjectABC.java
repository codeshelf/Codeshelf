/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: DomainObjectABC.java,v 1.34 2013/04/14 17:51:29 jeffw Exp $
 *******************************************************************************/
package com.codeshelf.model.domain;

import java.lang.reflect.Field;
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

	//@SuppressWarnings("unused")
	//private static final Logger	LOGGER	= LoggerFactory.getLogger(DomainObjectABC.class);
	private static Map<Class<? extends IDomainObject>, Field> daoFields = null; 
	private static Map<Field,ITypedDao<?>> daos = null;
	
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

	public static Map<Class<? extends IDomainObject>, Field> getDaoFieldMap() {
		// Create a list of all domain objects that have an ITypedDao field declared. 
		// The field must be static.
		
		if(daoFields == null) {
			Iterable<Class<? extends IDomainObject>> domainTypes = ClassIndex.getSubclasses(IDomainObject.class);
			daoFields = new HashMap<Class<? extends IDomainObject>, Field>();
			for (Class<? extends IDomainObject> domainType : domainTypes) {
				// iterate through domain types
				
				Field foundField = null;
				for(Field field : domainType.getDeclaredFields()) {
					// find an ITypedDao field
					if(ITypedDao.class.isAssignableFrom(field.getType())) {
						if(java.lang.reflect.Modifier.isStatic(field.getModifiers())) {
							if(foundField == null) {
								if(!field.getName().equals("DAO")) {
									throw new RuntimeException("Static DAO member did not follow naming convention, expected DAO but got "+field.getName());
								}
								foundField = field;
							} else {
								throw new RuntimeException("Failed to uniquely identify DAO field in "+domainType.getSimpleName()
										+": found "+foundField.getName()+" and "+field.getName()  );
							}
						} // if not static, ignore
					} // if not dao, ignore
				}
				if (foundField != null) {
					daoFields.put(domainType,  foundField);
				}
			}
		}
		return daoFields;
	}
	
	private static Map<Field,ITypedDao<?>> createDaos() {
		// Create new ITypedDao objects for each corresponding static field.
		// Return a list of them.
		
		Map<Field,ITypedDao<?>> daos = new HashMap<Field,ITypedDao<?>>();
		Map<Class<? extends IDomainObject>, Field> daoFields = getDaoFieldMap();
		Set<Class<? extends IDomainObject>> domainTypes = daoFields.keySet();
		for(Class<? extends IDomainObject> domainType : domainTypes) {
			Field field = daoFields.get(domainType);
			Class<?>[] memberClasses = domainType.getDeclaredClasses();
			Class<? extends ITypedDao<? extends IDomainObject>> foundDaoClass = null;
			for(int i=0; i < memberClasses.length; i++) {
				if(ITypedDao.class.isAssignableFrom(memberClasses[i])) {
					if(foundDaoClass == null) {
						if(!memberClasses[i].getSimpleName().startsWith(domainType.getSimpleName()) 
								|| !memberClasses[i].getSimpleName().endsWith("Dao")) {
							throw new RuntimeException("DAO class did not follow expected naming convention - expected "
								+domainType.getSimpleName()+"Dao but got "+memberClasses[i].getSimpleName()
								+" (remove this if convention has changed)");
						}

						@SuppressWarnings("unchecked")
						Class<? extends ITypedDao<? extends IDomainObject>> dc 
								= (Class<? extends ITypedDao<? extends IDomainObject>>) memberClasses[i]; 
						foundDaoClass = dc;
					} else {
						throw new RuntimeException("Failed to uniquely identify DAO class for "
								+domainType.getSimpleName()+". Found "+foundDaoClass.getSimpleName()+" and "+memberClasses[i].getSimpleName());
					}
				}
			}
			if(foundDaoClass != null) {
				ITypedDao<?> dao;
				try {
					dao = foundDaoClass.newInstance();
				} catch (InstantiationException | IllegalAccessException e) {
					throw new RuntimeException("Unexpected exception creating DAOs", e);
				}
				if(dao.getDaoClass().equals(field.getDeclaringClass())) {
					daos.put(field, dao);
				} else {
					throw new RuntimeException("Could not assign DAO for "
							+dao.getDaoClass().getSimpleName()+" to class "+field.getDeclaringClass().getSimpleName());
				}
			} 
		}
		return daos;
	}

	public static Map<Class<?>,Class<?>> assignStaticDaoFields() {
		// assign (or for testing, reassign) all of the static DAO fields to DAO instances
		// return a map of class -> DAO class assignments, just for debugging
		Map<Class<?>,Class<?>> result = new HashMap<Class<?>,Class<?>>();
		
		if(daos == null) {
			daos = createDaos();
		}
		for(Field field : daos.keySet()) {
			ITypedDao<?> dao = daos.get(field);
			try {
				field.set(null,dao);
			} catch (IllegalArgumentException | IllegalAccessException e) {
				throw new RuntimeException("Unexpected exception setting DAO", e);
			}
			result.put(field.getDeclaringClass(), dao.getClass());
		}

		return result;
	}
}
