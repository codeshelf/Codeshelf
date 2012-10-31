/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: SubLocationABC.java,v 1.1 2012/10/31 09:23:59 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.model.domain;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.ManyToOne;
import javax.persistence.MappedSuperclass;

import org.codehaus.jackson.annotate.JsonAutoDetect;
import org.codehaus.jackson.annotate.JsonAutoDetect.Visibility;

import com.avaje.ebean.annotation.CacheStrategy;
import com.gadgetworks.codeshelf.model.PositionTypeEnum;

@Entity
@MappedSuperclass
@CacheStrategy
@JsonAutoDetect(getterVisibility = Visibility.NONE)
public abstract class SubLocationABC<P extends IDomainObject> extends LocationABC<P> {

	// The owning location.
	@Column(nullable = false)
	@ManyToOne(optional = true)
	private LocationABC	parent;

	public SubLocationABC(final PositionTypeEnum inPosType, final Double inPosX, final double inPosY) {
		super(inPosType, inPosX, inPosY);
	}

	public SubLocationABC(final PositionTypeEnum inPosType, final Double inPosX, final double inPosY, final double inPosZ) {
		super(inPosType, inPosX, inPosY, inPosZ);
	}

	public final P getParent() {
		return (P) parent;
	}

	public final void setParent(P inParent) {
		parent = (LocationABC) inParent;
	}
}
