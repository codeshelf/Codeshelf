package com.codeshelf.api.pickscript;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.api.resources.subresources.EDIGatewaysResource;
import com.codeshelf.behavior.UiUpdateBehavior;
import com.codeshelf.device.ScriptSiteRunner;
import com.codeshelf.edi.EdiExportService;
import com.codeshelf.edi.ICsvAislesFileImporter;
import com.codeshelf.edi.ICsvInventoryImporter;
import com.codeshelf.edi.ICsvLocationAliasImporter;
import com.codeshelf.edi.ICsvOrderImporter;
import com.codeshelf.edi.ICsvWorkerImporter;
import com.codeshelf.flyweight.command.ColorEnum;
import com.codeshelf.flyweight.command.NetGuid;
import com.codeshelf.model.DeviceType;
import com.codeshelf.model.EdiTransportType;
import com.codeshelf.model.FacilityPropertyType;
import com.codeshelf.model.PositionTypeEnum;
import com.codeshelf.model.domain.Aisle;
import com.codeshelf.model.domain.Bay;
import com.codeshelf.model.domain.Che;
import com.codeshelf.model.domain.CodeshelfNetwork;
import com.codeshelf.model.domain.Che.CheLightingEnum;
import com.codeshelf.model.domain.Che.ProcessMode;
import com.codeshelf.model.domain.ExtensionPoint;
import com.codeshelf.model.domain.Facility;
import com.codeshelf.model.domain.LedController;
import com.codeshelf.model.domain.Location;
import com.codeshelf.model.domain.OrderHeader;
import com.codeshelf.model.domain.Path;
import com.codeshelf.model.domain.PathSegment;
import com.codeshelf.model.domain.Point;
import com.codeshelf.model.domain.ScannerTypeEnum;
import com.codeshelf.model.domain.Tier;
import com.codeshelf.persistence.TenantPersistenceService;
import com.codeshelf.security.CodeshelfSecurityManager;
import com.codeshelf.service.ExtensionPointEngine;
import com.codeshelf.service.ExtensionPointType;
import com.codeshelf.util.CsExceptionUtils;
import com.codeshelf.validation.BatchResult;
import com.codeshelf.ws.protocol.message.ScriptMessage;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;
import com.google.inject.Provider;
import com.sun.jersey.core.util.MultivaluedMapImpl;
import com.sun.jersey.multipart.FormDataMultiPart;

public class ScriptServerRunner {
	private final static String TEMPLATE_EDIT_FACILITY = "editFacility <facility domain id> <primary site controller id> <primary radio channel>";
	private final static String TEMPLATE_OUTLINE = "createDummyOutline [size]";
	private final static String TEMPLATE_SET_PROPERTY = "setProperty <name> <value>";
	private final static String TEMPLATE_DELETE_ORDERS = "deleteOrders <filename>";
	private final static String TEMPLATE_IMPORT_ORDERS = "importOrders <orders>";
	private final static String TEMPLATE_IMPORT_ORDERS_DELETION = "importOrdersWithDeletion <orders>";
	private final static String TEMPLATE_IMPORT_AISLES = "importAisles <filename>";
	private final static String TEMPLATE_IMPORT_LOCATIONS = "importLocations <filename>";
	private final static String TEMPLATE_IMPORT_INVENTORY = "importInventory <filename>";
	private final static String TEMPLATE_IMPORT_WORKERS = "importWorkers <filename>";
	private final static String TEMPLATE_SET_CONTROLLER = "setController <location> <lights/poscons> <controller> <channel> ['tiersInAisle']";
	private final static String TEMPLATE_SET_POSCONS = "setPoscons (assignments <tier> <startIndex> <'forward'/'reverse'>)";
	private final static String TEMPLATE_SET_POSCON_TO_BAY = "setPosconToBay (assignments <bay name> <controller> <poscon id>)";
	private final static String TEMPLATE_SET_WALL = "setWall <aisle> <off/putwall/skuwall>";
	private final static String TEMPLATE_CREATE_CHE = "createChe <che> <color> <mode> [name] [scannerType] [cheLighting]";
	private final static String TEMPLATE_DELETE_CHES = "deleteChes (<ches>)";
	private final static String TEMPLATE_DELETE_ALL_PATHS = "deleteAllPaths";
	private final static String TEMPLATE_DEF_PATH = "defPath <path id> (segments '-' <start x> <start y> <end x> <end y>)";
	private final static String TEMPLATE_SET_PATH_NAME = "setPathName <path id> [path name (spaces allowed)]";
	private final static String TEMPLATE_ASSIGN_PATH_SGM_AISLE = "assignPathSgmToAisle <pathid > <segment id> <aisle name>";
	private final static String TEMPLATE_ASSIGN_TAPE_TO_TIER = "assignTapeToTier (assignments <tape id> <tier name>)";
	private final static String TEMPLATE_DELETE_ALL_EXTENSIONS = "deleteAllExtensionPoints";
	private final static String TEMPLATE_DELETE_EXTENSION = "deleteExtensionPoint <type>";
	private final static String TEMPLATE_ADD_EXTENSION = "addExtensionPoint <filename> <type> <active/inactive>";
	private final static String TEMPLATE_SFTP = "sftp <'orders'/'wi'> <host> <port> <username> <password> <directories: in and out for orders, out for wi>";
	private final static String TEMPLATE_WAIT_SECONDS = "waitSeconds <seconds>";
	
