/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: Slot.java,v 1.5 2013/03/04 04:47:27 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.model.domain;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

import org.codehaus.jackson.annotate.JsonAutoDetect;
import org.codehaus.jackson.annotate.JsonAutoDetect.Visibility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.avaje.ebean.annotation.CacheStrategy;
import com.gadgetworks.codeshelf.model.PositionTypeEnum;
import com.gadgetworks.codeshelf.model.dao.GenericDaoABC;
import com.gadgetworks.codeshelf.model.dao.ITypedDao;
import com.google.inject.Inject;
import com.google.inject.Singleton;

// --------------------------------------------------------------------------
/**
 * Slot
 * 
 * The object that models a slit within a tier.
 * 
 * @author jeffw
 */

@Entity
@DiscriminatorValue("SLOT")
@CacheStrategy
@JsonAutoDetect(getterVisibility = Visibility.NONE)
public class Slot extends SubLocationABC<Tier> {

	@Inject
	public static ITypedDao<Slot>	DAO;

	@Singleton
	public static class SlotDao extends GenericDaoABC<Slot> implements ITypedDao<Slot> {
		public final Class<Slot> getDaoClass() {
			return Slot.class;
		}
	}

	private static final Logger	LOGGER	= LoggerFactory.getLogger(Slot.class);

	public Slot(final Double inPosX, final double inPosY) {
		super(PositionTypeEnum.METERS_FROM_PARENT, inPosX, inPosY);
	}

	public final void setParentTier(final Tier inParentTier) {
		setParent(inParentTier);
	}

	public final ITypedDao<Slot> getDao() {
		return DAO;
	}

	public final String getDefaultDomainIdPrefix() {
		return "T";
	}
}
