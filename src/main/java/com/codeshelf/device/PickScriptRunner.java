package com.codeshelf.device;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.model.domain.WorkInstruction;
import com.codeshelf.sim.worker.PickSimulator;
import com.codeshelf.ws.protocol.message.PickScriptMessage;
import com.google.common.collect.Lists;

public class PickScriptRunner {
	private final static String TEMPLATE_DEF_PICKER = "defPicker <pickerName> <cheGuid>";
	private final static String TEMPLATE_SETUP_CART = "setupCart <pickerName> <containers>";
	private final static String TEMPLATE_WAIT = "waitForState <pickerName> <states>";
	private final static String TEMPLATE_SET_PARAMS = "setParams <pickPause> <chanceSkipUpc> <chanceShort>";
	private final static String TEMPLATE_PICK = "pick <pickerNames>";
	private final static String TEMPLATE_PICKER_EXEC = "pickerExec <pickerName> <pickerCommand> [arguments]";
	
	private static final Logger LOGGER = LoggerFactory.getLogger(PickScriptRunner.class);
	private static final int WAIT_TIMEOUT = 4000;
	private int pickPause = 0;
	private double chanceSkipUpc = 0, chanceShort = 0;
	private HashMap<String, PickSimulator> pickers = new HashMap<>();
	private StringBuilder report = new StringBuilder();
	private final CsDeviceManager deviceManager;
	private static final Object lock = new Object();
	
	public PickScriptRunner(CsDeviceManager deviceManager) {
		this.deviceManager = deviceManager;
	}
	
	/**
	 * This method need to run asynchronously to not hold up the message queue between Server and Site
	 */
	public void runScript(final PickScriptMessage message) {
		Runnable runnable = new Runnable() {
			@Override
			public void run() {
				String script = message.getScript();
				if (script == null) {
					message.setResponseMessage("Empty script");
					deviceManager.clientEndpoint.sendMessage(message);
					return;
				}
				try {
					String[] lines = script.split("\n");
					for (String line : lines) {
						processLine(line);
					}
				} catch (Exception e) {
					String error = exceptionToString(e);
					report.append(error);
					report.append("Logging out from all pickers due to this error\n");
					logoutAll();
				}
				report.append("***Script Completed***\n");
				message.setResponseMessage(report.toString());
				deviceManager.clientEndpoint.sendMessage(message);
			}
		};
		new Thread(runnable).start();
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
		if (command.equalsIgnoreCase("pickerExec")) {
			processPickerExecCommand(parts);
		} else if (command.equalsIgnoreCase("defPicker")) {
			processDefinePickerCommand(parts);
		} else if (command.equalsIgnoreCase("waitForState")) {
			processWaitForStatesCommand(parts);
		} else if (command.equalsIgnoreCase("setupCart")) {
			processSetupCartCommand(parts);
		} else if (command.equalsIgnoreCase("setParams")) {
			processSetParamsCommand(parts);
		} else if (command.equalsIgnoreCase("pick")) {
			processPickCommand(parts);
		} else if (command.equalsIgnoreCase("pickAll")) {
			processPickAllCommand();
		} else if (command.startsWith("//")) {
		} else {
			throw new Exception("Invalid command '" + command + "'. Expected [pickerExec, defPicker, waitForState, setupCard, setParams, pick, pickAll, //]");
		}
	}
	
	/**
	 * Expects to see command
	 * pickerExec <pickerName> <pickerCommand> [arguments]
	 * @throws Exception 
	 */
	private void processPickerExecCommand(String parts[]) throws Exception {
		if (parts.length < 3) {
			throwIncorrectNumberOfArgumentsException(TEMPLATE_PICKER_EXEC);
		}
		String pickerName = parts[1];
		PickSimulator picker = getPicker(pickerName);
		String pickerCommand = parts[2];
		if (parts.length == 3){
			Method method = PickSimulator.class.getDeclaredMethod(pickerCommand);
			method.invoke(picker);
		} else if (parts.length == 4){
			Method method = PickSimulator.class.getDeclaredMethod(pickerCommand, String.class);
			method.invoke(picker, parts[3]);
		} else if (parts.length == 5){
			Method method = PickSimulator.class.getDeclaredMethod(pickerCommand, String.class, String.class);
			method.invoke(picker, parts[3], parts[4]);
		} else {
			throw new Exception("Ability to call PickSimulator methods wih more than 2 arguments not yet implemented.");
		}
	}
	