	private static final Logger LOGGER = LoggerFactory.getLogger(ScriptSiteRunner.class);
	private final UUID facilityId;
	private final UiUpdateBehavior uiUpdateBehavior;
	private final Provider<ICsvOrderImporter> orderImporterProvider;
	private final Provider<ICsvAislesFileImporter> aisleImporterProvider;
	private final Provider<ICsvLocationAliasImporter> locationsImporterProvider;
	private final Provider<ICsvInventoryImporter> inventoryImporterProvider;
	private final Provider<ICsvWorkerImporter> workerImporterProvider;
	private final FormDataMultiPart postBody;
	private StringBuilder report;
	private Facility facility;
	private HashMap<String, Path> paths = new HashMap<String, Path>();
	
	public ScriptServerRunner(
		UUID facilityId,
		FormDataMultiPart postBody,
		UiUpdateBehavior uiUpdateBehavior,
		Provider<ICsvAislesFileImporter> aisleImporterProvider,
		Provider<ICsvLocationAliasImporter> locationsImporterProvider,
		Provider<ICsvInventoryImporter> inventoryImporterProvider,
		Provider<ICsvOrderImporter> orderImporterProvider,
		Provider<ICsvWorkerImporter> workerImporterProvider) { 
		this.facilityId = facilityId;
		this.postBody = postBody;
		this.uiUpdateBehavior = uiUpdateBehavior;
		this.aisleImporterProvider = aisleImporterProvider;
		this.locationsImporterProvider = locationsImporterProvider;
		this.inventoryImporterProvider = inventoryImporterProvider;
		this.orderImporterProvider = orderImporterProvider;
		this.workerImporterProvider = workerImporterProvider;
	}
	
	public ScriptMessage processServerScript(List<String> lines){
		//Run the SERVER step of the script, creating a new transaction for every line of the script
		TenantPersistenceService persistence = TenantPersistenceService.getInstance();
		ScriptMessage message = new ScriptMessage();
		report = new StringBuilder();
		report.append("SERVER\n");
		boolean success = true;
		for (String line : lines) {
			persistence.beginTransaction();
			try {
				facility = Facility.staticGetDao().findByPersistentId(facilityId);
				processLine(line);
				persistence.commitTransaction();
			} catch (Exception e) {
				persistence.rollbackTransaction();
				report.append(CsExceptionUtils.exceptionToString(e)).append("\n");
				message.setMessageError(CsExceptionUtils.exceptionToString(e) + "\n");
				success = false;
				break;
			}
		}
		if (success) {
			report.append("***Server Script Segment Completed Successfully***\n");
		}
		message.setResponse(report.toString());
		LOGGER.info("Server script block completed");
		return message;
	}
	
