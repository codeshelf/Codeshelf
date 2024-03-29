/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: DomainObjectTreeABC.java,v 1.6 2013/03/16 08:03:08 jeffw Exp $
 *******************************************************************************/
package com.codeshelf.model.domain;

import java.util.UUID;

import javax.persistence.FetchType;
import javax.persistence.ManyToOne;
import javax.persistence.MappedSuperclass;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.Getter;
import lombok.Setter;

/**
 * @author jeffw
 *
 */

@MappedSuperclass
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE)
public abstract class DomainObjectTreeABC<P extends IDomainObject> extends DomainObjectABC implements IDomainObjectTree<P> {

	@SuppressWarnings("unused")
	private static final Logger	LOGGER	= LoggerFactory.getLogger(DomainObjectTreeABC.class);

	@ManyToOne(optional = false, fetch = FetchType.LAZY)
	@Getter @Setter
	private P	parent;
	
	public DomainObjectTreeABC() {
		super();
	}
	
	public DomainObjectTreeABC(String inDomainId) {
		super(inDomainId);
	}
	
	// --------------------------------------------------------------------------
	/* (non-Javadoc)
	 * @see com.codeshelf.model.domain.IDomainObject#getFullDomainId()
	 */
	@JsonIgnore
	public String getFullDomainId() {
		return getParentFullDomainId() + "." + getDomainId();
	}
	
	// --------------------------------------------------------------------------
	/* (non-Javadoc)
	 * @see com.codeshelf.model.domain.IDomainObject#getFullParentDomainId()
	 */
	@SuppressWarnings("rawtypes")
	@JsonIgnore
	public String getParentFullDomainId() {
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
	 * @see com.codeshelf.model.domain.IDomainObject#getParentPersistentId()
	 */
	@JsonIgnore
	public UUID getParentPersistentId() {
		UUID result = null;
		IDomainObject domainObject = getParent();
		if (domainObject != null) {
			result = domainObject.getPersistentId();
		}
		return result;
	}
	
}
