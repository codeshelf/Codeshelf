package com.codeshelf.api.pickscript;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Restrictions;

import com.codeshelf.edi.ICsvAislesFileImporter;
import com.codeshelf.edi.ICsvOrderImporter;
import com.codeshelf.model.domain.Aisle;
import com.codeshelf.model.domain.Facility;
import com.codeshelf.model.domain.LedController;
import com.codeshelf.model.domain.Location;
import com.codeshelf.model.domain.Tier;
import com.codeshelf.service.UiUpdateService;
import com.codeshelf.util.CsExceptionUtils;
import com.codeshelf.ws.protocol.message.PickScriptMessage;
import com.sun.jersey.multipart.FormDataMultiPart;

public class PickScriptServerRunner {
	private final static String TEMPLATE_IMPORT_ORDERS = "importOrders <filename>";
	private final static String TEMPLATE_IMPORT_AISLES = "importAisles <filename>";
	private final static String TEMPLATE_SET_LED_CONTROLLER = "setLedController <location> <controller> <channel> ['allTiersInAisle']";
	
	private final Facility facility;
	private final UiUpdateService uiUpdateService;
	private final ICsvOrderImporter orderImporter;
	private final ICsvAislesFileImporter aisleImporter;
	private StringBuilder report;
	private final FormDataMultiPart postBody;
	
	public PickScriptServerRunner(Facility facility, 
		FormDataMultiPart postBody,
		UiUpdateService uiUpdateService,
		ICsvAislesFileImporter aisleImporter,
		ICsvOrderImporter orderImporter) {
		this.facility = facility;
		this.postBody = postBody;
		this.uiUpdateService = uiUpdateService;
		this.aisleImporter = aisleImporter;
		this.orderImporter = orderImporter;
	}
	
	public PickScriptMessage processServerScript(String script){
		report = new StringBuilder();
		report.append("SERVER\n");
		PickScriptMessage message = new PickScriptMessage();
		try {
			String[] lines = script.split("\n");
			for (String line : lines) {
				processLine(line);
			}
		} catch (Exception e) {
			report.append(CsExceptionUtils.exceptionToString(e)).append("\n");
			message.setSuccess(false);
		}
		message.setResponse(report.toString());
		return message;
	}
	
	private void processLine(String line) throws Exception {
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
		if (command.equalsIgnoreCase("importOrders")) {
			processImportOrdersCommand(parts);
		} else if (command.equalsIgnoreCase("importAisles")) {
			processImportAislesCommand(parts);
		} else if (command.equalsIgnoreCase("setLedController")) {
			processSetAisleControllerCommand(parts);
		} else if (command.startsWith("//")) {
		} else {
			throw new Exception("Invalid command '" + command + "'. Expected [importOrders, importAisles, setLedController, //]");
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
	 * setLedController <location> <controller> <channel> ['allTiersInAisle']
	 * @throws Exception 
	 */
	private void processSetAisleControllerCommand(String parts[]) throws Exception {
		if (parts.length != 4 && parts.length != 5){
			throwIncorrectNumberOfArgumentsException(TEMPLATE_SET_LED_CONTROLLER);
		}
		if (parts.length == 5 && !parts[4].equalsIgnoreCase("allTiersInAisle")){
			throw new Exception("The optional 4th parameter in the 'setLedController' command has to be 'allTiersInAisle'");
		}
		String controllerName = parts[2];
		String controllerChannel = parts[3];

		//Find or create LED Controller
		LedController controller = uiUpdateService.addControllerCallWithObjects(facility, controllerName, "Lights");
		
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
		if (foundLocation instanceof Aisle){
			((Aisle) foundLocation).setControllerChannel(controller.getPersistentId().toString(), controllerChannel);
		} else if(foundLocation instanceof Tier){
			String tiersInAisle = parts.length==5 ? Tier.ALL_TIERS_IN_AISLE : Tier.THIS_TIER_ONLY;
			((Tier) foundLocation).setControllerChannel(controller.getPersistentId().toString(), controllerChannel, tiersInAisle);
		} else {
			throw new Exception(foundLocation.getClassName() + " " + foundLocation + " is not an Aisle or a Tier");
		}
		locations = null;
	}


	private InputStreamReader readFile(String filename) throws Exception{
		InputStream is = PickScriptParser.getInputStream(postBody, filename);
		if (is == null) {
			throw new Exception("Unable to find file " + filename);
		}
		return new InputStreamReader(is);
	}
	
	private void throwIncorrectNumberOfArgumentsException(String expected) throws Exception{
		throw new Exception("Incorrect number of arguments. Expected '" + expected + "'");
	}
}