	private void processLine(String line) throws Exception {
		LOGGER.info("Runing script line " + line);
		report.append("Run: ").append(line).append("\n");
		if (line == null || line.isEmpty()) {
			return;
		}
		String parts[] = ScriptParser.splitLine(line);
		
		String command = parts[0];
		if (command.equalsIgnoreCase("editFacility")) {
			processEditFacilityCommand(parts);
		} else if (command.equalsIgnoreCase("createDummyOutline")) {
			processOutlineCommand(parts);
		} else if (command.equalsIgnoreCase("setProperty")) {
			processSetPropertyCommand(parts);
		} else if (command.equalsIgnoreCase("deleteOrders")) {
			processDeleteOrdersCommand(parts);
		} else if (command.equalsIgnoreCase("importOrders")) {
			processImportOrdersCommand(parts, false);
		} else if (command.equalsIgnoreCase("importOrdersWithDeletion")) {
			processImportOrdersCommand(parts, true);
		} else if (command.equalsIgnoreCase("importAisles")) {
			processImportAislesCommand(parts);
		} else if (command.equalsIgnoreCase("importLocations")) {
			processImportLocationsCommand(parts);
		} else if (command.equalsIgnoreCase("importInventory")) {
			processImportInventory(parts);
		} else if (command.equalsIgnoreCase("importWorkers")) {
			processImportWorkers(parts);
		} else if (command.equalsIgnoreCase("setController")) {
			processSetLedControllerCommand(parts);
		} else if (command.equalsIgnoreCase("setPoscons")) {
			processSetPosconsCommand(parts);
		} else if (command.equalsIgnoreCase("setPosconToBay")) {
			processSetPosconToBayCommand(parts);
		} else if (command.equalsIgnoreCase("setWall")) {
			processSetWallCommand(parts);
		} else if (command.equalsIgnoreCase("createChe")) {
			processCreateCheCommand(parts);
		} else if (command.equalsIgnoreCase("deleteChes")) {
			processDeleteChesCommand(parts);
		} else if (command.equalsIgnoreCase("deleteAllPaths")) {
			processDeleteAllPathsCommand(parts);
		} else if (command.equalsIgnoreCase("defPath")) {
			processDefinePathCommand(parts);
		} else if (command.equalsIgnoreCase("setPathName")) {
			processSetPathNameCommand(parts);
		} else if (command.equalsIgnoreCase("assignPathSgmToAisle")) {
			processAsignPathSegmentToAisleCommand(parts);
		} else if (command.equalsIgnoreCase("assignTapeToTier")) {
			processAsignTapeToTierCommand(parts);
		} else if (command.equalsIgnoreCase("deleteAllExtensionPoints")) {
			processDeleteAllExtensionPointsCommand(parts);
		} else if (command.equalsIgnoreCase("deleteExtensionPoint")) {
			processDeleteExtensionPointCommand(parts);
		} else if (command.equalsIgnoreCase("addExtensionPoint")) {
			processAddExtensionPointCommand(parts);
		} else if (command.equalsIgnoreCase("sftp")) {
			processSftpCommand(parts);
		} else if (command.equalsIgnoreCase("waitSeconds")) {
			processWaitSecondsCommand(parts);
		} else if (command.startsWith("//")) {
		} else if  (command.equalsIgnoreCase("togglePutWall")) {
			throw new Exception("Command togglePutWall has been deprecated due to an addition of Sku Walls. Instead, use " + TEMPLATE_SET_WALL);
		} else {
			throw new Exception("Invalid command '" + command + "'. Expected [editFacility, createDummyOutline, setProperty, deleteOrders, importOrders, importOrdersWithDeletion, importAisles, importLocations, importInventory, importWorkers, setController, setPoscons, setPosconToBay, setWall, createChe, deleteChes, deleteAllPaths, defPath, setPathName, assignPathSgmToAisle, assignTapeToTier, deleteAllExtensionPoints, deleteExtensionPoint, addExtensionPoint, waitSeconds, //]");
		}
	}

	/**
	 * Expects to see command
	 * editFacility <facility domain id> <primary site controller id> <primary radio channel>
	 * @throws Exception 
	 */
	private void processEditFacilityCommand(String parts[]) throws Exception {
		if (parts.length != 4){
			throwIncorrectNumberOfArgumentsException(TEMPLATE_EDIT_FACILITY);
		}
		String domainId = parts[1];
		Facility facilityWithSameDomainId = Facility.staticGetDao().findByDomainId(null, domainId);
		if (facilityWithSameDomainId != null && !facility.equals(facilityWithSameDomainId)) {
			throw new Exception("Another facility " + domainId + " already exists");
		}
		facility.setDomainId(parts[1]);
		facility.setPrimarySiteControllerId(parts[2]);
		facility.setPrimaryChannel(Short.parseShort(parts[3]));
	}

	/**
	 * Expects to see command
	 * createDummyOutline [size]
	 * @throws Exception 
	 */
	private void processOutlineCommand(String parts[]) throws Exception {
		if (parts.length > 2){
			throwIncorrectNumberOfArgumentsException(TEMPLATE_OUTLINE);
		}
		int facilitySize = 1;
		if (parts.length == 2) {
			facilitySize = Integer.parseInt(parts[1]);
		}
		if (facilitySize < 1) {
			throw new Exception("Invalid facility size " + facilitySize + ".");
		}
		double anchorX = -122.271673679351807, anchorY = 37.8032855078260113, sideX = 0.00020 * facilitySize, sideY = 0.00016 * facilitySize;
		facility.createOrUpdateVertex("V0", "GPS", anchorX, anchorY, 0);
		facility.createOrUpdateVertex("V1", "GPS", anchorX, anchorY - sideY, 1);
		facility.createOrUpdateVertex("V2", "GPS", anchorX + sideX, anchorY - sideY, 2);
		facility.createOrUpdateVertex("V3", "GPS", anchorX + sideX, anchorY, 3);
	}

	/**
	 * Expects to see command
	 * setProperty <name> <value>
	 * @throws Exception 
	 */
	private void processSetPropertyCommand(String parts[]) throws Exception {
		if (parts.length != 3){
			throwIncorrectNumberOfArgumentsException(TEMPLATE_SET_PROPERTY);
		}
		String name = parts[1].toUpperCase(), value = parts[2]; 
		if (FacilityPropertyType.valueOf(name) == null) {
			throw new Exception("Property " + name + " doesn't exist");
		}
		uiUpdateBehavior.updateFacilityProperty(facilityId.toString(), name, value);
	}

