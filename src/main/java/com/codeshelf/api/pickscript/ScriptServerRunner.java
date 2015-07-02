package com.codeshelf.api.pickscript;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.device.ScriptSiteRunner;
import com.codeshelf.edi.ICsvAislesFileImporter;
import com.codeshelf.edi.ICsvInventoryImporter;
import com.codeshelf.edi.ICsvLocationAliasImporter;
import com.codeshelf.edi.ICsvOrderImporter;
import com.codeshelf.flyweight.command.ColorEnum;
import com.codeshelf.model.DeviceType;
import com.codeshelf.model.PositionTypeEnum;
import com.codeshelf.model.dao.PropertyDao;
import com.codeshelf.model.domain.Aisle;
import com.codeshelf.model.domain.Bay;
import com.codeshelf.model.domain.Che;
import com.codeshelf.model.domain.DomainObjectProperty;
import com.codeshelf.model.domain.Facility;
import com.codeshelf.model.domain.LedController;
import com.codeshelf.model.domain.Location;
import com.codeshelf.model.domain.OrderHeader;
import com.codeshelf.model.domain.Path;
import com.codeshelf.model.domain.PathSegment;
import com.codeshelf.model.domain.Point;
import com.codeshelf.model.domain.Tier;
import com.codeshelf.model.domain.Che.ProcessMode;
import com.codeshelf.model.domain.Vertex;
import com.codeshelf.persistence.TenantPersistenceService;
import com.codeshelf.security.CodeshelfSecurityManager;
import com.codeshelf.service.PropertyService;
import com.codeshelf.service.UiUpdateService;
import com.codeshelf.util.CsExceptionUtils;
import com.codeshelf.validation.BatchResult;
import com.codeshelf.ws.protocol.message.ScriptMessage;
import com.sun.jersey.multipart.FormDataMultiPart;

public class ScriptServerRunner {
	private final static String TEMPLATE_EDIT_FACILITY = "editFacility <facility domain id> <primary site controller id> <primary radio channel>";
	private final static String TEMPLATE_OUTLINE = "createDummyOutline [size 1/2/3]";
	private final static String TEMPLATE_SET_PROPERTY = "setProperty <name> <value>";
	private final static String TEMPLATE_DELETE_ORDERS = "deleteOrders <filename>";
	private final static String TEMPLATE_IMPORT_ORDERS = "importOrders <orders>";
	private final static String TEMPLATE_IMPORT_AISLES = "importAisles <filename>";
	private final static String TEMPLATE_IMPORT_LOCATIONS = "importLocations <filename>";
	private final static String TEMPLATE_IMPORT_INVENTORY = "importInventory <filename>";
	private final static String TEMPLATE_SET_CONTROLLER = "setController <location> <lights/poscons> <controller> <channel> ['tiersInAisle']";
	private final static String TEMPLATE_SET_POSCONS = "setPoscons (assignments <tier> <startIndex> <'forward'/'reverse'>)";
	private final static String TEMPLATE_SET_POSCON_TO_BAY = "setPosconToBay (assignments <bay name> <controller> <poscon id>)";
	private final static String TEMPLATE_SET_WALL = "setWall <aisle> <off/putwall/skuwall>";
	private final static String TEMPLATE_CREATE_CHE = "createChe <che> <color> <mode> [name]";
	private final static String TEMPLATE_DELETE_ALL_PATHS = "deleteAllPaths";
	private final static String TEMPLATE_DEF_PATH = "defPath <pathName> (segments '-' <start x> <start y> <end x> <end y>)";
	private final static String TEMPLATE_ASSIGN_PATH_SGM_AISLE = "assignPathSgmToAisle <pathName> <segment id> <aisle name>";
	private final static String TEMPLATE_ASSIGN_TAPE_TO_TIER = "assignTapeToTier <tape id> <tier name>";
	private final static String TEMPLATE_WAIT_SECONDS = "waitSeconds <seconds>";
	
	private static final Logger LOGGER = LoggerFactory.getLogger(ScriptSiteRunner.class);
	private final TenantPersistenceService persistence;
	private final UUID facilityId;
	private final UiUpdateService uiUpdateService;
	private final PropertyService propertyService;
	private final ICsvOrderImporter orderImporter;
	private final ICsvAislesFileImporter aisleImporter;
	private final ICsvLocationAliasImporter locationsImporter;
	private final ICsvInventoryImporter inventoryImporter;
	private final FormDataMultiPart postBody;
	private StringBuilder report;
	private Facility facility;
	private HashMap<String, Path> paths = new HashMap<String, Path>();
	
