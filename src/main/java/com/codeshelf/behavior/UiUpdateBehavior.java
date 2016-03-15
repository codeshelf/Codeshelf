package com.codeshelf.behavior;

import java.sql.Timestamp;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.hibernate.Criteria;
import org.hibernate.criterion.Restrictions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.edi.InventoryCsvImporter;
import com.codeshelf.edi.InventorySlottedCsvBean;
import com.codeshelf.event.EventProducer;
import com.codeshelf.flyweight.command.ColorEnum;
import com.codeshelf.flyweight.command.NetGuid;
import com.codeshelf.manager.User;
import com.codeshelf.model.DeviceType;
import com.codeshelf.model.FacilityPropertyType;
import com.codeshelf.model.dao.DaoException;
import com.codeshelf.model.dao.ITypedDao;
import com.codeshelf.model.domain.Che;
import com.codeshelf.model.domain.Che.CheLightingEnum;
import com.codeshelf.model.domain.Che.ProcessMode;
import com.codeshelf.util.FormUtility;
import com.codeshelf.model.domain.CodeshelfNetwork;
import com.codeshelf.model.domain.ContainerUse;
import com.codeshelf.model.domain.Facility;
import com.codeshelf.model.domain.Gtin;
import com.codeshelf.model.domain.Item;
import com.codeshelf.model.domain.ItemMaster;
import com.codeshelf.model.domain.LedController;
import com.codeshelf.model.domain.Location;
import com.codeshelf.model.domain.OrderDetail;
import com.codeshelf.model.domain.OrderHeader;
import com.codeshelf.model.domain.ScannerTypeEnum;
import com.codeshelf.model.domain.SiteController;
import com.codeshelf.model.domain.SiteController.SiteControllerRole;
import com.codeshelf.model.domain.UomMaster;
import com.codeshelf.model.domain.WirelessDeviceABC;
import com.codeshelf.model.domain.WorkInstruction;
import com.codeshelf.validation.DefaultErrors;
import com.codeshelf.validation.ErrorCode;
import com.codeshelf.validation.InputValidationException;
import com.codeshelf.ws.protocol.message.PosConSetupMessage;
import com.codeshelf.ws.protocol.message.PropertyChangeMessage;
import com.codeshelf.ws.protocol.message.SiteControllerOperationMessage;
import com.codeshelf.ws.protocol.message.SiteControllerOperationMessage.SiteControllerTask;
import com.codeshelf.ws.protocol.message.PosConLightAddressesMessage;
import com.codeshelf.ws.server.WebSocketManagerService;
import com.google.common.base.Strings;
import com.google.inject.Inject;

// --------------------------------------------------------------------------
/**
 * This is a relatively unstructured collection of update methods that the UI may call.
 * There are other specialized services: LightService, PropertyService, WorkService
 * Much easier to add a new function to this than to create a new service.
 */
public class UiUpdateBehavior implements IApiBehavior {

	private static final Logger	LOGGER	= LoggerFactory.getLogger(UiUpdateBehavior.class);

	private WebSocketManagerService webSocketManagerService;
	
	@Inject
	public UiUpdateBehavior(WebSocketManagerService webSocketManagerService) {
		this.webSocketManagerService = webSocketManagerService;
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
		final String processModeStr,
		final String scannerTypeStr,
		final String cheLighting) {
		Facility facility = Facility.staticGetDao().findByPersistentId(facilityPersistentId);
		Che che = new Che();
		che.setParent(facility.getNetworks().get(0));
		return updateCheHelper(che, domainId, description, colorStr, controllerId, processModeStr, scannerTypeStr, cheLighting);
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
		final String processModeStr,
		final String scannerTypeStr,
		final String cheLightingStr) {
		Che che = Che.staticGetDao().findByPersistentId(cheId);

		if (che == null) {
			LOGGER.error("Could not find che {0}", cheId);
			return;
		}
		updateCheHelper(che, domainId, description, colorStr, controllerId, processModeStr, scannerTypeStr, cheLightingStr);
	}
	