	/**
	 * Expects to see command
	 * deleteOrders <orders>
	 * @throws Exception 
	 */
	private void processDeleteOrdersCommand(String parts[]) throws Exception {
		if (parts.length < 2){
			throwIncorrectNumberOfArgumentsException(TEMPLATE_DELETE_ORDERS);
		}
		for (int i = 1; i < parts.length; i++) {
			String orderId = parts[i];
			OrderHeader order = OrderHeader.staticGetDao().findByDomainId(facility, orderId);
			if (order != null) {
				order.delete();
			} else {
				LOGGER.info("Warning: While running script, did not find order {}. Continuing with execution", orderId);
			}
		}
	}

	/**
	 * Expects to see command
	 * importOrders <filename>
	 * or
	 * importOrdersWithDeletion <filename>
	 * @throws Exception 
	 */
	private void processImportOrdersCommand(String parts[], boolean deleteOldOrders) throws Exception {
		if (parts.length != 2){
			if (deleteOldOrders){
				throwIncorrectNumberOfArgumentsException(TEMPLATE_IMPORT_ORDERS_DELETION);
			} else {
				throwIncorrectNumberOfArgumentsException(TEMPLATE_IMPORT_ORDERS);
			}
		}
		String filename = parts[1];
		InputStreamReader reader = readFile(filename);
		long receivedTime = System.currentTimeMillis();
		ICsvOrderImporter orderImporter = orderImporterProvider.get();
		BatchResult<Object> results = orderImporter.importOrdersFromCsvStream(reader, facility, new Timestamp(System.currentTimeMillis()), deleteOldOrders);
		String username = CodeshelfSecurityManager.getCurrentUserContext().getUsername();
		orderImporter.persistDataReceipt(facility, username, filename + ".csv", receivedTime, EdiTransportType.SCRIPT, results);
	}

	/**
	 * Expects to see command
	 * importAisles <filename>
	 * @throws Exception 
	 */
	private void processImportAislesCommand(String parts[]) throws Exception {
		if (parts.length != 2){
			throwIncorrectNumberOfArgumentsException(TEMPLATE_IMPORT_AISLES);
		}
		String filename = parts[1];
		InputStreamReader reader = readFile(filename);
		aisleImporterProvider.get().importAislesFileFromCsvStream(reader, facility, new Timestamp(System.currentTimeMillis()));
	}

	/**
	 * Expects to see command
	 * importLocations <filename>
	 * @throws Exception 
	 */
	private void processImportLocationsCommand(String parts[]) throws Exception {
		if (parts.length != 2){
			throwIncorrectNumberOfArgumentsException(TEMPLATE_IMPORT_LOCATIONS);
		}
		String filename = parts[1];
		InputStreamReader reader = readFile(filename);
		locationsImporterProvider.get().importLocationAliasesFromCsvStream(reader, facility, new Timestamp(System.currentTimeMillis()));
	}

	/**
	 * Expects to see command
	 * importInventory <filename>
	 * @throws Exception 
	 */
	private void processImportInventory(String parts[]) throws Exception {
		if (parts.length != 2){
			throwIncorrectNumberOfArgumentsException(TEMPLATE_IMPORT_INVENTORY);
		}
		String filename = parts[1];
		InputStreamReader reader = readFile(filename);
		inventoryImporterProvider.get().importSlottedInventoryFromCsvStream(reader, facility, new Timestamp(System.currentTimeMillis()));
	}

	/**
	 * Expects to see command
	 * importWorkers <filename>
	 * @throws Exception 
	 */
	private void processImportWorkers(String parts[]) throws Exception {
		if (parts.length != 2){
			throwIncorrectNumberOfArgumentsException(TEMPLATE_IMPORT_WORKERS);
		}
		String filename = parts[1];
		InputStreamReader reader = readFile(filename);
		workerImporterProvider.get().importWorkersFromCsvStream(reader, facility, false, new Timestamp(System.currentTimeMillis()));
	}

