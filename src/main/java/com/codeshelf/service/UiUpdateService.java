package com.codeshelf.service;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.hibernate.Criteria;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Restrictions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.edi.InventoryCsvImporter;
import com.codeshelf.edi.InventorySlottedCsvBean;
import com.codeshelf.event.EventProducer;
import com.codeshelf.flyweight.command.ColorEnum;
import com.codeshelf.flyweight.command.NetGuid;
import com.codeshelf.model.DeviceType;
import com.codeshelf.model.dao.ITypedDao;
import com.codeshelf.model.domain.Aisle;
import com.codeshelf.model.domain.Che;
import com.codeshelf.model.domain.Che.ProcessMode;
import com.codeshelf.model.domain.CodeshelfNetwork;
import com.codeshelf.model.domain.Facility;
import com.codeshelf.model.domain.Item;
import com.codeshelf.model.domain.ItemMaster;
import com.codeshelf.model.domain.LedController;
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

	public Item storeItem(String facilityId,
		String itemId,
		String storedLocationId,
		String cmDistanceFromLeft,
		String quantity,
		String inUomId,
		String orderDetailId) {
		Facility facility = Facility.staticGetDao().findByPersistentId(facilityId);
		if (facility == null) {
			throw new InputValidationException("Facility {0} not found", facilityId);
		}

		storedLocationId = Strings.nullToEmpty(storedLocationId);

		//TODO This is a proof of concept and needs refactor to not have a dependency out of the EDI package
		InventoryCsvImporter importer = new InventoryCsvImporter(new EventProducer());
		UomMaster uomMaster = importer.upsertUomMaster(inUomId, facility);

		ItemMaster itemMaster = ItemMaster.staticGetDao().findByDomainId(facility, itemId);
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
			OrderDetail detail = OrderDetail.staticGetDao().findByPersistentId(orderDetailId);
			if (detail != null) {
				detail.setPreferredLocation(storedLocationId);
				OrderDetail.staticGetDao().store(detail);
			}
		}
		return returnItem;
	}

	// --------------------------------------------------------------------------
	/**
	 * Internal API to update one property. Extensively used in JUnit testing, so will not log. Caller should log.
	 * Throw in a way that causes proper answer to go back to UI. Avoid other throws.
	 */
	public UUID addChe(final String facilityPersistentId,
		final String domainId,
		final String description,
		final String colorStr,
		final String controllerId,
		final String processModeStr) {
		Facility facility = Facility.staticGetDao().findByPersistentId(facilityPersistentId);
		Che che = new Che();
		che.setParent(facility.getNetworks().get(0));
		ProcessMode processMode = ProcessMode.getMode(processModeStr);
		if (processMode == null) {
			LOGGER.error("Provide a valid processMode [SETUP_ORDERS,LINE_SCAN]");
			return null;
		}
		try {
			ColorEnum color = ColorEnum.valueOf(colorStr.toUpperCase());
			che.setColor(color);
		} catch (Exception e) {
		}
		if (domainId != null && !domainId.isEmpty()) {
			che.setDomainId(domainId);
		}
		if (description != null) {
			che.setDescription(description);
		}
		che.setProcessMode(processMode);
		try {
			// Perhaps this should be at ancestor level. CHE changes this field only. LED controller changes domain ID and controller ID.
			NetGuid currentGuid = che.getDeviceNetGuid();
			NetGuid newGuid = new NetGuid(controllerId);
			if (newGuid == null || currentGuid.equals(newGuid)) {
				return null;
			}
			che.setDeviceNetGuid(newGuid);
			//Che.staticGetDao().store(this);
		} catch (Exception e) {
			// Need to fix this. What kind of exception? Presumably, bad controller ID that leads to invalid GUID
			LOGGER.error("Failed to set controller ID", e);
		}

		Che.staticGetDao().store(che);
		return che.getPersistentId();
	}

	// --------------------------------------------------------------------------
	/**
	 * Internal API to update one property. Extensively used in JUnit testing, so will not log. Caller should log.
	 * Throw in a way that causes proper answer to go back to UI. Avoid other throws.
	 */
	public void updateChe(final String cheId,
		final String domainId,
		final String description,
		final String colorStr,
		final String controllerId,
		final String processModeStr) {
		Che che = Che.staticGetDao().findByPersistentId(cheId);

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
		} catch (Exception e) {
		}
		if (domainId != null && !domainId.isEmpty()) {
			che.setDomainId(domainId);
		}
		if (description != null) {
			che.setDescription(description);
		}
		che.setProcessMode(processMode);
		try {
			// Perhaps this should be at ancestor level. CHE changes this field only. LED controller changes domain ID and controller ID.
			NetGuid currentGuid = che.getDeviceNetGuid();
			NetGuid newGuid = new NetGuid(controllerId);
			if (newGuid == null || currentGuid.equals(newGuid)) {
				return;
			}
			che.setDeviceNetGuid(newGuid);
			//Che.staticGetDao().store(this);
		} catch (Exception e) {
			// Need to fix this. What kind of exception? Presumeably, bad controller ID that leads to invalid GUID
			LOGGER.error("Failed to set controller ID", e);
		}

		Che.staticGetDao().store(che);
	}

	public void deleteChe(final String cheId) {
		Che che = Che.staticGetDao().findByPersistentId(cheId);

		if (che == null) {
			LOGGER.error("Could not find che {0}", cheId);
			return;
		}
		Che.staticGetDao().delete(che);
	}

	public ProcessMode getDefaultProcessMode(String cheId) {
		// an artifact of new CHE dialog is we want the process type before we have a persistent ID
		if (cheId == null || cheId.isEmpty()) {
			return ProcessMode.SETUP_ORDERS;
		}

		Che che = Che.staticGetDao().findByPersistentId(cheId);
		if (che == null) {
			LOGGER.error("Could not find Che " + cheId);
			return ProcessMode.SETUP_ORDERS;
		}
		Facility facility = che.getFacility();
		List<Criterion> filterParams = new ArrayList<Criterion>();
		filterParams.add(Restrictions.eq("parent", facility));
		List<Aisle> aisled = Aisle.staticGetDao().findByFilter(filterParams);
		return (aisled.isEmpty()) ? ProcessMode.LINE_SCAN : ProcessMode.SETUP_ORDERS;
	}
	
	/**
	 * Create a new LED or PosCon controller
	 */
	public UUID addController(
		final String facilityPersistentId,
		final String inNewControllerId,
		final String inNewDeviceType) {
		
		Facility facility = Facility.staticGetDao().findByPersistentId(facilityPersistentId);
		CodeshelfNetwork network = facility.getNetworks().get(0);
		NetGuid guid = new NetGuid(inNewControllerId);
		String domainId = guid.getHexStringNoPrefix();
		
		LedController controller = network.findOrCreateLedController(domainId, guid);
		if (inNewDeviceType!=null) {
			DeviceType deviceType = DeviceType.valueOf(inNewDeviceType);
			controller.setDeviceType(deviceType);
			controller.getDao().store(controller);
		}
		return controller.getPersistentId();
	}
	
	/**
	 * Delete an LED or PosCon controller
	 */
	public void deleteController(final UUID controllerId) {
		ITypedDao<LedController> ledDao = LedController.staticGetDao();
		LedController controller = ledDao.findByPersistentId(controllerId);
		if (controller == null) {return;}
		ITypedDao<Location> locationDao = Location.staticGetLocationDao();
		Criteria crit = locationDao.createCriteria();
		crit.add(Restrictions.eq("ledController", controller));
		List<Location> locations = locationDao.findByCriteriaQuery(crit);
		for (Location location : locations) {
			location.setLedController(null);
			location.setLedChannel(null);
		}
		ledDao.delete(controller);
	}
}