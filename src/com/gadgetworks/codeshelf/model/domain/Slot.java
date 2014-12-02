/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: Slot.java,v 1.6 2013/04/11 07:42:44 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.model.domain;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.gadgetworks.codeshelf.model.dao.GenericDaoABC;
import com.gadgetworks.codeshelf.model.dao.ITypedDao;
import com.gadgetworks.codeshelf.platform.persistence.PersistenceService;
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
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE)
public class Slot extends LocationABC {

	@Inject
	public static ITypedDao<Slot>	DAO;

	@Singleton
	public static class SlotDao extends GenericDaoABC<Slot> implements ITypedDao<Slot> {
		@Inject
		public SlotDao(final PersistenceService persistenceService) {
			super(persistenceService);
		}

		public final Class<Slot> getDaoClass() {
			return Slot.class;
		}
	}

	@SuppressWarnings("unused")
	private static final Logger	LOGGER	= LoggerFactory.getLogger(Slot.class);
/*
	public Slot(Tier parent, String domainId, final Point inAnchorPoint, final Point inPickFaceEndPoint) {
		super(parent, domainId, inAnchorPoint, inPickFaceEndPoint);
	}
	*/
	public Slot() {
		super();
	}

	@SuppressWarnings("unchecked")
	public final ITypedDao<Slot> getDao() {
		return DAO;
	}

	public final String getDefaultDomainIdPrefix() {
		return "T";
	}
	
	public final String getSlotIdForComparable() {
		return getCompString(getDomainId());
	}

	public static void setDao(SlotDao inSlotDao) {
		Slot.DAO = inSlotDao;
	}
}