	/**
	 * Expects to see command
	 * setLedController <location> <type lights/poscons> <controller> <channel> ['tiersInAisle']
	 * @throws Exception 
	 */
	private void processSetLedControllerCommand(String parts[]) throws Exception {
		if (parts.length != 5 && parts.length != 6){
			throwIncorrectNumberOfArgumentsException(TEMPLATE_SET_CONTROLLER);
		}
		//Verify device type and fix capitalization
		String type = null;
		if ("Lights".equalsIgnoreCase(parts[2])){
			type = DeviceType.Lights.toString();
		} else if ("Poscons".equalsIgnoreCase(parts[2])){
			type = DeviceType.Poscons.toString();
		} else {
			throw new Exception("Invalid controller type " + parts[2] + " (lights/poscons)");
		}
		if (parts.length == 6 && !parts[5].equalsIgnoreCase("tiersInAisle")){
			throw new Exception("The optional 4th parameter in the 'setLedController' command has to be 'tiersInAisle'");
		}
		String controllerName = parts[3];
		String controllerChannel = parts[4];

		//Find or create Controller
		LedController controller = uiUpdateBehavior.addControllerCallWithObjects(facility, controllerName, type);
		
		//Find Location
		String locationName = parts[1];
		Location location = facility.findSubLocationById(locationName);
		if (location == null) {
			throw new Exception("Could not find location " + locationName);
		}
		
		//Assign controller to a given Aisle, given Tier, or to all Tiers in an Aisle of a given Tier
		if (location.isAisle()){
			Aisle aisle = Aisle.staticGetDao().findByPersistentId(location.getPersistentId());
			aisle.setControllerChannel(controller.getPersistentId().toString(), controllerChannel);
		} else if(location.isTier()){
			String tiersInAisle = parts.length==6 ? Tier.ALL_TIERS_IN_AISLE : Tier.THIS_TIER_ONLY;
			Tier tier = Tier.staticGetDao().findByPersistentId(location.getPersistentId());
			tier.setControllerChannel(controller.getPersistentId().toString(), controllerChannel, tiersInAisle);
		} else {
			throw new Exception(location.getClassName() + " " + locationName + " is not an Aisle or a Tier");
		}
	}

	/**
	 * Expects to see command
	 * setPoscons (assignments <tier> <startIndex> <'forward'/'reverse'>)
	 * @throws Exception 
	 */
	private void processSetPosconsCommand(String parts[]) throws Exception {
		int blockLength = 3;
		if (parts.length < 4 || (parts.length - 1) % blockLength != 0 ){
			throwIncorrectNumberOfArgumentsException(TEMPLATE_SET_POSCONS);
		}
		int totalBlocks = (parts.length - 1) / blockLength;
		for (int blockNum = 0; blockNum < totalBlocks; blockNum++) {
			int offset = 1 + blockNum * blockLength;
			String direction = parts[offset + 2];
			if (!"forward".equalsIgnoreCase(direction) && !"reverse".equalsIgnoreCase(direction)) {
				throw new Exception("Last argument of the 'setPoscons' assignment block has to be 'forward' or 'reverse'");
			}
			String tierName = parts[offset];
			Integer startIndex = Integer.parseInt(parts[offset + 1]);
			boolean reverse = "reverse".equalsIgnoreCase(direction);
			
			Tier tier = findTier(tierName);
			tier.setPoscons(startIndex, reverse);
		}
	}
	
	/**
	 * Expects to see command
	 * setPosconToBay (assignments <bay name> <controller> <poscon id>)
	 * @throws Exception 
	 */
	private void processSetPosconToBayCommand(String parts[]) throws Exception {
		int blockLength = 3;
		if (parts.length < 4 || (parts.length - 1) % blockLength != 0 ){
			throwIncorrectNumberOfArgumentsException(TEMPLATE_SET_POSCON_TO_BAY);
		}
		int totalBlocks = (parts.length - 1) / blockLength;
		for (int blockNum = 0; blockNum < totalBlocks; blockNum++) {
			int offset = 1 + blockNum * blockLength;
			String bayName = parts[offset + 0], controllerName = parts[offset + 1], posconId = parts[offset + 2];
			//Find or create Controller
			LedController controller = uiUpdateBehavior.addControllerCallWithObjects(facility, controllerName, DeviceType.Poscons.toString());
			//Assign poscon to Bay
			Bay bay = findBay(bayName);
			bay.setPosconAssignment(controller.getPersistentId().toString(), posconId);
		}
	}
	
	/**
	 * Expects to see command
	 * setWall <aisle> <off/putwall/skuwall>
	 * @throws Exception 
	 */
	private void processSetWallCommand(String parts[]) throws Exception {
		if (parts.length != 3){
			throwIncorrectNumberOfArgumentsException(TEMPLATE_SET_WALL);
		} 
		Aisle aisle = findAisle(parts[1]);
		String wallType = parts[2];
		if (Location.PUTWALL_USAGE.equalsIgnoreCase(wallType)){
			aisle.setUsage(Location.PUTWALL_USAGE);
		} else if (Location.SKUWALL_USAGE.equalsIgnoreCase(wallType)){
			aisle.setUsage(Location.SKUWALL_USAGE);
		} else if ("off".equalsIgnoreCase(wallType)){
			aisle.setUsage(null);
		} else {
			throw new Exception("Unknown wall type '" + wallType + "' [off/putwall/skuwall]");
		}
	}

