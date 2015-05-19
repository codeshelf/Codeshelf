package com.codeshelf.api.pickscript;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Restrictions;
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
import com.codeshelf.model.domain.Aisle;
import com.codeshelf.model.domain.Che;
import com.codeshelf.model.domain.Facility;
import com.codeshelf.model.domain.LedController;
import com.codeshelf.model.domain.Location;
import com.codeshelf.model.domain.Path;
import com.codeshelf.model.domain.PathSegment;
import com.codeshelf.model.domain.Point;
import com.codeshelf.model.domain.Tier;
import com.codeshelf.model.domain.Che.ProcessMode;
import com.codeshelf.persistence.TenantPersistenceService;
import com.codeshelf.service.UiUpdateService;
import com.codeshelf.util.CsExceptionUtils;
import com.codeshelf.ws.protocol.message.PickScriptMessage;
import com.sun.jersey.multipart.FormDataMultiPart;

public class ScriptServerRunner {
	private final static String TEMPLATE_EDIT_FACILITY = "editFacility <facility domain id> <primary site controller id> <primary radio channel>";
	private final static String TEMPLATE_IMPORT_ORDERS = "importOrders <filename>";
	private final static String TEMPLATE_IMPORT_AISLES = "importAisles <filename>";
	private final static String TEMPLATE_IMPORT_LOCATIONS = "importLocations <filename>";
	private final static String TEMPLATE_IMPORT_INVENTORY = "importInventory <filename>";
	private final static String TEMPLATE_SET_CONTROLLER = "setController <location> <lights/poscons> <controller> <channel> ['allTiersInAisle']";
	private final static String TEMPLATE_TOGGLE_PUT_WALL = "togglePutWall <aisle> [boolean putwall]";
	private final static String TEMPLATE_CREATE_CHE = "createChe <che> <color> <mode>";
	private final static String TEMPLATE_DELETE_ALL_PATHS = "deleteAllPaths";
	private final static String TEMPLATE_DEF_PATH = "defPath <pathName> (segments 'X' <start x> <start y> <end x> <end y>)";
	private final static String TEMPLATE_ASSIGN_PATH_SGM_AISLE = "assignPathSgmToAisle <pathName> <segment id> <aisle name>";
	private final static String TEMPLATE_WAIT_SECONDS = "waitSeconds <seconds>";
	
