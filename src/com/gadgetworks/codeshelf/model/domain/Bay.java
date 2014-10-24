/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: Bay.java,v 1.21 2013/04/11 18:11:12 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.model.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.gadgetworks.codeshelf.model.dao.GenericDaoABC;
import com.gadgetworks.codeshelf.model.dao.ITypedDao;
import com.gadgetworks.codeshelf.platform.persistence.PersistenceService;
import com.google.common.collect.Ordering;
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
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE)
//@ToString(doNotUseGetters = true)
public class Bay extends SubLocationABC<Aisle> {


	@Inject
	public static ITypedDao<Bay>	DAO;

	@Singleton
	public static class BayDao extends GenericDaoABC<Bay> implements ITypedDao<Bay> {
		@Inject
		public BayDao(PersistenceService persistenceService) {
			super(persistenceService);
		}

		public final Class<Bay> getDaoClass() {
			return Bay.class;
		}
	}

	@SuppressWarnings("unused")
	private static final Logger	LOGGER	= LoggerFactory.getLogger(Bay.class);

	@SuppressWarnings("rawtypes")
	private static Comparator<ISubLocation> topDownTierOrder = new TopDownTierOrder();
	
	public Bay() {
		super();
	}
/*
	public Bay(Aisle parent, String domainId, Point inAnchorPoint, Point inPickFaceEndPoint) {
		super(parent, domainId, inAnchorPoint, inPickFaceEndPoint);
	}
	*/
	@SuppressWarnings("unchecked")
	public final ITypedDao<Bay> getDao() {
		return DAO;
	}

	public final String getDefaultDomainIdPrefix() {
		return "B";
	}
	
	public final String getBaySortName() {
		// to support list view meta-field tierSortName
		String bayName = this.getDomainId();
		String aisleName = "";
		Aisle aisleLocation = this.getParent();
				
		if (aisleLocation != null) {
			aisleName = aisleLocation.getDomainId();
		}
		return (aisleName + "-" + bayName);
	}

	public final String getBayIdForComparable() {
		return getCompString(getDomainId());
	}
	
	@Override
	public List<ILocation<?>> getSubLocationsInWorkingOrder() {
		@SuppressWarnings("rawtypes")
		List<ISubLocation> copy = new ArrayList<ISubLocation>(getActiveChildren());
		Collections.sort(copy, topDownTierOrder);
		List<ILocation<?>> result = new ArrayList<ILocation<?>>();
		for (ILocation<?> childLocation : copy) {
			// add sublocation
			result.add(childLocation);
			// and its sublocations recursively
			result.addAll(childLocation.getSubLocationsInWorkingOrder());
		}
		return result;

	}
	
	@SuppressWarnings("rawtypes")
	private static final class TopDownTierOrder implements Comparator<ISubLocation> {
		final Ordering<Double> doubleOrdering = Ordering.<Double>natural().reverse().nullsLast();

		@Override
		public int compare(ISubLocation o1, ISubLocation o2) {
			Double o1Z = o1.getAbsoluteAnchorPoint().getZ();
			Double o2Z = o2.getAbsoluteAnchorPoint().getZ();
			int result = doubleOrdering.compare(o1Z, o2Z); 
			return result;
		}
		
	}

	public static void setDao(ITypedDao<Bay> inBayDao) {
		Bay.DAO = inBayDao;
	}
	
	public Tier createTier(String inTierId, Point inAnchorPoint, Point inPickFaceEndPoint) {
		Tier tier = new Tier();
		tier.setDomainId(inTierId);
		tier.setAnchorPoint(inAnchorPoint);
		tier.setPickFaceEndPoint(inPickFaceEndPoint);
		
		this.addLocation((SubLocationABC<? extends IDomainObject>)tier);
		
		return tier;
	}

	@Override
	public void setParent(Aisle inParent) {
		this.setParent((ILocation<?>)inParent);
	}

}
