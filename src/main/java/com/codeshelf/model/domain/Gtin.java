/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: ItemMaster.java,v 1.29 2013/09/18 00:40:09 jeffw Exp $
 *******************************************************************************/
package com.codeshelf.model.domain;

import java.util.List;
import java.util.UUID;

import javax.persistence.Cacheable;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import lombok.Getter;
import lombok.Setter;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Restrictions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.model.dao.GenericDaoABC;
import com.codeshelf.model.dao.ITypedDao;
import com.codeshelf.persistence.TenantPersistenceService;
import com.codeshelf.validation.ErrorCode;
import com.codeshelf.validation.InputValidationException;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.google.common.collect.ImmutableList;

// --------------------------------------------------------------------------
/**
 * GtinMap
 * 
 * A map between SKU and GTIN
 * 
 * @author huffa
 */

@Entity
@Table(name = "gtin", uniqueConstraints = {@UniqueConstraint(columnNames = {"parent_persistentid", "domainid"})})
@Cacheable
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE)
public class Gtin extends DomainObjectTreeABC<ItemMaster> {

	private static final Logger	LOGGER	= LoggerFactory.getLogger(Gtin.class);

	public static class GtinMapDao extends GenericDaoABC<Gtin> implements ITypedDao<Gtin> {
		public final Class<Gtin> getDaoClass() {
			return Gtin.class;
		}
	}

	// The actual UoM.
	@ManyToOne(optional = false, fetch = FetchType.LAZY)
	@JoinColumn(name = "uom_master_persistentid")
	@Getter
	@Setter
	private UomMaster	uomMaster;

	public Gtin() {
	}

	@Override
	public String toString() {
		String returnStr = getDomainId() + " for: " + getItemMasterId() + "/" + this.getUomMasterId();
		return returnStr;
	}

	public void setGtin(final String inGtin) {
		setDomainId(inGtin);
	}

	public String getGtin() {
		return getDomainId();
	}

	@Override
	public String getDefaultDomainIdPrefix() {
		return "GTIN";
	}

	@SuppressWarnings("unchecked")
	public final ITypedDao<Gtin> getDao() {
		return staticGetDao();
	}

	public static ITypedDao<Gtin> staticGetDao() {
		return TenantPersistenceService.getInstance().getDao(Gtin.class);
	}

	@Override
	public Facility getFacility() {
		return getParent().getFacility();
	}

	// Functions for UI
	public String getItemDescription() {
		ItemMaster theMaster = this.getParent();
		if (theMaster == null)
			return "";
		else {
			return theMaster.getDescription();
		}
	}

	public String getItemMasterId() {
		ItemMaster master = getParent();
		return master.getDomainId();
	}

	public UUID getitemMasterPersistentId() {
		ItemMaster master = getParent();
		return master.getPersistentId();
	}

	public String getUomMasterId() {
		// uom is not nullable, but we see null in unit test.
		UomMaster theUom = getUomMaster();
		if (theUom != null) {
			return theUom.getDomainId();
		} else {
			return "";
		}
	}

	public UUID getUomMasterPersistentId() {
		UomMaster theUom = getUomMaster();
		return theUom.getPersistentId();
	}

	/**
	 * This pattern recommended by Ivan. Avoids a join. Someday, we may have higher level application layers where Gtin is truly global, and ItemMaster is by customer across many facilities.
	 * For now, assume separate by facility and make sure it is reasonably efficient.
	 */
	public static Gtin getGtinForFacility(Facility facility, String scannedID) {
		if (scannedID == null || scannedID.isEmpty()) {
			LOGGER.error("null value in getGtinForFacility");
			return null;
		}
		List<Gtin> gtins = Gtin.staticGetDao().findByFilter(ImmutableList.<Criterion> of(Restrictions.eq("domainId", scannedID)));
		for (Gtin gtin : gtins) {
			// Just wondering. Might this fail due to tenant security constraints someday? 
			// gtins list may include same domainId for other facility. Then we have to getFacility() on that, which should not be allowed, as from facility you can get to anything.
			if (gtin.getFacility().equals(facility)) {
				return gtin;
			}
		}
		
		/*
		 * Eliminate this in v20. We require that gtins in the database be the real gtin, which means the value that will be scanned.
		 * Orders file import might pass gtin with stripped leading zeroes or something. That is dealt with in outboundOrderBatchProcessor
		 * 
		// If the GTIN was not found, we might have the odd case of leading zeroes. If so, EDI processing (especially a file that took a trip through a spreadsheet
		// may have stripped the leading zeroes. Still scans with the zeroes, bu the data base may have the gtin/upc without.
		if (scannedID.charAt(0) == '0') {
			// we are in a leading zero situation. Strip. This regex does not strip "0" down to blank, but otherwise strips leading zeros
			String strippedID = scannedID.replaceFirst("^0+(?!$)", "");
			// search again
			List<Gtin> gtin2s = Gtin.staticGetDao().findByFilter(ImmutableList.<Criterion> of(Restrictions.eq("domainId",
				strippedID)));
			for (Gtin gtin : gtin2s) {
				if (gtin.getFacility().equals(facility)) {
					LOGGER.warn("Found GTIN {} for value {}. Most likely data processing stripped leading zeros during order processing.",
						strippedID,
						scannedID);
					return gtin;
				}
			}

		}
		*/

		return null;
	}

