/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: DomainObjectTreeABC.java,v 1.6 2013/03/16 08:03:08 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.model.domain;

import java.util.UUID;

import javax.persistence.Entity;

import lombok.ToString;

import org.codehaus.jackson.annotate.JsonAutoDetect;
import org.codehaus.jackson.annotate.JsonAutoDetect.Visibility;
import org.codehaus.jackson.annotate.JsonIgnore;
import org.codehaus.jackson.annotate.JsonProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.avaje.ebean.annotation.CacheStrategy;

/**
 * @author jeffw
 *
 */

@Entity
@CacheStrategy(useBeanCache = true)
@JsonAutoDetect(getterVisibility = Visibility.NONE)
//@ToString(doNotUseGetters = true)
public abstract class DomainObjectTreeABC<P extends IDomainObject> extends DomainObjectABC implements IDomainObjectTree<P> {

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
