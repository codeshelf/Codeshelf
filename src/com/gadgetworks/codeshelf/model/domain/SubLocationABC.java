/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: SubLocationABC.java,v 1.4 2013/03/15 14:57:13 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.model.domain;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.ManyToOne;
import javax.persistence.MappedSuperclass;

import lombok.ToString;

import org.codehaus.jackson.annotate.JsonAutoDetect;
import org.codehaus.jackson.annotate.JsonAutoDetect.Visibility;

import com.avaje.ebean.annotation.CacheStrategy;
import com.gadgetworks.codeshelf.model.PositionTypeEnum;
import com.gadgetworks.codeshelf.model.dao.GenericDaoABC;
import com.gadgetworks.codeshelf.model.dao.ITypedDao;
import com.google.inject.Inject;
import com.google.inject.Singleton;

@Entity
@MappedSuperclass
@CacheStrategy
@JsonAutoDetect(getterVisibility = Visibility.NONE)
@ToString(doNotUseGetters = true)
public abstract class SubLocationABC<P extends IDomainObject> extends LocationABC<P> {

	@Inject
	public static ITypedDao<SubLocationABC>	DAO;

	@Singleton
	public static class SubLocationDao extends GenericDaoABC<SubLocationABC> implements ITypedDao<SubLocationABC> {
		public final Class<SubLocationABC> getDaoClass() {
			return SubLocationABC.class;
		}
	}

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
