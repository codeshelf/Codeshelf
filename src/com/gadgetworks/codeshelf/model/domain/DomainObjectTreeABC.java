/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: DomainObjectTreeABC.java,v 1.3 2012/12/25 10:48:13 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.model.domain;

import java.util.UUID;

import javax.persistence.Entity;

import lombok.ToString;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.jackson.annotate.JsonAutoDetect;
import org.codehaus.jackson.annotate.JsonAutoDetect.Visibility;
import org.codehaus.jackson.annotate.JsonProperty;

import com.avaje.ebean.annotation.CacheStrategy;

/**
 * @author jeffw
 *
 */

@Entity
@CacheStrategy
@ToString
@JsonAutoDetect(getterVisibility = Visibility.NONE)
public abstract class DomainObjectTreeABC<P extends IDomainObject> extends DomainObjectABC implements IDomainObjectTree<P> {

	private static final Log	LOGGER	= LogFactory.getLog(DomainObjectTreeABC.class);

	// --------------------------------------------------------------------------
	/* (non-Javadoc)
	 * @see com.gadgetworks.codeshelf.model.domain.IDomainObject#getFullDomainId()
	 */
	@JsonProperty
	public String getFullDomainId() {
		return getParentFullDomainId() + "." + getDomainId();
	}

	// --------------------------------------------------------------------------
	/* (non-Javadoc)
	 * @see com.gadgetworks.codeshelf.model.domain.IDomainObject#getFullParentDomainId()
	 */
	@JsonProperty
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
	@JsonProperty
	public final UUID getParentPersistentId() {
		UUID result = null;
		IDomainObject domainObject = getParent();
		if (domainObject != null) {
			result = domainObject.getPersistentId();
		}
		return result;
	}

}