	/**
	 * Expects to see command
	 * createChe <che> <color> <mode> [name] [scannerType] [cheLighting]
	 * @throws Exception 
	 */
	private void processCreateCheCommand(String parts[]) throws Exception {
		if (parts.length < 4 || parts.length > 7){
			throwIncorrectNumberOfArgumentsException(TEMPLATE_CREATE_CHE);
		}
		String controllerId = parts[1], color = parts[2].toUpperCase(), mode = parts[3];
		String domainId = parts.length >= 5 ? parts[4] : controllerId;
		String scannerType = parts.length >= 6 ? parts[5] : "ORIGINALSERIAL";
		String cheLighting = parts.length == 7 ? parts[6] : "POSCON_V1";
		//Confirm that the provided enum values are valid
		ColorEnum.valueOf(color);
		ProcessMode.valueOf(mode);
		ScannerTypeEnum.valueOf(scannerType);
		CheLightingEnum.valueOf(cheLighting);
		
		//Create or update CHE
		Che che = Che.staticGetDao().findByDomainId(facility.getNetwork(CodeshelfNetwork.DEFAULT_NETWORK_NAME), domainId);
		if (che == null) {
			uiUpdateBehavior.addChe(facility.getPersistentId().toString(), domainId, null, color, controllerId, mode, scannerType, cheLighting);
		} else {
			uiUpdateBehavior.updateChe(che.getPersistentId().toString(), domainId, null, color, controllerId, mode, scannerType, cheLighting);
		}
	}

	/**
	 * Expects to see command
	 * deleteChes (<ches>)
	 * @throws Exception 
	 */
	private void processDeleteChesCommand(String parts[]) throws Exception {
		int len = parts.length;
		if (len < 2){
			throwIncorrectNumberOfArgumentsException(TEMPLATE_DELETE_CHES);
		}
		String cheGuidStr = null;
		NetGuid cheGuid = null;
		HashSet<String> cheGuidsToDelete = new HashSet<>();
		//Make a list of Che Guids that this script command wants to delete  
		for (int i = 1; i < len; i++){
			cheGuidStr = parts[i];
			cheGuid = new NetGuid(cheGuidStr);
			cheGuidsToDelete.add(cheGuid.getHexStringNoPrefix());
		}
		//Iterate over all Ches in this facility and find matches to the Guids in script command 
		Map<String, Object> filterArgs = ImmutableMap.<String, Object> of("facilityId", facility.getPersistentId());
		List<Che> allChes = Che.staticGetDao().findByFilter("cheByFacility", filterArgs);
		for (Che che : allChes) {
			if (facility.equals(che.getFacility())){
				if (cheGuidsToDelete.contains(che.getDeviceGuidStrNoPrefix())){
					uiUpdateBehavior.deleteChe(che.getPersistentId().toString());
				}
			}
		}
	}

	/**
	 * Expects to see command
	 * deleteAllPaths
	 * @throws Exception 
	 */
	private void processDeleteAllPathsCommand(String parts[]) throws Exception {
		if (parts.length != 1){
			throwIncorrectNumberOfArgumentsException(TEMPLATE_DELETE_ALL_PATHS);
		}
		paths.clear();
		List<Path> pathList = Path.staticGetDao().findByParent(facility);
		for (Path path : pathList) {
			path.deleteThisPath();
		}
	}

	/**
	 * Expects to see command
	 * defPath <path> (segments '-' <start x> <start y> <end x> <end y>)
	 * @throws Exception 
	 */
	private void processDefinePathCommand(String parts[]) throws Exception {
		if (parts.length < 7 || (parts.length - 2) % 5 != 0 ){
			throwIncorrectNumberOfArgumentsException(TEMPLATE_DEF_PATH);
		}
		String pathId = parts[1];
		if (paths.containsKey(pathId)) {
			throw new Exception("Path " + pathId + " has already been defined in this script");
		}
		Path path = facility.createPath("");
		int totalSegnemts = (parts.length - 2) / 5;
		for (int segNum = 0; segNum < totalSegnemts; segNum++) {
			int offset = 2 + segNum * 5;
			if (!parts[offset].equals("-")) {
				throw new Exception("Did not put an '-' before each path segment");
			}
			double startX = Double.parseDouble(parts[offset + 1]);
			double startY = Double.parseDouble(parts[offset + 2]);
			double endX =   Double.parseDouble(parts[offset + 3]);
			double endY =   Double.parseDouble(parts[offset + 4]);
			Point head = new Point(PositionTypeEnum.METERS_FROM_PARENT, startX, startY, 0.0);
			Point tail = new Point(PositionTypeEnum.METERS_FROM_PARENT, endX, endY, 0.0);
			path.createPathSegment(segNum, head, tail);
		}
		paths.put(pathId, path);
	}
	