	private static final Logger LOGGER = LoggerFactory.getLogger(ScriptSiteRunner.class);
	private final TenantPersistenceService persistence;
	private final UUID facilityId;
	private final UiUpdateService uiUpdateService;
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
		ICsvAislesFileImporter aisleImporter,
		ICsvLocationAliasImporter locationsImporter,
		ICsvInventoryImporter inventoryImporter,
		ICsvOrderImporter orderImporter) {
		this.persistence = persistence;
		this.facilityId = facilityId;
		this.postBody = postBody;
		this.uiUpdateService = uiUpdateService;
		this.aisleImporter = aisleImporter;
		this.locationsImporter = locationsImporter;
		this.inventoryImporter = inventoryImporter;
		this.orderImporter = orderImporter;
	}
	
	public PickScriptMessage processServerScript(String script){
		report = new StringBuilder();
		report.append("SERVER\n");
		PickScriptMessage message = new PickScriptMessage();
		try {
			String[] lines = script.split("\n");
			for (String line : lines) {
				persistence.beginTransaction();
				facility = Facility.staticGetDao().findByPersistentId(facilityId);
				processLine(line);
				persistence.commitTransaction();
			}
		} catch (Exception e) {
			persistence.rollbackTransaction();
			report.append(CsExceptionUtils.exceptionToString(e)).append("\n");
			message.setSuccess(false);
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
		} else if (command.equalsIgnoreCase("importOrders")) {
			processImportOrdersCommand(parts);
		} else if (command.equalsIgnoreCase("importAisles")) {
			processImportAislesCommand(parts);
		} else if (command.equalsIgnoreCase("importLocations")) {
			processImportLocationsCommand(parts);
		} else if (command.equalsIgnoreCase("importInventory")) {
			processImportInventory(parts);
		} else if (command.equalsIgnoreCase("setController")) {
			processSetAisleControllerCommand(parts);
		} else if (command.equalsIgnoreCase("togglePutWall")) {
			processTogglePutWallCommand(parts);
		} else if (command.equalsIgnoreCase("createChe")) {
			processCreateCheCommand(parts);
		} else if (command.equalsIgnoreCase("deleteAllPaths")) {
			processDeleteAllPathsCommand(parts);
		} else if (command.equalsIgnoreCase("defPath")) {
			processDefinePathCommand(parts);
		} else if (command.equalsIgnoreCase("assignPathSgmToAisle")) {
			processAsignPathSegmentToAisleCommand(parts);
		} else if (command.equalsIgnoreCase("waitSeconds")) {
			processWaitSecondsCommand(parts);
		} else if (command.startsWith("//")) {
		} else {
			throw new Exception("Invalid command '" + command + "'. Expected [editFacility, importOrders, importAisles, importInventory, setController, togglePutWall, createChe, deleteAllPaths, defPath, assignPathSgmToAisle, waitSeconds, //]");
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
	 * importOrders <filename>
	 * @throws Exception 
	 */
	private void processImportOrdersCommand(String parts[]) throws Exception {
		if (parts.length != 2){
			throwIncorrectNumberOfArgumentsException(TEMPLATE_IMPORT_ORDERS);
		}
		String filename = parts[1];
		InputStreamReader reader = readFile(filename);
		orderImporter.importOrdersFromCsvStream(reader, facility, new Timestamp(System.currentTimeMillis()));
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
	 * setLedController <location> <type lights/poscons> <controller> <channel> ['allTiersInAisle']
	 * @throws Exception 
	 */
	private void processSetAisleControllerCommand(String parts[]) throws Exception {
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
		if (parts.length == 6 && !parts[5].equalsIgnoreCase("allTiersInAisle")){
			throw new Exception("The optional 4th parameter in the 'setLedController' command has to be 'allTiersInAisle'");
		}
		String controllerName = parts[3];
		String controllerChannel = parts[4];

		//Find or create Controller
		LedController controller = uiUpdateService.addControllerCallWithObjects(facility, controllerName, type);
		
		//Find Location
		String locationName = parts[1];
		List<Criterion> filterParams = new ArrayList<Criterion>();
		filterParams.add(Restrictions.eq("facility", facility));
		List<Location> locations = Location.staticGetLocationDao().getAll();
		Location foundLocation = null;
		for (Location location : locations) {
			if (location.getFacility().equals(facility) && location.getNominalLocationId().equalsIgnoreCase(locationName)){
				foundLocation = location;
				break;				
			}
		}
		if (foundLocation == null) {
			throw new Exception("Could not find location " + locationName);
		}
		
		//Assign controller to a given Aisle, given Tier, or to all Tiers in an Aisle of a given Tier
		if (foundLocation.isAisle()){
			Aisle aisle = Aisle.staticGetDao().findByPersistentId(foundLocation.getPersistentId());
			aisle.setControllerChannel(controller.getPersistentId().toString(), controllerChannel);
		} else if(foundLocation.isTier()){
			String tiersInAisle = parts.length==6 ? Tier.ALL_TIERS_IN_AISLE : Tier.THIS_TIER_ONLY;
			Tier tier = Tier.staticGetDao().findByPersistentId(foundLocation.getPersistentId());
			tier.setControllerChannel(controller.getPersistentId().toString(), controllerChannel, tiersInAisle);
		} else {
			throw new Exception(foundLocation.getClassName() + " " + foundLocation + " is not an Aisle or a Tier");
		}
	}
	
	/**
	 * Expects to see command
	 * togglePutWall <aisle> [boolean putwall]
	 * @throws Exception 
	 */
	private void processTogglePutWallCommand(String parts[]) throws Exception {
		if (parts.length < 2 || parts.length > 3){
			throwIncorrectNumberOfArgumentsException(TEMPLATE_TOGGLE_PUT_WALL);
		} 
		Boolean putWall = null;
		if (parts.length == 3){
			String flag = parts[2];
			if (!"true".equalsIgnoreCase(flag) && !"false".equalsIgnoreCase(flag)) {
				throw new Exception("The second and optional parameter of 'togglePutWall' must be 'true' or 'false'");
			}
			putWall = "true".equalsIgnoreCase(flag);
		}
		Aisle aisle = Aisle.staticGetDao().findByDomainId(facility, parts[1]);
		if (aisle == null) {
			throw new Exception("Unable to find aisle " + parts[1]);
		}
		if (putWall == null) {
			aisle.togglePutWallLocation();
		} else {
			aisle.setAsPutWallLocation(putWall);
		}
	}

	/**
	 * Expects to see command
	 * createChe <che> <color> <mode>
	 * @throws Exception 
	 */
	private void processCreateCheCommand(String parts[]) throws Exception {
		if (parts.length != 4){
			throwIncorrectNumberOfArgumentsException(TEMPLATE_CREATE_CHE);
		}
		String name = parts[1], color = parts[2].toUpperCase(), mode = parts[3];
		//Confirm that the provided enum values are valid
		ColorEnum.valueOf(color);
		ProcessMode.valueOf(mode);
		
		//Create or update CHE
		Che che = Che.staticGetDao().findByDomainId(facility.getNetworks().get(0), name);
		if (che == null) {
			uiUpdateService.addChe(facility.getPersistentId().toString(), name, null, color, name, mode);
		} else {
			uiUpdateService.updateChe(che.getPersistentId().toString(), name, null, color, name, mode);
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
	 * defPath <pathName> (segments 'X' <start x> <start y> <end x> <end y>)
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
			if (!parts[offset].equalsIgnoreCase("X")) {
				throw new Exception("Did not put an 'X' before each path segment");
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
		Aisle aisle = Aisle.staticGetDao().findByDomainId(facility, aisleName);
		if (aisle == null) {
			throw new Exception("Unable to find aisle " + aisleName);
		}
		aisle.associatePathSegment(segment);
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
		InputStream is = PickScriptParser.getInputStream(postBody, filename);
		if (is == null) {
			StringBuilder availableFiles = new StringBuilder("Available Files: ");
			Set<String> fields = postBody.getFields().keySet();
			for (String field : fields) {
				availableFiles.append(field).append(", ");
			}
			throw new Exception("Unable to find file " + filename + ". " + availableFiles);
		}
		return new InputStreamReader(is);
	}
	
	private void throwIncorrectNumberOfArgumentsException(String expected) throws Exception{
		throw new Exception("Incorrect number of arguments. Expected '" + expected + "'");
	}
}
