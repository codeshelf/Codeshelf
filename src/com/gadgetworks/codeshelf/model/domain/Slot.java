/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: Slot.java,v 1.3 2012/10/31 09:23:59 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.model.domain;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.Table;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.jackson.annotate.JsonAutoDetect;
import org.codehaus.jackson.annotate.JsonAutoDetect.Visibility;

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
@Table(name = "LOCATION")
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

	private static final Log	LOGGER	= LogFactory.getLog(Slot.class);

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
