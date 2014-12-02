/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: IDomainObject.java,v 1.18 2013/03/15 14:57:13 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.model.domain;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonGetter;
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
	@JsonGetter
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
	long getVersion();

	// --------------------------------------------------------------------------
	/**
	 * @param inVersion
	 */
	void setVersion(long inVersion);

	// --------------------------------------------------------------------------
	/**
	 * Return the DAO for this domain object.
	 * @return
	 */
	<T extends IDomainObject> ITypedDao<T> getDao();

	abstract Facility getFacility();
}
