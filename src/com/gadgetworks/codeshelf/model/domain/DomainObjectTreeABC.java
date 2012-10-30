/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: DomainObjectTreeABC.java,v 1.1 2012/10/30 15:21:34 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.model.domain;

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

//	@NonNull
//	@ManyToOne(optional = false)
//	@Column(nullable = false)
//	@JsonProperty
	//	@Getter
	//	@Setter
//	private P					parent;

	// --------------------------------------------------------------------------
	/* (non-Javadoc)
	 * @see com.gadgetworks.codeshelf.model.domain.IDomainObjectTree#getParent()
	 */
//	public final P getParent() {
//		return parent;
//	}

	// --------------------------------------------------------------------------
	/* (non-Javadoc)
	 * @see com.gadgetworks.codeshelf.model.domain.IDomainObjectTree#setParent(com.gadgetworks.codeshelf.model.domain.IDomainObjectTree)
	 */
//	public final void setParent(P inParent) {
//		parent = inParent;
//	}

	// --------------------------------------------------------------------------
	/* (non-Javadoc)
	 * @see com.gadgetworks.codeshelf.model.domain.IDomainObject#getFullDomainId()
	 */
	@JsonProperty
	public final String getFullDomainId() {
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
			if (theParent instanceof IDomainObject) {
				result = theParent.getDomainId();
			} else if (theParent instanceof IDomainObjectTree) {
				result = ((IDomainObjectTree) theParent).getFullDomainId();
			}
		}
		return result;
	}

	// --------------------------------------------------------------------------
	/* (non-Javadoc)
	 * @see com.gadgetworks.codeshelf.model.domain.IDomainObject#getParentPersistentId()
	 */
	@JsonProperty
	public final Long getParentPersistentId() {
		Long result = null;
		IDomainObject domainObject = getParent();
		if (domainObject != null) {
			result = domainObject.getPersistentId();
		}
		return result;
	}

}
