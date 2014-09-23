/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: DomainObjectTreeABC.java,v 1.6 2013/03/16 08:03:08 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.model.domain;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * @author jeffw
 *
 */

//@Entity
//@CacheStrategy(useBeanCache = true)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE)
//@ToString(doNotUseGetters = true)
public abstract class DomainObjectTreeABC<P extends IDomainObject> extends DomainObjectABC implements IDomainObjectTree<P> {

	@SuppressWarnings("unused")
	private static final Logger	LOGGER	= LoggerFactory.getLogger(DomainObjectTreeABC.class);

	public DomainObjectTreeABC() {
		super();
	}
	
	public DomainObjectTreeABC(String inDomainId) {
		super(inDomainId);
	}
	
	// --------------------------------------------------------------------------
	/* (non-Javadoc)
	 * @see com.gadgetworks.codeshelf.model.domain.IDomainObject#getFullDomainId()
	 */
	@JsonIgnore
	public String getFullDomainId() {
		return getParentFullDomainId() + "." + getDomainId();
	}

	// --------------------------------------------------------------------------
	/* (non-Javadoc)
	 * @see com.gadgetworks.codeshelf.model.domain.IDomainObject#getFullParentDomainId()
	 */
	@SuppressWarnings("rawtypes")
	@JsonIgnore
	public final String getParentFullDomainId() {
		String result = "";

		IDomainObject theParent = getParent();
		if (theParent != null) {
			if (theParent instanceof IDomainObjectTree) {
				result = ((IDomainObjectTree) theParent).getFullDomainId();
			} else if (theParent instanceof IDomainObject) {
				result = theParent.getDomainId();
			}
		}
		return result;
	}

	// --------------------------------------------------------------------------
	/* (non-Javadoc)
	 * @see com.gadgetworks.codeshelf.model.domain.IDomainObject#getParentPersistentId()
	 */
	@JsonIgnore
	public final UUID getParentPersistentId() {
		UUID result = null;
		IDomainObject domainObject = getParent();
		if (domainObject != null) {
			result = domainObject.getPersistentId();
		}
		return result;
	}

}
