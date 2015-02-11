/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: Bay.java,v 1.21 2013/04/11 18:11:12 jeffw Exp $
 *******************************************************************************/
package com.codeshelf.model.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.model.dao.GenericDaoABC;
import com.codeshelf.model.dao.ITypedDao;
import com.codeshelf.platform.persistence.TenantPersistenceService;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
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
public class Bay extends Location {


	@Inject
	public static ITypedDao<Bay>	DAO;

	@Singleton
	public static class BayDao extends GenericDaoABC<Bay> implements ITypedDao<Bay> {
		@Inject
		public BayDao(TenantPersistenceService tenantPersistenceService) {
			super(tenantPersistenceService);
		}

		public final Class<Bay> getDaoClass() {
			return Bay.class;
		}
	}

	@SuppressWarnings("unused")
	private static final Logger	LOGGER	= LoggerFactory.getLogger(Bay.class);

	private static Comparator<Location> topDownTierOrder = new TopDownTierOrder();
	
	public Bay() {
		super();
	}

	@SuppressWarnings("unchecked")
	public final ITypedDao<Bay> getDao() {
		return DAO;
	}

	public final String getDefaultDomainIdPrefix() {
		return "B";
	}
	
	public String getBaySortName() {
		// to support list view meta-field tierSortName
		String bayName = this.getDomainId();
		String aisleName = "";
		Aisle aisleLocation = this.<Aisle>getParentAtLevel(Aisle.class);
				
		if (aisleLocation != null) {
			aisleName = aisleLocation.getDomainId();
		}
		return (aisleName + "-" + bayName);
	}

	public String getBayIdForComparable() {
		return getCompString(getDomainId());
	}
	
	@Override
	public List<Location> getSubLocationsInWorkingOrder() {
		List<Location> copy = new ArrayList<Location>(getActiveChildren());
		Collections.sort(copy, topDownTierOrder);
		List<Location> result = new ArrayList<Location>();
		for (Location childLocation : copy) {
			// add sublocation
			result.add(childLocation);
			// and its sublocations recursively
			result.addAll(childLocation.getSubLocationsInWorkingOrder());
		}
		return result;

	}
	
	private static final class TopDownTierOrder implements Comparator<Location> {
		final Ordering<Double> doubleOrdering = Ordering.<Double>natural().reverse().nullsLast();

		@Override
		public int compare(Location o1, Location o2) {
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
		
		this.addLocation(tier);
		
		return tier;
	}
	
	@Override
	public boolean isBay() {
		return true;
	}
	
	public static Bay as(Location location) {
		if (location==null) {
			return null;
		}
		if (location.isBay()) {
	    	return (Bay) location;
	    }
		throw new RuntimeException("Location is not a bay: "+location);
	}
		
}
