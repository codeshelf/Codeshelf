/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: IDomainObject.java,v 1.17 2013/03/10 08:58:43 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.model.domain;

import java.sql.Timestamp;
import java.util.UUID;

import com.gadgetworks.codeshelf.model.dao.ITypedDao;

/**
 * @author jeffw
 *
 */
public interface IDomainObject {

	String	ID_PROPERTY			= "domainId";
	String	PARENT_PROPERTY		= "parent.persistentId";
	String	PARENT_ORG_PROPERTY	= "parentOrganization.persistentId";

	// --------------------------------------------------------------------------
	/**
	 * The prefix to use when creating a default domain ID.
	 * @return
	 */
	String getDefaultDomainIdPrefix();

	// --------------------------------------------------------------------------
	/**
	 * Return the short domain ID for this object (that is unique among all of the objects under this parent).
	 * @return
	 */
	String getDomainId();

	// --------------------------------------------------------------------------
	/**
	 * @param inId
	 */
	void setDomainId(String inId);

	// --------------------------------------------------------------------------
	/**
	 * @return
	 */
	//	List<? extends IDomainObject> getChildren();

	// --------------------------------------------------------------------------
	/**
	 * @return
	 */
	String getClassName();

	// --------------------------------------------------------------------------
	/**
	 * @return
	 */
	UUID getPersistentId();

	// --------------------------------------------------------------------------
	/**
	 * @param inPersistentId
	 */
	void setPersistentId(UUID inPersistentId);
	
	// --------------------------------------------------------------------------
	/**
	 * @return
	 */
	Timestamp getVersion();
	
	// --------------------------------------------------------------------------
	/**
	 * @param inVersion
	 */
	void setVersion(Timestamp inVersion);

	// --------------------------------------------------------------------------
	/**
	 * Return the DAO for this domain object.
	 * @return
	 */
	<T extends IDomainObject> ITypedDao<T> getDao();
	
	// --------------------------------------------------------------------------
	/**
	 * This allows us to get a domain object field value from the DAO in a way that goes around Ebean getter/setter decoration.
	 * DO NOT CALL THIS METHOD OUTSIDE OF DAO STORE().
	 * @param inFieldName
	 * @param inFieldValue
	 */
	Object getFieldValueByName(final String inFieldName);

	// --------------------------------------------------------------------------
	/**
	 * This allows us to set a domain object field value from the DAO in a way that goes around Ebean getter/setter decoration.
	 * DO NOT CALL THIS METHOD OUTSIDE OF DAO STORE().
	 * @param inFieldName
	 * @param inFieldValue
	 */
	void setFieldValueByName(final String inFieldName, final Object inFieldValue);

}