	public boolean gtinIsAnOnboardingManufacture() {
		// If user scanned an inventory for an unknown GTIN, then we make phony item (to track the inventory) and phony itemMaster
		// because the item must have itemMaster. We give the GTIN ID. Aside from that, gtin and master domainID would never match.
		ItemMaster master = getParent();
		return getDomainId().equals(master.getDomainId());
	}

	/**
	 * This might go better in UiUpdaterService. But the direct column edit in WebApp makes this compelling.
	 * This edit is tricky. See OutboundOrdersWithGtinTest.java for the many business cases. We will keep it simple here.
	 * Validate, comply if we can, throw if we cannot.
	 * The validation is at least 8 chars long, and not a duplicate.
	 */
	public void setGtinUi(String inNewGtin) {
		// "this" is the gtin the user edited and presumably wants to keep.
		if (inNewGtin == null || inNewGtin.length() < 8) {
			LOGGER.warn("Disallow user changing Gtin {} to {} for SKU {}.", getDomainId(), inNewGtin, getItemMasterId());
			throw new InputValidationException(this, "Gtin/UPC", inNewGtin, ErrorCode.FIELD_GENERAL);
		}

		List<Gtin> gtins = Gtin.staticGetDao().findByFilter(ImmutableList.<Criterion> of(Restrictions.eq("domainId", inNewGtin)));
		if (gtins.isEmpty()) {
			// No conflict. Go ahead and change it.
			LOGGER.warn("User changing Gtin {} to {} for SKU {}.", getDomainId(), inNewGtin, getItemMasterId());
			setDomainId(inNewGtin);
			Gtin.staticGetDao().store(this);
		} else {
			boolean isOnboardingConversionCase = false;
			Item existingItemLocation = null;

			if (gtins.size() == 1) {
				Gtin existingGtin = gtins.get(0);
				if (existingGtin.gtinIsAnOnboardingManufacture()) {
					// it probably has an item location.
					ItemMaster existingMaster = existingGtin.getParent();
					List<Item> existingItems = existingMaster.getItems();
					if (existingItems.size() == 1) {
						existingItemLocation = existingItems.get(0);
						isOnboardingConversionCase = true;
					}
				}
			}
			if (isOnboardingConversionCase) {
				// would be nice to automatically fix, like the order import does.
				LOGGER.warn("User tried to change Gtin {} to {}. Need to delete {} and then reinventory {} to location {}",
					getDomainId(),
					inNewGtin,
					inNewGtin,
					getItemMasterId(),
					existingItemLocation.getItemLocationName());
			} else {
				LOGGER.warn("User tried to change Gtin {} to {} for SKU {}, but that Gtin exists. Delete it first.",
					getDomainId(),
					inNewGtin,
					getItemMasterId());
			}
			throw new InputValidationException(this, "Gtin/UPC", inNewGtin, ErrorCode.FIELD_REFERENCE_NOT_UNIQUE);
		}
	}

	/**
	 * Need getter defined for the UI field
	*/
	public String getGtinUi() {
		return getDomainId();
	}
	
	/**
	 * UI field is very useful to show what gtins have item locations.
	*/
	public String getGtinLocations() {
		ItemMaster master = getParent();
		List<Item> items = master.getItemsOfUom(getUomMasterId());
		String locsString = "";
		for(Item item :items) {
			if (!locsString.isEmpty()) {
				locsString += " , ";
			}
			locsString += item.getItemLocationName();
		}
		return locsString;
	}


}
