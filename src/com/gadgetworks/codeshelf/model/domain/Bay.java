/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: Bay.java,v 1.13 2012/10/30 15:21:34 jeffw Exp $
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
 * Bay
 * 
 * The object that models a storage bay in an aisle (pallet bay, etc.)
 * 
 * @author jeffw
 */

@Entity
@Table(name = "LOCATION")
@DiscriminatorValue("BAY")
@CacheStrategy
@JsonAutoDetect(getterVisibility = Visibility.NONE)
public class Bay extends LocationABC<Bay> {

	@Inject
	public static ITypedDao<Bay>	DAO;

	@Singleton
	public static class BayDao extends GenericDaoABC<Bay> implements ITypedDao<Bay> {
		public final Class<Bay> getDaoClass() {
			return Bay.class;
		}
	}

	private static final Log	LOGGER	= LogFactory.getLog(Bay.class);

	public Bay(final Aisle inAisle, final String inBayId, final Double inPosX, final double inPosY, final double inPosZ) {
		super(PositionTypeEnum.METERS_FROM_PARENT, inPosX, inPosY, inPosZ);
		setParent(inAisle);
		setDomainId(inBayId);
	}

	public final void setParentAisle(final Aisle inParentAisle) {
		setParent(inParentAisle);
	}

	public final ITypedDao<Bay> getDao() {
		return DAO;
	}

	public final String getDefaultDomainIdPrefix() {
		return "B";
	}
}