	private UUID updateCheHelper(final Che che,
		final String domainId,
		final String description,
		final String colorStr,
		final String controllerId,
		final String processModeStr,
		final String scannerTypeStr,
		final String cheLightingStr) {
		ProcessMode processMode = ProcessMode.getMode(processModeStr);
		if (processMode == null) {
			LOGGER.error("Provide a valid processMode [SETUP_ORDERS,LINE_SCAN]");
			return null;
		}
		ScannerTypeEnum scannerType = ScannerTypeEnum.getScannerType(scannerTypeStr);
		if (scannerType == null) {
			LOGGER.error("Provide a valid scannerType [ORIGINALSERIAL,CODECORPS3600]");
			return null;
		}
		CheLightingEnum cheLighting = null;
		try {
			cheLighting = CheLightingEnum.valueOf(cheLightingStr);
		} catch (Exception e) {
			LOGGER.error("Provide a valid cheLighting [POSCON_V1,LABEL_V1,NOLIGHTING]");
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
		che.setScannerType(scannerType);
		che.setCheLighting(cheLighting);
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

	public void deleteChe(final String cheId) {
		Che che = Che.staticGetDao().findByPersistentId(cheId);

		if (che == null) {
			LOGGER.error("Could not find che {}", cheId);
			return;
		}
		LOGGER.info("Deleting che {}", cheId);
		List<ContainerUse> uses = che.getUses();
		for (ContainerUse use : uses) {
			OrderHeader header = use.getOrderHeader();
			if (header != null) {
				LOGGER.info("Null-out ContainerUse on OrderHeader {}", header.getDomainId());
				header.setContainerUse(null);
				OrderHeader.staticGetDao().store(header);
			}
			LOGGER.info("Deleting ContainerUse {}", use.getDomainId());
			ContainerUse.staticGetDao().delete(use);
		}
		List<WorkInstruction> instructions = che.getCheWorkInstructions();
		LOGGER.info("Deleting {} WorkeInstructions for che {}", instructions.size(), cheId);
		for (WorkInstruction instruction : instructions) {
			WorkInstruction.staticGetDao().delete(instruction);
		}
		Che.staticGetDao().delete(che);
	}
	
	public void deleleGtin(final String gtinPersistentId) {
		Gtin gtin = Gtin.staticGetDao().findByPersistentId(gtinPersistentId);
		if (gtin == null) {
			LOGGER.error("Could not find gtin {} to delete", gtinPersistentId);
			return;
		}
		LOGGER.info("Deleting gtin: {}", gtin);
		ItemMaster master = gtin.getParent();
		master.removeGtinFromMaster(gtin);
		try {
			Gtin.staticGetDao().delete(gtin);
		} catch (DaoException e) {
			LOGGER.error("deleteGtin", e);
		}		
	}

	public ProcessMode getDefaultProcessMode(String cheId) {
		// an artifact of new CHE dialog is we want the process type before we have a persistent ID
		return ProcessMode.SETUP_ORDERS;
		// Change v24. no-aisle facility used to yield LINE_SCAN. Now setup orders.
		// Instead of this mechanism, might want new CHE to be the modal value among existing CHE.

		/*
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
		*/
	}
	
	public void posConSetup(String deviceId, boolean isChe){
		WirelessDeviceABC device = null;
		if (isChe) { 
			device = Che.staticGetDao().findByPersistentId(deviceId);
		} else {
			device = LedController.staticGetDao().findByPersistentId(deviceId);
		}
		if (device == null) {
			LOGGER.error("Could not find " + (isChe?"che ":"controlelr ") + deviceId);
			return;
		}
		Set<User> users = device.getFacility().getSiteControllerUsers();
		PosConSetupMessage message = new PosConSetupMessage(device.getDeviceNetGuid().toString());
		WebSocketManagerService.getInstance().sendMessage(users, message);
	}
	
	public void posConLightAddresses(String deviceId, boolean isChe){
		WirelessDeviceABC device = null;
		if (isChe) { 
			device = Che.staticGetDao().findByPersistentId(deviceId);
		} else {
			device = LedController.staticGetDao().findByPersistentId(deviceId);
		}
		if (device == null) {
			LOGGER.error("Could not find " + (isChe?"che ":"controlelr ") + deviceId);
			return;
		}
		Set<User> users = device.getFacility().getSiteControllerUsers();
		PosConLightAddressesMessage message = new PosConLightAddressesMessage(device.getDeviceNetGuid().toString());
		WebSocketManagerService.getInstance().sendMessage(users, message);
	}
	
	/**
	 * Create a new LED or PosCon controller
	 */
	public UUID addController(
		final String facilityPersistentId,
		final String inNewControllerId,
		final String inNewDeviceType) {
		
		Facility facility = Facility.staticGetDao().findByPersistentId(facilityPersistentId);
		LedController controller = addControllerCallWithObjects(facility, inNewControllerId, inNewDeviceType);
		return controller.getPersistentId();
	}
	
	public LedController addControllerCallWithObjects(
		final Facility facility,
		final String inNewControllerId,
		final String inNewDeviceType) {
		CodeshelfNetwork network = facility.getNetworks().get(0);
		NetGuid guid = new NetGuid(inNewControllerId);
		String domainId = guid.getHexStringNoPrefix();
		
		LedController controller = network.findOrCreateLedController(domainId, guid);
		if (inNewDeviceType!=null) {
			DeviceType deviceType = DeviceType.valueOf(inNewDeviceType);
			controller.setDeviceType(deviceType);
			controller.getDao().store(controller);
		}
		return controller;
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
	
	public void updateFacilityProperty(String facilityId, String name, String value) {
		Facility facility = Facility.staticGetDao().findByPersistentId(facilityId);
		if (facility == null) {
			LOGGER.error("Unable to find facility {}", facilityId);
			return;
		}
		FacilityPropertyType type = FacilityPropertyType.valueOf(name);
		PropertyBehavior.setProperty(facility, type, value);
		webSocketManagerService.sendMessage(facility.getSiteControllerUsers(), new PropertyChangeMessage(type, value));
	}

	public CodeshelfNetwork getDefaultFacilityNetwork(String facilityId) {
		Facility facility = Facility.staticGetDao().findByPersistentId(facilityId);
		if (facility == null) {
			LOGGER.error("Unable to find facility {}", facilityId);
			return null;
		}
		return facility.getNetwork(CodeshelfNetwork.DEFAULT_NETWORK_NAME);
	}
	
	public void updateSiteController(String uuid, String domainId, String location, String networkDomainId){
		SiteController controller = SiteController.staticGetDao().findByPersistentId(uuid);
		if (controller == null) {
			FormUtility.throwUiValidationException("Site Controller", "Server Error: Site Controller " + uuid + " not found", ErrorCode.GENERAL);
		}
		if (!controller.getDomainId().equals(domainId)){
			//If site controller's domain id was changed, delete the old site controller and create a new one
			addSiteController(controller.getFacility().getPersistentId().toString(), domainId, location, networkDomainId);
			deleteSiteController(uuid);
		} else {
			CodeshelfNetwork prevNetwork = controller.getParent();
			CodeshelfNetwork network = validateSiteControllerParams(controller.getFacility(), domainId, location, networkDomainId);
			
			if (!prevNetwork.equals(network)){ 
				controller.setRole(SiteControllerRole.STANDBY);
				network.stealSiteController(controller);

				//Restart the modified site controller.
				SiteControllerOperationMessage shutdownMessage = new SiteControllerOperationMessage(SiteControllerTask.SHUTDOWN);
				Set<User> users = new HashSet<>();
				User user = controller.getUser();
				if (user != null) {
					users.add(user);
				}
				WebSocketManagerService.getInstance().sendMessage(users, shutdownMessage);			
			}
			//Modify location. But, if location was the only thing that changed, no need to restart site controller
			controller.setLocation(location);
			SiteController.staticGetDao().store(controller);			
		}
	}
	
	public void addSiteController(String facilityUUID, String domainId, String location, String networkDomainId){
		Facility facility = Facility.staticGetDao().findByPersistentId(facilityUUID);
		if (facility == null) {
			FormUtility.throwUiValidationException("Facility", "Server Error: Facility " + facilityUUID + " not found", ErrorCode.GENERAL);
		}
		CodeshelfNetwork network = validateSiteControllerParams(facility, domainId, location, networkDomainId);
		SiteController existingController = SiteController.staticGetDao().findByDomainId(network, domainId);
		if (existingController != null) {
			FormUtility.throwUiValidationException("Site Controller ID", "Site controller " + domainId + " already exists.", ErrorCode.FIELD_INVALID);
		}

		network.createSiteController(Integer.parseInt(domainId), location, false, SiteControllerRole.STANDBY);
		
	}
	
	private CodeshelfNetwork validateSiteControllerParams(Facility facility, String domainId, String location, String networkDomainId){
		if (domainId == null || domainId.isEmpty()) {
			FormUtility.throwUiValidationException("Site Controller ID", "Required Field.", ErrorCode.FIELD_REQUIRED);	
		}
		try {
			Integer.parseInt(domainId);
		} catch (Exception e) {
			FormUtility.throwUiValidationException("Site Controller ID", domainId + " is not an integer.", ErrorCode.FIELD_WRONG_TYPE);
		}
		if (location == null || location.isEmpty()) {
			FormUtility.throwUiValidationException("Location", "Required Field.", ErrorCode.FIELD_REQUIRED);	
		}
		if (networkDomainId == null || networkDomainId.isEmpty()) {
			FormUtility.throwUiValidationException("Network", "Required Field.", ErrorCode.FIELD_REQUIRED);	
		}
		CodeshelfNetwork network = facility.getNetwork(networkDomainId);
		if (network == null) {
			FormUtility.throwUiValidationException("Network", "Server Error: Network " + networkDomainId + " not found.", ErrorCode.FIELD_INVALID);
		}
		return network;
	}
	
	public void deleteSiteController(String uuid){
		SiteController controller = SiteController.staticGetDao().findByPersistentId(uuid);
		if (controller == null) {
			FormUtility.throwUiValidationException("Site Controller", "Server Error: Site Controller " + uuid + " not found", ErrorCode.GENERAL);
		}

		controller.getParent().removeSiteController(controller.getDomainId());
		Set<User> users = new HashSet<>();
		User user = controller.getUser();
		if (user != null) {
			users.add(user);
		}
		SiteControllerOperationMessage shutdownMessage = new SiteControllerOperationMessage(SiteControllerTask.SHUTDOWN);
		WebSocketManagerService.getInstance().sendMessage(users, shutdownMessage);
	}
	
	public void makeSiteControllerPrimaryForNetwork(String uuid){
		SiteController controller = SiteController.staticGetDao().findByPersistentId(uuid);
		if (controller == null) {
			FormUtility.throwUiValidationException("Site Controller", "Server Error: Site Controller " + uuid + " not found", ErrorCode.GENERAL);
		}

		//Update all site controllers on the network
		CodeshelfNetwork network = controller.getParent();
		for (SiteController c : network.getSiteControllers().values()) {
			if (controller.equals(c)){
				c.setRole(SiteControllerRole.NETWORK_PRIMARY);
			} else {
				c.setRole(SiteControllerRole.STANDBY);
			}
			SiteController.staticGetDao().store(c);
		}
		
		SiteControllerOperationMessage shutdownMessage = new SiteControllerOperationMessage(SiteControllerTask.SHUTDOWN);
		WebSocketManagerService.getInstance().sendMessage(network.getSiteControllerUsers(), shutdownMessage);
	}

}