	/**
	 * Expects to see command
	 * defPicker <pickerName> <cheGuid>
	 * @throws Exception
	 */
	private void processDefinePickerCommand(String parts[]) throws Exception {
		if (parts.length != 3){
			throwIncorrectNumberOfArgumentsException(TEMPLATE_DEF_PICKER);
		}
		String pickerName = parts[1];
		String cheGuid = parts[2];
		PickSimulator picker = new PickSimulator(deviceManager, cheGuid);
		pickers.put(pickerName, picker);
	}
	
	/**
	 * Expects to see command
	 * waitForState <pickerName> <states>
	 * @throws Exception
	 */
	private void processWaitForStatesCommand(String parts[]) throws Exception {
		if (parts.length < 3){
			throwIncorrectNumberOfArgumentsException(TEMPLATE_WAIT);
		}
		PickSimulator picker = getPicker(parts[1]);
		ArrayList<CheStateEnum> states = Lists.newArrayList();
		for (int i = 2; i < parts.length; i++) {
			CheStateEnum state = CheStateEnum.valueOf(parts[i]);
			states.add(state);
		}
		picker.waitForOneOfCheStates(states, WAIT_TIMEOUT);
	}
	
	/**
	 * Expects to see command
	 * setParams <pickPause> <chanceSkipUpc> <chanceShort>
	 * @throws Exception
	 */
	private void processSetParamsCommand(String parts[]) throws Exception {
		if (parts.length != 4){
			throwIncorrectNumberOfArgumentsException(TEMPLATE_SET_PARAMS);
		}
		pickPause = Integer.parseInt(parts[1]);
		chanceSkipUpc = Double.parseDouble(parts[2]);
		chanceShort = Double.parseDouble(parts[3]);
	}

	/**
	 * Expects to see command
	 * setupCart <pickerName> <containers>
	 * @throws Exception
	 */
	private void processSetupCartCommand(String parts[]) throws Exception {
		if (parts.length < 3){
			throwIncorrectNumberOfArgumentsException(TEMPLATE_SETUP_CART);
		}
		String pickerName = parts[1];
		PickSimulator picker = getPicker(pickerName);
		picker.waitForCheState(CheStateEnum.CONTAINER_SELECT, WAIT_TIMEOUT);
		String container;
		for (int i = 2; i < parts.length; i++){
			container = parts[i];
			if (container != null && !container.isEmpty()){
				picker.setupContainer(parts[i], (i-1)+"");
			}
		}
	}
	
	/**
	 * If needed, this command can be made part of the available functionality
	 */
	private void logoutAll(){
		for (PickSimulator picker : pickers.values()){
			picker.logout();
		}
	}
	
	/**
	 * Expects to see command
	 * pickAll
	 * @throws Exception
	 */
	private void processPickAllCommand() throws Exception{
		LOGGER.info("Start che picks");
		ExecutorService executor = Executors.newFixedThreadPool(pickers.size());
		for (final PickSimulator picker : pickers.values()) {
			if (picker != null) {
				Runnable runnable = new Runnable() {
					@Override
					public void run() {
						try {
							pick(picker);
						} catch (Exception e) {
							report.append(exceptionToString(e));
						}
					}
				};
				executor.execute(runnable);
			}
		}
		executor.shutdown();
		executor.awaitTermination(5, TimeUnit.MINUTES);
		LOGGER.info("che picks done");
	}
	
	/**
	 * Expects to see command
	 * pick <pickerName>
	 * @throws Exception
	 */
	private void processPickCommand(String parts[]) throws Exception{
		if (parts.length != 2){
			throwIncorrectNumberOfArgumentsException(TEMPLATE_PICK);
		}
		PickSimulator picker = getPicker(parts[1]);
		pick(picker);
	}
	
