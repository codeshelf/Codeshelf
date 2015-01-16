package com.gadgetworks.codeshelf.service;

import java.sql.Timestamp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gadgetworks.codeshelf.edi.InventoryCsvImporter;
import com.gadgetworks.codeshelf.edi.InventorySlottedCsvBean;
import com.gadgetworks.codeshelf.event.EventProducer;
import com.gadgetworks.codeshelf.model.domain.Che;
import com.gadgetworks.codeshelf.model.domain.Facility;
import com.gadgetworks.codeshelf.model.domain.Item;
import com.gadgetworks.codeshelf.model.domain.ItemMaster;
import com.gadgetworks.codeshelf.model.domain.Location;
import com.gadgetworks.codeshelf.model.domain.OrderDetail;
import com.gadgetworks.codeshelf.model.domain.UomMaster;
import com.gadgetworks.codeshelf.validation.DefaultErrors;
import com.gadgetworks.codeshelf.validation.ErrorCode;
import com.gadgetworks.codeshelf.validation.InputValidationException;
import com.gadgetworks.flyweight.command.ColorEnum;
import com.gadgetworks.flyweight.command.NetGuid;
import com.google.common.base.Strings;
// --------------------------------------------------------------------------
/**
 * This is a relatively unstructured collection of update methods that the UI may call.
 * There are other specialized services: LightService, PropertyService, WorkService
 * Much easier to add a new function to this than to create a new service.
 */
public class UiUpdateService implements IApiService {

	private static final Logger	LOGGER	= LoggerFactory.getLogger(UiUpdateService.class);

	public void UiUpdateService() {
	}

	// --------------------------------------------------------------------------
	/**
	 * Throw InputValidationException to make proper response to go back to UI. Avoid other throws.
	 */
	public void updateItemLocation() {
		
	}
	
	public Item upsertItem(String facilityId, String itemId, String storedLocationId, String cmDistanceFromLeft, String quantity, String inUomId, String orderDetailId) {
		Facility facility = Facility.DAO.findByPersistentId(facilityId);
		if (facility == null) {
			throw new InputValidationException("Facility {0} not found", facilityId);
		}
		
		//TODO This is a proof of concept and needs refactor to not have a dependency out of the EDI package
		storedLocationId = Strings.nullToEmpty(storedLocationId);

		InventoryCsvImporter importer = new InventoryCsvImporter(new EventProducer(), ItemMaster.DAO, Item.DAO, UomMaster.DAO);
		UomMaster uomMaster = importer.upsertUomMaster(inUomId, facility);

		ItemMaster itemMaster = facility.getItemMaster(itemId);
		InventorySlottedCsvBean itemBean = new InventorySlottedCsvBean();
		itemBean.setItemId(itemId);
		itemBean.setLocationId(storedLocationId);
		itemBean.setCmFromLeft(cmDistanceFromLeft);
		itemBean.setQuantity(quantity);
		itemBean.setUom(inUomId);
		Location location = facility.findSubLocationById(storedLocationId);
		if (location == null && !Strings.isNullOrEmpty(storedLocationId)) {
			DefaultErrors errors = new DefaultErrors(Item.class);
			errors.rejectValue("storedLocation", storedLocationId, ErrorCode.FIELD_REFERENCE_NOT_FOUND);
			throw new InputValidationException(errors);
		}

		Item returnItem = importer.updateSlottedItem(false,
			itemBean,
			location,
			new Timestamp(System.currentTimeMillis()),
			itemMaster,
			uomMaster);
		
		if (orderDetailId != null && !orderDetailId.isEmpty()) {
			OrderDetail detail = OrderDetail.DAO.findByPersistentId(orderDetailId);
			if (detail != null) {
				detail.setPreferredLocation(storedLocationId);
				OrderDetail.DAO.store(detail);
			}
		}
		return returnItem;
	}


	// --------------------------------------------------------------------------
	/**
	 * Internal API to update one property. Extensively used in JUnit testing, so will not log. Caller should log.
	 * Throw in a way that causes proper answer to go back to UI. Avoid other throws.
	 */
	public void updateCheEdits(final String cheId, final String domainId, final String description, final String colorStr, final String controllerId) {
		Che che = Che.DAO.findByPersistentId(cheId);

		if (che == null) {
			LOGGER.error("Could not find che {0}", cheId);
			return;
		}
		try {
			ColorEnum color = ColorEnum.valueOf(colorStr.toUpperCase());
			che.setColor(color);
		} catch (Exception e) {}
		if (domainId != null && !domainId.isEmpty()) {che.setDomainId(domainId);}
		if (description != null){che.setDescription(description);}
		try {
			// Perhaps this should be at ancestor level. CHE changes this field only. LED controller changes domain ID and controller ID.
			NetGuid currentGuid = che.getDeviceNetGuid();
			NetGuid newGuid = new NetGuid(controllerId);
			if (newGuid == null || currentGuid.equals(newGuid)) {return;}
			che.setDeviceNetGuid(newGuid);
			//Che.DAO.store(this);
		} catch (Exception e) {
			// Need to fix this. What kind of exception? Presumeably, bad controller ID that leads to invalid GUID
			LOGGER.error("Failed to set controller ID", e);
		}

		Che.DAO.store(che);
	}
}