	/**
	 * Expects to see command
	 * defPath <path> <path name (spaces allowed)>
	 * @throws Exception 
	 */
	private void processSetPathNameCommand(String parts[]) throws Exception {
		if (parts.length < 2){
			throwIncorrectNumberOfArgumentsException(TEMPLATE_SET_PATH_NAME);
		}
		String pathId = parts[1];
		Path path = paths.get(pathId);
		StringBuilder nameBuilder = new StringBuilder();
		for (int i = 2; i < parts.length; i++) {
			nameBuilder.append(parts[i]).append(" ");
		}
		String name = nameBuilder.toString();
		if (!name.isEmpty()) {
			name = name.substring(0, name.length() - 1);
		}
		path.setPathNameUi(name);
	}

	/**
	 * Expects to see command
	 * assignPathSgmToAisle <path> <segment id> <aisle name>
	 * @throws Exception 
	 */
	private void processAsignPathSegmentToAisleCommand(String parts[]) throws Exception {
		if (parts.length != 4){
			throwIncorrectNumberOfArgumentsException(TEMPLATE_ASSIGN_PATH_SGM_AISLE);
		}
		String pathId = parts[1], aisleName= parts[3];
		Path path = paths.get(pathId);
		if (path == null) {
			throw new Exception("Path " + pathId + " has not been defined in this script");
		}
		int segmentId = Integer.parseInt(parts[2]);
		PathSegment segment = path.getPathSegment(segmentId);
		if (segment == null) {
			throw new Exception("Path " + pathId + " does not have segment " + segmentId);
		}
		Aisle aisle = findAisle(aisleName);
		aisle.associatePathSegment(segment);
	}

	/**
	 * Expects to see command
	 * assignTapeToTier (assignments <tape id> <tier name>)
	 * @throws Exception 
	 */
	private void processAsignTapeToTierCommand(String parts[]) throws Exception {
		if (parts.length < 3 || (parts.length - 1) % 2 != 0 ){
			throwIncorrectNumberOfArgumentsException(TEMPLATE_ASSIGN_TAPE_TO_TIER);
		}
		int totalSegnemts = (parts.length - 1) / 2;
		for (int segNum = 0; segNum < totalSegnemts; segNum++) {
			int offset = 1 + segNum * 2;
			String tapeId = parts[offset], tierName= parts[offset + 1];
			Tier tier = findTier(tierName);
			tier.setTapeIdUi(tapeId);
		}
	}

	/**
	 * Expects to see command
	 * deleteAllExtensionPoints
	 * @throws Exception 
	 */
	private void processDeleteAllExtensionPointsCommand(String parts[]) throws Exception {
		if (parts.length != 1){
			throwIncorrectNumberOfArgumentsException(TEMPLATE_DELETE_ALL_EXTENSIONS);
		}
		ExtensionPointEngine engine = ExtensionPointEngine.getInstance(facility);

		List<ExtensionPoint> extensions = engine.getAllExtensions();
		for (ExtensionPoint extension : extensions) {
			engine.delete(extension);
		}
	}

	/**
	 * Expects to see command
	 * deleteExtensionPoint <type>
	 * @throws Exception 
	 */
	private void processDeleteExtensionPointCommand(String parts[]) throws Exception {
		if (parts.length != 2){
			throwIncorrectNumberOfArgumentsException(TEMPLATE_DELETE_EXTENSION);
		}
		String typeStr = parts[1];
		ExtensionPointType typeEnum = null;
		try {
			typeEnum = ExtensionPointType.valueOf(typeStr);
		} catch (IllegalArgumentException e) {
			throw new Exception("Illegal type '" + typeStr + "' for an extension point. [OrderImportBeanTransformation,OrderImportHeaderTransformation,OrderImportCreateHeader,OrderImportLineTransformationOrderOnCartContent,WorkInstructionExportContent,WorkInstructionExportCreateHeader,WorkInstructionExportCreateTrailer,WorkInstructionExportLineTransformation]");
		}
		ExtensionPointEngine engine = ExtensionPointEngine.getInstance(facility);
		Optional<ExtensionPoint> point = engine.getExtensionPoint(typeEnum);
		if (point.isPresent()) {
			engine.delete(point.get());
		}
	}

	/**
	 * Expects to see command
	 * addExtensionPoint <filename> <type> <active/inactive>
	 * @throws Exception 
	 */
	private void processAddExtensionPointCommand(String parts[]) throws Exception {
		if (parts.length != 4){
			throwIncorrectNumberOfArgumentsException(TEMPLATE_ADD_EXTENSION);
		}
		String filename = parts[1], typeStr = parts[2], state = parts[3];
		//Parse type and verify that no other extensions of this type exist
		ExtensionPointType typeEnum = null;
		try {
			typeEnum = ExtensionPointType.valueOf(typeStr);
		} catch (IllegalArgumentException e) {
			throw new Exception("Illegal type '" + typeStr + "' for an extension point.");
		}
		
		ExtensionPointEngine engine = ExtensionPointEngine.getInstance(facility);
		Optional<ExtensionPoint> extension = engine.getExtensionPoint(typeEnum);
		if (extension != null && extension.isPresent()) {
			throw new Exception(typeEnum + " extension already exists in this facility. Run 'deleteExtensionPoint " + typeEnum + "' to delete it.");
		}

		boolean active = true;
		if ("active".equalsIgnoreCase(state)) {
			active = true;
		} else if ("inactive".equalsIgnoreCase(state)) {
			active = false;
		} else {
			throw new Exception("Invalid exptension point state '" + state + "'. [active/inactive]");
		}
		InputStreamReader reader = readFile(filename);
		String script = IOUtils.toString(reader);
		ExtensionPoint point = new ExtensionPoint(facility, typeEnum);
		point.setScript(script);
		point.setActive(active);
		engine.create(point);
	}

