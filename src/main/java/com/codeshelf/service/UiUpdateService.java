package com.codeshelf.service;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Restrictions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.edi.InventoryCsvImporter;
import com.codeshelf.edi.InventorySlottedCsvBean;
import com.codeshelf.event.EventProducer;
import com.codeshelf.flyweight.command.ColorEnum;
import com.codeshelf.flyweight.command.NetGuid;
import com.codeshelf.model.domain.Aisle;
import com.codeshelf.model.domain.Che;
import com.codeshelf.model.domain.Che.ProcessMode;
import com.codeshelf.model.domain.Facility;
import com.codeshelf.model.domain.Item;
import com.codeshelf.model.domain.ItemMaster;
import com.codeshelf.model.domain.Location;
import com.codeshelf.model.domain.OrderDetail;
import com.codeshelf.model.domain.UomMaster;
import com.codeshelf.validation.DefaultErrors;
import com.codeshelf.validation.ErrorCode;
import com.codeshelf.validation.InputValidationException;
import com.google.common.base.Strings;
// --------------------------------------------------------------------------
/**
 * This is a relatively unstructured collection of update methods that the UI may call.
 * There are other specialized services: LightService, PropertyService, WorkService
 * Much easier to add a new function to this than to create a new service.
 */
public class UiUpdateService implements IApiService {

	private static final Logger	LOGGER	= LoggerFactory.getLogger(UiUpdateService.class);

	public UiUpdateService() {
	}

	public Item storeItem(String facilityId, String itemId, String storedLocationId, String cmDistanceFromLeft, String quantity, String inUomId, String orderDetailId) {
		Facility facility = Facility.DAO.findByPersistentId(facilityId);
		if (facility == null) {
			throw new InputValidationException("Facility {0} not found", facilityId);
		}
		
		storedLocationId = Strings.nullToEmpty(storedLocationId);

		//TODO This is a proof of concept and needs refactor to not have a dependency out of the EDI package
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
	public void updateCheEdits(final String cheId, final String domainId, final String description, final String colorStr, final String controllerId, final String processModeStr) {
		Che che = Che.DAO.findByPersistentId(cheId);

		if (che == null) {
			LOGGER.error("Could not find che {0}", cheId);
			return;
		}
		ProcessMode processMode = ProcessMode.getMode(processModeStr);
		if (processMode == null) {
			LOGGER.error("Provide a valid processMode [SETUP_ORDERS,LINE_SCAN]");
			return;
		}
		try {
			ColorEnum color = ColorEnum.valueOf(colorStr.toUpperCase());
			che.setColor(color);
		} catch (Exception e) {}
		if (domainId != null && !domainId.isEmpty()) {che.setDomainId(domainId);}
		if (description != null){che.setDescription(description);}
		che.setProcessMode(processMode);
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
	
	public ProcessMode getDefaultProcessMode(String cheId){
		Che che = Che.DAO.findByPersistentId(cheId);
		if (che == null) {
			LOGGER.error("Could not find Che " + cheId);
			return ProcessMode.SETUP_ORDERS;
		}
		Facility facility = che.getFacility();
		List<Criterion> filterParams = new ArrayList<Criterion>();
		filterParams.add(Restrictions.eq("parent", facility));
		List<Aisle> aisled = Aisle.DAO.findByFilter(filterParams);
		return (aisled.isEmpty())? ProcessMode.LINE_SCAN : ProcessMode.SETUP_ORDERS;
	}
}