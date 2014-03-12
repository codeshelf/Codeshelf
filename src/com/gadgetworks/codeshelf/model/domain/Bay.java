/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: Bay.java,v 1.21 2013/04/11 18:11:12 jeffw Exp $
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
import com.gadgetworks.codeshelf.model.dao.ISchemaManager;
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
@DiscriminatorValue("BAY")
@CacheStrategy(useBeanCache = false)
@JsonAutoDetect(getterVisibility = Visibility.NONE)
//@ToString(doNotUseGetters = true)
public class Bay extends SubLocationABC<Aisle> {

	@Inject
	public static ITypedDao<Bay>	DAO;

	@Singleton
	public static class BayDao extends GenericDaoABC<Bay> implements ITypedDao<Bay> {
		@Inject
		public BayDao(final ISchemaManager inSchemaManager) {
			super(inSchemaManager);
		}
		
		public final Class<Bay> getDaoClass() {
			return Bay.class;
		}
	}

	private static final Logger	LOGGER	= LoggerFactory.getLogger(Bay.class);

	public Bay(final Aisle inAisle, final String inBayId, final Double inPosX, final double inPosY, final double inPosZ) {
		super(PositionTypeEnum.METERS_FROM_PARENT, inPosX, inPosY, inPosZ);
		setParent(inAisle);
		setDomainId(inBayId);
		inAisle.addLocation(this);
	}

	public final ITypedDao<Bay> getDao() {
		return DAO;
	}

	public final String getDefaultDomainIdPrefix() {
		return "B";
	}
}