	private void pick(PickSimulator picker) throws Exception{
		//At this point, CHE should already have containers set up. Start START to advance to Pick or Review stage
		picker.scanCommand(CheDeviceLogic.STARTWORK_COMMAND);
		ArrayList<CheStateEnum> states = Lists.newArrayList();
		states.add(CheStateEnum.SETUP_SUMMARY);
		states.add(CheStateEnum.LOCATION_SELECT);
		states.add(CheStateEnum.NO_WORK);
		states.add(CheStateEnum.DO_PICK);
		states.add(CheStateEnum.SCAN_SOMETHING);
		picker.waitForOneOfCheStates(states, WAIT_TIMEOUT);
		CheStateEnum state = picker.getCurrentCheState();
		
		//If CHE is in a Review stage, scan START again to advance to Pick stage
		if (state == CheStateEnum.SETUP_SUMMARY || state == CheStateEnum.LOCATION_SELECT){
			Thread.sleep(pickPause);
			picker.scanCommand(CheDeviceLogic.STARTWORK_COMMAND);
			states.remove(CheStateEnum.LOCATION_SELECT);
			picker.waitForOneOfCheStates(states, WAIT_TIMEOUT);
		}
		
		state = picker.getCurrentCheState();
		//If Che immediately arrives at the end-of-work state, stop processing this order
		if (state == CheStateEnum.NO_WORK || state == CheStateEnum.SETUP_SUMMARY){
			picker.logout();
			synchronized (lock) {
				report.append("No work generated for the CHE\n");
			}
			return;
		}
	
		List<WorkInstruction> picksList = picker.getAllPicksList();
		LOGGER.info("{} instructions to pick on the path", picksList.size());
		
		//Iterate over instructions, picking items, until no instructions left
		ArrayList<CheStateEnum> pickStates = Lists.newArrayList();
		pickStates.add(CheStateEnum.SCAN_SOMETHING);
		pickStates.add(CheStateEnum.DO_PICK);
		while(true){
			Thread.sleep(pickPause);
			WorkInstruction instruction = picker.getActivePick();
			if (instruction == null) {
				break;
			} else if (instruction.isHousekeeping()) {
				//Skip Housekeeping instruction
				picker.waitForCheState(CheStateEnum.DO_PICK, WAIT_TIMEOUT);
				picker.pickItemAuto();
			} else {
				//Process normal instruction
				picker.waitForOneOfCheStates(pickStates, WAIT_TIMEOUT);
				state = picker.getCurrentCheState();
				//When picking multiple orders containing same items, UPC scan is only needed once per item
				if (state == CheStateEnum.SCAN_SOMETHING) {
					//Scan UPC or skip it
					if (chance(chanceSkipUpc)) {
						LOGGER.info("Skip UPC scan");
						picker.scanSomething(CheDeviceLogic.SKIP_SCAN);
					} else {
						LOGGER.info("Scan UPC");
						picker.scanSomething(instruction.getItemId());
					}
					picker.waitForCheState(CheStateEnum.DO_PICK, WAIT_TIMEOUT);
				}
				//Pick item or short it
				if (chance(chanceShort)) {
					LOGGER.info("Short Item");
					picker.scanCommand("SHORT");
					picker.waitForCheState(CheStateEnum.SHORT_PICK, WAIT_TIMEOUT);
					int button = picker.buttonFor(picker.getActivePick());
					picker.pick(button,0);
					picker.waitForCheState(CheStateEnum.SHORT_PICK_CONFIRM, WAIT_TIMEOUT);
					picker.scanCommand("YES");
				} else {
					LOGGER.info("Pick Item");
					picker.pickItemAuto();
				}
			}
		}
		String msg = "Completed CHE run";
		LOGGER.info(msg);
	}

	private boolean chance(double percentage) {
		double rnd = Math.random();
		return rnd < percentage;
	}

	private PickSimulator getPicker(String pickerName) throws Exception {
		PickSimulator picker = pickers.get(pickerName);
		if (picker == null) {
			throw new Exception(String.format("Undefined picker '%s'. Execute '%s' first", pickerName, TEMPLATE_DEF_PICKER));
		}
		return picker;
	}
	
	private String exceptionToString(Exception e) { 
		report.append(e.getClass().getName()).append(": ");
		String error = e.getMessage();
		if (error == null || error.isEmpty()) {
			error = ExceptionUtils.getStackTrace(e);
		}
		return e.getClass().getName() + ": " + error + "\n";
	}
	
	private void throwIncorrectNumberOfArgumentsException(String expected) throws Exception{
		throw new Exception("Incorrect number of arguments. Expected '" + expected + "'");
	}
}