	public ScriptServerRunner(TenantPersistenceService persistence,
		UUID facilityId,
		FormDataMultiPart postBody,
		UiUpdateService uiUpdateService,
		PropertyService propertyService,
		ICsvAislesFileImporter aisleImporter,
		ICsvLocationAliasImporter locationsImporter,
		ICsvInventoryImporter inventoryImporter,
		ICsvOrderImporter orderImporter) {
		this.persistence = persistence;
		this.facilityId = facilityId;
		this.postBody = postBody;
		this.uiUpdateService = uiUpdateService;
		this.propertyService = propertyService;
		this.aisleImporter = aisleImporter;
		this.locationsImporter = locationsImporter;
		this.inventoryImporter = inventoryImporter;
		this.orderImporter = orderImporter;
	}
	
	public ScriptMessage processServerScript(List<String> lines){
		report = new StringBuilder();
		report.append("SERVER\n");
		ScriptMessage message = new ScriptMessage();
		try {
			for (String line : lines) {
				persistence.beginTransaction();
				facility = Facility.staticGetDao().findByPersistentId(facilityId);
				processLine(line);
				persistence.commitTransaction();
			}
			report.append("***Server Script Segment Completed Successfully***\n");
		} catch (Exception e) {
			persistence.rollbackTransaction();
			report.append(CsExceptionUtils.exceptionToString(e)).append("\n");
			message.setMessageError(CsExceptionUtils.exceptionToString(e) + "\n");
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
		//Clean up multiple spaces between words 
		line = line.trim().replaceAll("\\s+", " ");
		String parts[] = line.split(" ");
		for (int i = 0; i < parts.length; i++) {
			parts[i] = parts[i].trim();
		}
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
			processImportOrdersCommand(parts);
		} else if (command.equalsIgnoreCase("importAisles")) {
			processImportAislesCommand(parts);
		} else if (command.equalsIgnoreCase("importLocations")) {
			processImportLocationsCommand(parts);
		} else if (command.equalsIgnoreCase("importInventory")) {
			processImportInventory(parts);
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
		} else if (command.equalsIgnoreCase("deleteAllPaths")) {
			processDeleteAllPathsCommand(parts);
		} else if (command.equalsIgnoreCase("defPath")) {
			processDefinePathCommand(parts);
		} else if (command.equalsIgnoreCase("assignPathSgmToAisle")) {
			processAsignPathSegmentToAisleCommand(parts);
		} else if (command.equalsIgnoreCase("assignTapeToTier")) {
			processAsignTapeToTierCommand(parts);
		} else if (command.equalsIgnoreCase("waitSeconds")) {
			processWaitSecondsCommand(parts);
		} else if (command.startsWith("//")) {
		} else if  (command.equalsIgnoreCase("togglePutWall")) {
			throw new Exception("Command togglePutWall has been deprecated due to an addition of Sku Walls. Instead, use " + TEMPLATE_SET_WALL);
		} else {
			throw new Exception("Invalid command '" + command + "'. Expected [editFacility, createDummyOutline, setProperty, deleteOrders, importOrders, importAisles, importLocations, importInventory, setController, setPoscons, setPosconToBay, togglePutWall, createChe, deleteAllPaths, defPath, assignPathSgmToAisle, assignTapeToTier, waitSeconds, //]");
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
		facility.setDomainId(parts[1]);
		facility.setPrimarySiteControllerId(parts[2]);
		facility.setPrimaryChannel(Short.parseShort(parts[3]));
	}

	/**
	 * Expects to see command
	 * createDummyOutline [size 1/2/3]
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
		if (facilitySize < 1 || facilitySize > 3) {
			throw new Exception("Invalid facility size " + facilitySize + ". Allowed values: 1-3");
		}
		List<Vertex> vertices = facility.getVertices();
		while (!vertices.isEmpty()){
			Vertex.staticGetDao().delete(vertices.remove(0));
		}
		if (facilitySize == 1) {
			facility.createVertex("V0", "GPS", -122.271673679351807, 37.8032855078260113, 0);
			facility.createVertex("V1", "GPS", -122.271673679351807, 37.8031498746375263, 1);
			facility.createVertex("V2", "GPS", -122.271479676417243, 37.8031498746375263, 2);
			facility.createVertex("V3", "GPS", -122.271479676417243, 37.8032855078260113, 3);
		} else if (facilitySize == 2) {
			facility.createVertex("V0", "GPS", -122.271673679351807, 37.8032855078260113, 0);
			facility.createVertex("V1", "GPS", -122.271673679351807, 37.8030545074026705, 1);
			facility.createVertex("V2", "GPS", -122.271384457997215, 37.8030545074026705, 2);
			facility.createVertex("V3", "GPS", -122.271384457997215, 37.8032855078260113, 3);
		} else if (facilitySize == 3) {
			facility.createVertex("V0", "GPS", -122.271673679351807, 37.8032855078260113, 0);
			facility.createVertex("V1", "GPS", -122.271673679351807, 37.8029527822164439, 1);
			facility.createVertex("V2", "GPS", -122.271210114411247, 37.8029527822164439, 2);
			facility.createVertex("V3", "GPS", -122.271210114411247, 37.8032855078260113, 3);
		}
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
		String name = parts[1].toUpperCase();
		DomainObjectProperty property = PropertyDao.getInstance().getPropertyWithDefault(facility, name);
		if (property == null) {
			throw new Exception("Property " + name + " doesn't exist");
		}
		propertyService.changePropertyValue(facility, name, parts[2]);
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
	 * @throws Exception 
	 */
	private void processImportOrdersCommand(String parts[]) throws Exception {
		if (parts.length != 2){
			throwIncorrectNumberOfArgumentsException(TEMPLATE_IMPORT_ORDERS);
		}
		String filename = parts[1];
		InputStreamReader reader = readFile(filename);
		long receivedTime = System.currentTimeMillis();
		BatchResult<Object> results = orderImporter.importOrdersFromCsvStream(reader, facility, new Timestamp(System.currentTimeMillis()));
		String username = CodeshelfSecurityManager.getCurrentUserContext().getUsername();
		orderImporter.persistDataReceipt(facility, username, filename + ".csv", receivedTime, results);
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
		aisleImporter.importAislesFileFromCsvStream(reader, facility, new Timestamp(System.currentTimeMillis()));
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
		locationsImporter.importLocationAliasesFromCsvStream(reader, facility, new Timestamp(System.currentTimeMillis()));
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
		inventoryImporter.importSlottedInventoryFromCsvStream(reader, facility, new Timestamp(System.currentTimeMillis()));
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
			throw new Exception("Invalid controller type " + parts[2] + " (lights/poscons");
		}
		if (parts.length == 6 && !parts[5].equalsIgnoreCase("tiersInAisle")){
			throw new Exception("The optional 4th parameter in the 'setLedController' command has to be 'tiersInAisle'");
		}
		String controllerName = parts[3];
		String controllerChannel = parts[4];

		//Find or create Controller
		LedController controller = uiUpdateService.addControllerCallWithObjects(facility, controllerName, type);
		
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
			LedController controller = uiUpdateService.addControllerCallWithObjects(facility, controllerName, DeviceType.Poscons.toString());
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
	 * createChe <che> <color> <mode> [name]
	 * @throws Exception 
	 */
	private void processCreateCheCommand(String parts[]) throws Exception {
		if (parts.length < 4 || parts.length > 5){
			throwIncorrectNumberOfArgumentsException(TEMPLATE_CREATE_CHE);
		}
		String controllerId = parts[1], color = parts[2].toUpperCase(), mode = parts[3];
		String domainId = parts.length == 5 ? parts[4] : controllerId;
		//Confirm that the provided enum values are valid
		ColorEnum.valueOf(color);
		ProcessMode.valueOf(mode);
		
		//Create or update CHE
		Che che = Che.staticGetDao().findByDomainId(facility.getNetworks().get(0), domainId);
		if (che == null) {
			uiUpdateService.addChe(facility.getPersistentId().toString(), domainId, null, color, controllerId, mode);
		} else {
			uiUpdateService.updateChe(che.getPersistentId().toString(), domainId, null, color, controllerId, mode);
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
	 * defPath <pathName> (segments '-' <start x> <start y> <end x> <end y>)
	 * @throws Exception 
	 */
	private void processDefinePathCommand(String parts[]) throws Exception {
		if (parts.length < 7 || (parts.length - 2) % 5 != 0 ){
			throwIncorrectNumberOfArgumentsException(TEMPLATE_DEF_PATH);
		}
		String pathName = parts[1];
		if (paths.containsKey(pathName)) {
			throw new Exception("Path " + pathName + " has already been defined in this script");
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
		paths.put(pathName, path);
	}

	/**
	 * Expects to see command
	 * assignPathSgmToAisle <pathName> <segment id> <aisle name>
	 * @throws Exception 
	 */
	private void processAsignPathSegmentToAisleCommand(String parts[]) throws Exception {
		if (parts.length != 4){
			throwIncorrectNumberOfArgumentsException(TEMPLATE_ASSIGN_PATH_SGM_AISLE);
		}
		String pathName = parts[1], aisleName= parts[3];
		Path path = paths.get(pathName);
		if (path == null) {
			throw new Exception("Path " + pathName + " has not been defined in this script");
		}
		int segmentId = Integer.parseInt(parts[2]);
		PathSegment segment = path.getPathSegment(segmentId);
		if (segment == null) {
			throw new Exception("Path " + pathName + " does not have segment " + segmentId);
		}
		Aisle aisle = findAisle(aisleName);
		aisle.associatePathSegment(segment);
	}

	/**
	 * Expects to see command
	 * assignTapeToTier <tape id> <tier name>
	 * @throws Exception 
	 */
	private void processAsignTapeToTierCommand(String parts[]) throws Exception {
		if (parts.length != 3){
			throwIncorrectNumberOfArgumentsException(TEMPLATE_ASSIGN_TAPE_TO_TIER);
		}
		String tapeId = parts[1], tierName= parts[2];
		Tier tier = findTier(tierName);
		tier.setTapeIdUi(tapeId);
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
		System.out.println("Pause server script");
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
