/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: SubLocationABC.java,v 1.10 2013/04/11 18:11:12 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.model.domain;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.ManyToOne;
import javax.persistence.MappedSuperclass;

import lombok.Getter;
import lombok.Setter;

import org.codehaus.jackson.annotate.JsonAutoDetect;
import org.codehaus.jackson.annotate.JsonAutoDetect.Visibility;
import org.codehaus.jackson.annotate.JsonProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.avaje.ebean.annotation.CacheStrategy;
import com.gadgetworks.codeshelf.model.PositionTypeEnum;
import com.gadgetworks.codeshelf.model.dao.GenericDaoABC;
import com.gadgetworks.codeshelf.model.dao.ISchemaManager;
import com.gadgetworks.codeshelf.model.dao.ITypedDao;
import com.google.inject.Inject;
import com.google.inject.Singleton;

@Entity
@MappedSuperclass
@CacheStrategy(useBeanCache = false)
@JsonAutoDetect(getterVisibility = Visibility.NONE)
//@ToString(doNotUseGetters = true)
public abstract class SubLocationABC<P extends IDomainObject> extends LocationABC<P> implements ISubLocation<P> {

	@Inject
	public static ITypedDao<SubLocationABC>	DAO;

	@Singleton
	public static class SubLocationDao extends GenericDaoABC<SubLocationABC> implements ITypedDao<SubLocationABC> {
		@Inject
		public SubLocationDao(final ISchemaManager inSchemaManager) {
			super(inSchemaManager);
		}

		public final Class<SubLocationABC> getDaoClass() {
			return SubLocationABC.class;
		}
	}

	private static final Logger	LOGGER	= LoggerFactory.getLogger(SubLocationABC.class);

	// The owning location.
	@Column(nullable = true)
	@ManyToOne(optional = true)
	private SubLocationABC		parent;

	@Column(nullable = false)
	@Enumerated(value = EnumType.STRING)
	@Getter
	@Setter
	@JsonProperty
	private PositionTypeEnum	pickFaceEndPosTypeEnum;

	// X pos of pick face end (pick face starts at anchor pos).
	@Column(nullable = false)
	@Getter
	@Setter
	@JsonProperty
	private Double				pickFaceEndPosX;

	// Y pos of pick face end (pick face starts at anchor pos).
	@Column(nullable = false)
	@Getter
	@Setter
	@JsonProperty
	private Double				pickFaceEndPosY;

	// Z pos of pick face end (pick face starts at anchor pos).
	@Column(nullable = false)
	@Getter
	@Setter
	@JsonProperty
	private Double				pickFaceEndPosZ;

	public SubLocationABC(final Point inAnchorPoint, final Point inPickFaceEndPoint) {
		super(inAnchorPoint);
		setPickFaceEndPosTypeEnum(inPickFaceEndPoint.getPosTypeEnum());
		setPickFaceEndPosX(inPickFaceEndPoint.getX());
		setPickFaceEndPosY(inPickFaceEndPoint.getY());
		setPickFaceEndPosZ(inPickFaceEndPoint.getZ());
	}

	// --------------------------------------------------------------------------
	/* (non-Javadoc)
	 * @see com.gadgetworks.codeshelf.model.domain.SubLocationABC#getParent()
	 */
	@Override
	public final P getParent() {
		// There's some weirdness with Ebean and navigating a recursive hierarchy. (You can't go down and then back up to a different class.)
		// This fixes that problem, but it's not pretty.
		//		return (P) parent;
		if (parent == null) {
			return (P) parent;
		} else {
			return (P) DAO.findByPersistentId(parent.getClass(), parent.getPersistentId());
		}
	}

	// --------------------------------------------------------------------------
	/* (non-Javadoc)
	 * @see com.gadgetworks.codeshelf.model.domain.SubLocationABC#setParent(P)
	 */
	@Override
	public final void setParent(P inParent) {
		parent = (SubLocationABC) inParent;
	}
}