	/**
	 * Expects to see command
	 * sftp <'orders'/'wi'> <host> <port> <username> <password> <directories: in and out for orders, out for wi>
	 * @throws Exception 
	 */
	private void processSftpCommand(String parts[]) throws Exception {
		if (parts.length != 7 && parts.length != 8){
			throwIncorrectNumberOfArgumentsException(TEMPLATE_SFTP);
		}
		String type = parts[1], host = parts[2], port = parts[3], username = parts[4], password = parts[5];
		if ("**********".equals(password)){
			throw new Exception("You used the SFTP password placeholder '**********' from the scripting documentation. If your server password actually is '**********', you are now stuck because I wanted to put this error message here");
		}
		boolean orders = false;
		if ("orders".equalsIgnoreCase(type)){
			orders = true;
		} else if ("wi".equalsIgnoreCase(type)){
			orders = false;
		} else {
			throw new Exception("Invalid type " + type + ". Expected 'orders' or 'wi'");
		}
		String serviceDomainId = orders?"SFTPORDERS":"SFTPWIS";
		EDIGatewaysResource res = new EDIGatewaysResource(new EdiExportService());
		res.setParent(facility);
		//MultivaluedMapImpl <String, String> params = new MultiValueMap();
		//HashMap<String, String> params = new HashMap<>();
		MultivaluedMapImpl params = new MultivaluedMapImpl();
		params.add("domainId", serviceDomainId);
		params.add("active", "true");
		params.add("host", host);
		params.add("port", port);
		params.add("username", username);
		params.add("password", password);
		if (orders) {
			if (parts.length != 8) {
				throwIncorrectNumberOfArgumentsException("sftp orders <host> <port> <username> <password> <in directory> <out directory>");
			}
			params.add("importPath", parts[6]);
			params.add("archivePath", parts[7]);
		} else {
			if (parts.length != 7) {
				throwIncorrectNumberOfArgumentsException("sftp wi <host> <port> <username> <password> <out directory>");
			}
			params.add("exportPath", parts[6]);
		}
		res.update(serviceDomainId, params);
	}

	/**
	 * Expects to see command
	 * waitSeconds <seconds>
	 * @throws Exception 
	 */
	private void processWaitSecondsCommand(String parts[]) throws Exception {
		if (parts.length != 2){
			throwIncorrectNumberOfArgumentsException(TEMPLATE_WAIT_SECONDS);
		}
		int seconds = Integer.parseInt(parts[1]);
		LOGGER.info("Pause server script");
		Thread.sleep(seconds * 1000); 
	}
	
	private InputStreamReader readFile(String filename) throws Exception{
		InputStream is = ScriptStepParser.getInputStream(postBody, filename);
		if (is == null) {
			throw new Exception("Unable to find file " + filename);
		}
		return new InputStreamReader(is);
	}
	
	private void throwIncorrectNumberOfArgumentsException(String expected) throws Exception{
		throw new Exception("Incorrect number of arguments. Expected '" + expected + "'");
	}
	
	private Bay findBay(String bayName) throws Exception{
		Location location = findLocation(bayName);
		if (!location.isBay()){
			throw new Exception(location.getClassName() + " " + bayName + " is not an Aisle");
		}
		Bay bay = Bay.staticGetDao().findByPersistentId(location.getPersistentId());
		return bay;
	}

	private Aisle findAisle(String aisleName) throws Exception{
		Location location = findLocation(aisleName);
		if (!location.isAisle()){
			throw new Exception(location.getClassName() + " " + aisleName + " is not an Aisle");
		}
		Aisle aisle = Aisle.staticGetDao().findByPersistentId(location.getPersistentId());
		return aisle;
	}
	
	private Tier findTier(String tierName) throws Exception{
		Location location = findLocation(tierName);
		if (!location.isTier()){
			throw new Exception(location.getClassName() + " " + tierName + " is not a Tier");
		}
		Tier tier = Tier.staticGetDao().findByPersistentId(location.getPersistentId());
		return tier;
	}
	
	private Location findLocation(String locationName) throws Exception{
		Location location = facility.findSubLocationById(locationName);
		if (location == null) {
			throw new Exception("Unable to find location " + locationName);
		}
		return location;
	}

}
