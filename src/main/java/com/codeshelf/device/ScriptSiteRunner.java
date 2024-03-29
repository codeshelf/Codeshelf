package com.codeshelf.device;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.api.pickscript.ScriptParser;
import com.codeshelf.flyweight.command.NetGuid;
import com.codeshelf.flyweight.controller.INetworkDevice;
import com.codeshelf.flyweight.controller.NetworkDeviceStateEnum;
import com.codeshelf.model.domain.WorkInstruction;
import com.codeshelf.sim.worker.PickSimulator;
import com.codeshelf.util.CsExceptionUtils;
import com.codeshelf.util.ThreadUtils;
import com.codeshelf.ws.protocol.message.ScriptMessage;
import com.google.common.collect.Lists;

public class ScriptSiteRunner {
	private final static String TEMPLATE_DEF_CHE = "defChe (ches <cheName> <cheGuid>)";
	private final static String TEMPLATE_DEF_CHE_USE_RADIO = "defCheUseRadio (ches <cheName> <cheGuid>)";
	private final static String TEMPLATE_LOGIN = "login <cheName> <workerId> [state]";
	private final static String TEMPLATE_LOGIN_SETUP = "loginSetup <cheName> <workerId>";
	private final static String TEMPLATE_LOGIN_REMOTE = "loginRemote <cheName> <workerId> <linkToChe>";
	private final static String TEMPLATE_SCAN = "scan <cheName> <scan> [expectedState]";
	private final static String TEMPLATE_SETUP_CART = "setupCart <cheName> <containers>";
	private final static String TEMPLATE_SETUP_MANY = "setupMany <'start'/'stop'>";
	private final static String TEMPLATE_WAIT_FOR_STATES = "waitForState <cheName> <states>";
	private final static String TEMPLATE_SET_PARAMS = "setParams (assignments 'pickSpeed'/'skipFreq'/'shortFreq'=<value>)";
	private final static String TEMPLATE_PICK = "pick <cheNames>";
	private final static String TEMPLATE_CHE_EXEC = "cheExec <cheName> <cheCommand> [arguments]";
	private final static String TEMPLATE_ORDER_TO_WALL = "orderToWall <cheName> <orderId> <location>";
	private final static String TEMPLATE_WAIT_SECONDS = "waitSeconds <seconds>";
	private final static String TEMPLATE_WAIT_DEVICES = "waitForDevices <devices>";
	private final static String TEMPLATE_LOGOUT = "logout [cheNames]";

	private static final Logger LOGGER = LoggerFactory.getLogger(ScriptSiteRunner.class);
	private static final int WAIT_TIMEOUT = 4000;
	private static final int COMPUTE_WORK_TIMEOUT = 35000;
	private int pickPauseMs = 0, staggerMs = 0;
	private double chanceSkipUpc = 0, chanceShort = 0;
	private LinkedHashMap<String, PickSimulator> ches = new LinkedHashMap<>();	//LinkedHashMap allows iteration in order of insertion
	private StringBuilder report = new StringBuilder();
	private final CsDeviceManager deviceManager;
	private static final Object lock = new Object();
	private Random rnd_gen = new Random(System.currentTimeMillis());
	private boolean accumulatingSetupCommands = false;
	private List<String[]> accumulatedSetupCommands = new ArrayList<>();
	
	public ScriptSiteRunner(CsDeviceManager deviceManager) {
		this.deviceManager = deviceManager;
	}
	
	/**
	 * This method need to run asynchronously to not hold up the message queue between Server and Site
	 */
	public void runScript(final ScriptMessage message) {
		Runnable runnable = new Runnable() {
			@Override
			public void run() {
				List<String> lines = message.getLines();
				if (lines == null) {
					message.setMessageError("Empty site script");
					deviceManager.clientEndpoint.sendMessage(message);
					return;
				}
				report.append("SITE\n");
				try {
					for (String line : lines) {
						processLine(line);
					}
					if (accumulatingSetupCommands) {
						throw new Exception("'setupMany' block wasn't closed with 'setupMany stop'");
					}
					LOGGER.info("Site script block completed");
					report.append("***Site Script Segment Completed Successfully***\n");
				} catch (Exception e) {
					LOGGER.info("Logging out from all CHEs due to error");
					try {logoutAll();} catch (Exception e1) {}
					String error = CsExceptionUtils.exceptionToString(e);
					report.append(error).append("Logging out from all CHEs due to this error\n");
					message.setMessageError(error);
				}
				
				message.setResponse(report.toString());
				deviceManager.clientEndpoint.sendMessage(message);
			}
		};
		new Thread(runnable).start();
	}
	
	private void processLine(String line) throws Exception {
		LOGGER.info("Runing script line " + line);
		report.append("Run: ").append(line).append("\n");
		if (line == null || line.isEmpty()) {
			return;
		}
		String parts[] = ScriptParser.splitLine(line);
		
		String command = parts[0];
		if (command.equalsIgnoreCase("cheExec")) {
			processCheExecCommand(parts);
		} else if (command.equalsIgnoreCase("defChe")) {
			processDefineCheCommand(parts, false);
		} else if (command.equalsIgnoreCase("defCheUseRadio")) {
			processDefineCheCommand(parts, true);
		} else if (command.equalsIgnoreCase("login")) {
			processLoginCommand(parts);
		} else if (command.equalsIgnoreCase("loginSetup")) {
			processLoginSetupCommand(parts);
		} else if (command.equalsIgnoreCase("loginRemote")) {
			processLoginRemoteCommand(parts);
		} else if (command.equalsIgnoreCase("scan")) {
			processScanCommand(parts);
		} else if (command.equalsIgnoreCase("orderToWall")) {
			processOrderToWallCommand(parts);
		} else if (command.equalsIgnoreCase("waitForState")) {
			processWaitForStatesCommand(parts);
		} else if (command.equalsIgnoreCase("setupCart")) {
			processSetupCartCommand(parts);
		} else if (command.equalsIgnoreCase("setupMany")) {
			processSetupManyCommand(parts);
		} else if (command.equalsIgnoreCase("setParams")) {
			processSetParamsCommand(parts);
		} else if (command.equalsIgnoreCase("pick")) {
			processPickCommand(parts);
		} else if (command.equalsIgnoreCase("pickAll")) {
			processPickAllCommand();
		} else if (command.equalsIgnoreCase("waitSeconds")) {
			processWaitSecondsCommand(parts);
		}  else if (command.equalsIgnoreCase("waitForDevices")) {
			processWaitForDevicesCommand(parts);
		}  else if (command.equalsIgnoreCase("logout")) {
			processLogoutCommand(parts);
		} else if (command.startsWith("//")) {
		} else {
			throw new Exception("Invalid command '" + command + "'. Expected [cheExec, defChe, defCheUseRadio, login, loginSetup, loginRemote, scan, orderToWall, waitForState, setupCard, setupMany, setParams, pick, pickAll, waitSeconds, waitForDevices, logout, //]");
		}
	}
	
	/**
	 * Expects to see command
	 * cheExec <cheName> <cheCommand> [arguments]
	 * @throws Exception 
	 */
	private void processCheExecCommand(String parts[]) throws Exception {
		if (parts.length < 3) {
			throwIncorrectNumberOfArgumentsException(TEMPLATE_CHE_EXEC);
		}
		String cheName = parts[1];
		PickSimulator che = getChe(cheName);
		String cheCommand = parts[2];
		if (parts.length == 3){
			Method method = PickSimulator.class.getDeclaredMethod(cheCommand);
			method.invoke(che);
		} else if (parts.length == 4){
			Method method = PickSimulator.class.getDeclaredMethod(cheCommand, String.class);
			method.invoke(che, parts[3]);
		} else if (parts.length == 5){
			Method method = PickSimulator.class.getDeclaredMethod(cheCommand, String.class, String.class);
			method.invoke(che, parts[3], parts[4]);
		} else {
			throw new Exception("Ability to call PickSimulator methods wih more than 2 arguments not yet implemented.");
		}
	}
	
	/**
	 * Expects to see command
	 * defChe (ches <cheName> <cheGuid>)
	 * or
	 * defCheUseRadio (ches <cheName> <cheGuid>)
	 * @throws Exception
	 */
	private void processDefineCheCommand(String parts[], boolean withRadio) throws Exception {
		int blockLength = 2;
		if (parts.length < 3 || (parts.length - 1) % blockLength != 0 ){
			if (withRadio) {
				throwIncorrectNumberOfArgumentsException(TEMPLATE_DEF_CHE_USE_RADIO);
			} else {
				throwIncorrectNumberOfArgumentsException(TEMPLATE_DEF_CHE);
			}
		}
		int totalBlocks = (parts.length - 1) / blockLength;
		for (int blockNum = 0; blockNum < totalBlocks; blockNum++) {
			int offset = 1 + blockNum * blockLength;
			String cheName = parts[offset + 0];
			NetGuid cheGuid = new NetGuid(parts[offset + 1]);
			//In case, the device has just been added to the system, wait for Server to send device info to Site
			long start = System.currentTimeMillis(), now = System.currentTimeMillis();
			while (now < start + 15 * 1000){
				if (deviceManager.getDeviceByGuid(cheGuid) != null){
					break;
				}
				Thread.sleep(1000);
				now = System.currentTimeMillis();
			}
			
			PickSimulator che = new PickSimulator(deviceManager, cheGuid);
			che.setUseRadio(withRadio);
			ches.put(cheName, che);
		}
	}
	
	/**
	 * Expects to see command
	 * login <cheName> <workerId> [state]
	 * @throws Exception
	 */
	private void processLoginCommand(String parts[]) throws Exception {
		if (parts.length != 3 && parts.length != 4){
			throwIncorrectNumberOfArgumentsException(TEMPLATE_LOGIN);
		}
		PickSimulator che = getChe(parts[1]);
		if (parts.length == 3) {
			che.login(parts[2]);
		} else {
			che.loginAndCheckState(parts[2], CheStateEnum.valueOf(parts[3]));
		}
	}

	/**
	 * Expects to see command
	 * loginSetup <cheName> <workerId>
	 * @throws Exception
	 */
	private void processLoginSetupCommand(String parts[]) throws Exception {
		if (parts.length != 3){
			throwIncorrectNumberOfArgumentsException(TEMPLATE_LOGIN_SETUP);
		}
		PickSimulator che = getChe(parts[1]);
		che.loginAndSetup(parts[2]);
	}

	/**
	 * Expects to see command
	 * loginRemote <cheName> <workerId> <linkToCheName>
	 * @throws Exception
	 */
	private void processLoginRemoteCommand(String parts[]) throws Exception {
		if (parts.length != 4){
			throwIncorrectNumberOfArgumentsException(TEMPLATE_LOGIN_REMOTE);
		}
		PickSimulator mobileChe = getChe(parts[1]);
		String connectTo = parts[3];
		mobileChe.loginAndRemoteLink(parts[2], connectTo);
	}
	
	/**
	 * Expects to see command
	 * scan <cheName> <scan> [expectedState]
	 * @throws Exception
	 */
	private void processScanCommand(String parts[]) throws Exception {
		if (parts.length != 3 && parts.length != 4){
			throwIncorrectNumberOfArgumentsException(TEMPLATE_SCAN);
		}
		PickSimulator che = getChe(parts[1]);
		che.scanSomething(parts[2]);
		if (parts.length == 4) {
			che.waitForCheState(CheStateEnum.valueOf(parts[3]), WAIT_TIMEOUT);
		}
	}
	
	/**
	 * Expects to see command
	 * orderToWall <cheName> <orderId> <location>
	 * @throws Exception
	 */
	private void processOrderToWallCommand(String parts[]) throws Exception {
		if (parts.length != 4){
			throwIncorrectNumberOfArgumentsException(TEMPLATE_ORDER_TO_WALL);
		}
		PickSimulator che = getChe(parts[1]);
		che.waitForCheState(CheStateEnum.PUT_WALL_SCAN_ORDER, WAIT_TIMEOUT);
		che.scanOrderId(parts[2]);
		che.waitForCheState(CheStateEnum.PUT_WALL_SCAN_LOCATION, WAIT_TIMEOUT);
		che.scanSomething(parts[3]);
		che.waitForCheState(CheStateEnum.PUT_WALL_SCAN_ORDER, WAIT_TIMEOUT);
	}
	
	/**
	 * Expects to see command
	 * waitForState <cheName> <states>
	 * @throws Exception
	 */
	private void processWaitForStatesCommand(String parts[]) throws Exception {
		if (parts.length < 3){
			throwIncorrectNumberOfArgumentsException(TEMPLATE_WAIT_FOR_STATES);
		}
		PickSimulator che = getChe(parts[1]);
		ArrayList<CheStateEnum> states = Lists.newArrayList();
		for (int i = 2; i < parts.length; i++) {
			CheStateEnum state = CheStateEnum.valueOf(parts[i]);
			states.add(state);
		}
		che.waitForCheStates(states, WAIT_TIMEOUT);
	}
	
	/**
	 * Expects to see command
	 * "setParams (assignments 'pickSpeed'/'skipFreq'/'shortFreq'=<value>)";
	 * @throws Exception
	 */
	private void processSetParamsCommand(String parts[]) throws Exception {
		if (parts.length < 2){
			throwIncorrectNumberOfArgumentsException(TEMPLATE_SET_PARAMS);
		}
		HashSet<String> madeAssignments = new HashSet<>();
		String pickSpeedName = "pickSpeed", skipFreqName = "skipFreq", shortFreqName = "shortFreq", staggerName = "stagger";
		String assignment[], name, value;
		for (int i = 1; i < parts.length; i++) {
			assignment = parts[i].split("=");
			if (assignment.length != 2) {
				throw new Exception("Count not process assignment " + parts[i]);
			}
			name = assignment[0];
			value = assignment[1]; 
			//Verify that this command does not repeat property assignments
			if (madeAssignments.contains(name)) {
				throw new Exception("Attempting to set " + name + " multiple times in this command");
			}
			madeAssignments.add(name);
			//Save property value
			if (name.equalsIgnoreCase(pickSpeedName)) {
				double pickPauseSecMax = 2 * 60, pickPauseSec = Double.parseDouble(value);
				if (pickPauseSec > pickPauseSecMax) {
					throw new Exception("Tring to set pickSpeed to " + pickPauseSec + " seconds. Max value = " + pickPauseSecMax);
				}
				pickPauseMs = (int)(pickPauseSec * 1000);
			} else if (name.equalsIgnoreCase(skipFreqName)) {
				chanceSkipUpc = Double.parseDouble(value);
				validateFrequency(chanceSkipUpc, skipFreqName);
			} else if (name.equalsIgnoreCase(shortFreqName)) {
				chanceShort = Double.parseDouble(value);
				validateFrequency(chanceShort, shortFreqName);
			} else if (name.equalsIgnoreCase(staggerName)) {
				double staggerSecMax = 2 * 60, staggerSec = Double.parseDouble(value);
				if (staggerSec > staggerSecMax) {
					throw new Exception("Tring to set stagger to " + staggerSec + " seconds. Max value = " + staggerSecMax);
				}
				staggerMs = (int)(staggerSec * 1000);
			} else {
				throw new Exception(String.format("Unknown pick property name %s [%s/%s/%s/%s]", name, pickSpeedName, skipFreqName, shortFreqName, staggerName));
			}
		}
	}
	
	private void validateFrequency(Double frequency, String fieldName) throws Exception {
		if (frequency < 0 || frequency > 1) {
			throw new Exception("Attepting to set invalid frequency value " + frequency + " for " + fieldName + ". Allowed range [0-1]");
		}
	}

	/**
	 * Expects to see command
	 * setupCart <cheName> <containers>
	 * @throws Exception
	 */
	private void processSetupCartCommand(String parts[]) throws Exception {
		if (parts.length < 3){
			throwIncorrectNumberOfArgumentsException(TEMPLATE_SETUP_CART);
		}
		if (accumulatingSetupCommands) {
			accumulatedSetupCommands.add(parts);
		} else {
			String cheName = parts[1];
			PickSimulator che = getChe(cheName);
			che.waitForThisOrLinkedCheState(CheStateEnum.CONTAINER_SELECT, WAIT_TIMEOUT);
			String container;
			for (int i = 2; i < parts.length; i++){
				container = parts[i];
				if (container != null && !container.isEmpty()){
					che.setupContainer(parts[i], (i-1)+"");
				}
				ThreadUtils.sleep(pickPauseMs);
			}
		}
	}
	
	/**
	 * Expects to see command
	 * setupMany <'start'/'end'>
	 * @throws Exception
	 */
	private void processSetupManyCommand(String parts[]) throws Exception {
		if (parts.length != 2){
			throwIncorrectNumberOfArgumentsException(TEMPLATE_SETUP_MANY);
		}
		String action = parts[1];
		boolean start = "start".equalsIgnoreCase(action), stop = "stop".equalsIgnoreCase(action);
		if (!start && !stop){
			throw new Exception("Unknown setupMany parameter " + action + ". Need to be 'start' or 'stop'");
		}
		if (start){
			if (accumulatingSetupCommands){
				throw new Exception("Running 'setupMany start' without closing the previously open block with 'setupMany stop'");
			}
			accumulatedSetupCommands.clear();
			accumulatingSetupCommands = true;
		} else {
			if (!accumulatingSetupCommands){
				throw new Exception("Running 'setupMany stop' without previously opening the block with 'setupMany start'");
			}
			accumulatingSetupCommands = false;
			final AtomicBoolean failed = new AtomicBoolean(false);
			ExecutorService executor = Executors.newFixedThreadPool(accumulatedSetupCommands.size());
			for (final String[] commands : accumulatedSetupCommands) {
					Runnable runnable = new Runnable() {
						@Override
						public void run() {
							try {
								processSetupCartCommand(commands);
							} catch (Exception e) {
								report.append(CsExceptionUtils.exceptionToString(e)).append("\n");
								synchronized (failed) {
									failed.set(true);
								}
							}
						}
					};
					executor.execute(runnable);
					ThreadUtils.sleep(staggerMs);
			}
			executor.shutdown();
			executor.awaitTermination(5, TimeUnit.MINUTES);
			accumulatedSetupCommands.clear();
			if (failed.get()) {
				throw new Exception("Error executing the 'setupMany' block");
			}
		}
	}
	
	/**
	 * Expects to see command
	 * logout [cheNames]
	 * @throws Exception 
	 */
	private void processLogoutCommand(String parts[]) throws Exception {
		if (parts.length < 1){
			throwIncorrectNumberOfArgumentsException(TEMPLATE_LOGOUT);
		}
		if (parts.length == 1){
			logoutAll();
		} else {
			for (int i = 1; i < parts.length; i++) {
				PickSimulator che = getChe(parts[i]);
				che.logout();
			}
		}
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
		LOGGER.info("Pause site script");
		Thread.sleep(seconds * 1000); 
	}
	
	/**
	 * Expects to see command
	 * waitForDevices <devices>
	 * @throws Exception 
	 */
	private void processWaitForDevicesCommand(String parts[]) throws Exception {
		if (parts.length < 2){
			throwIncorrectNumberOfArgumentsException(TEMPLATE_WAIT_DEVICES);
		}
		NetGuid[] devices = new NetGuid[parts.length - 1];
		for (int i = 1; i < parts.length; i++) {
			devices[i-1] = new NetGuid(parts[i]);
		}
		long start = System.currentTimeMillis(), now = System.currentTimeMillis();
		StringBuilder missingLog = new StringBuilder();
		boolean allFound = false;
		while (now < start + 15 * 1000){
			missingLog = new StringBuilder();
			allFound = true;
			for (NetGuid netGuid : devices) {
				INetworkDevice device = deviceManager.getDeviceByGuid(netGuid);
				if (device == null) {
					allFound = false;
					missingLog.append(netGuid).append(" - unknown guid").append(", ");
				} else {
					NetworkDeviceStateEnum state = device.getDeviceStateEnum();
					if (state != NetworkDeviceStateEnum.STARTED){
						String stateStr = state == null? "NotConnected" : state.toString();
						missingLog.append(netGuid).append(" - ").append(stateStr).append(", ");
						allFound = false;
					}
				}
			}
			if (allFound) {
				break;
			}
			Thread.sleep(1000);
			now = System.currentTimeMillis();
		}
		if (!allFound) {
			throw new Exception("Following devices are unknown or not connected: " + missingLog);
		}
	}


	/**
	 * If needed, this command can be made part of the available functionality
	 */
	private void logoutAll(){
		for (PickSimulator che : ches.values()){
			che.logout();
		}
	}
	
	/**
	 * Expects to see command
	 * pickAll
	 * @throws Exception
	 */
	private void processPickAllCommand() throws Exception{
		LOGGER.info("Start che picks");
		ExecutorService executor = Executors.newFixedThreadPool(ches.size());
		for (final PickSimulator che : ches.values()){
			generateWork(che);
			Runnable runnable = new Runnable() {
				@Override
				public void run() {
					try {
						pick(che);
					} catch (Exception e) {
						report.append(CsExceptionUtils.exceptionToString(e)).append("\n");
					}
				}
			};
			executor.execute(runnable);
			ThreadUtils.sleep(staggerMs);
		}
		executor.shutdown();
		executor.awaitTermination(5, TimeUnit.MINUTES);
		LOGGER.info("che picks done");
	}
	
	/**
	 * Expects to see command
	 * pick <cheName>
	 * @throws Exception
	 */
	private void processPickCommand(String parts[]) throws Exception{
		if (parts.length != 2){
			throwIncorrectNumberOfArgumentsException(TEMPLATE_PICK);
		}
		PickSimulator che = getChe(parts[1]);
		generateWork(che);
		pick(che);
	}
	
	private void generateWork(PickSimulator che) {
		//At this point, CHE should already have containers set up. Start START to advance to Pick or Review stage
		che.scanCommand(CheDeviceLogic.STARTWORK_COMMAND);
		ArrayList<CheStateEnum> states = Lists.newArrayList();
		states.add(CheStateEnum.SETUP_SUMMARY);
		states.add(CheStateEnum.DO_PICK);
		states.add(CheStateEnum.SCAN_SOMETHING);
		che.waitForCheStates(states, COMPUTE_WORK_TIMEOUT);
		CheStateEnum state = che.getCurrentCheState();
		
		//If CHE is in a Review stage, scan START again to advance to Pick stage
		if (state == CheStateEnum.SETUP_SUMMARY){
			ThreadUtils.sleep(pickPauseMs);
			che.scanCommand(CheDeviceLogic.STARTWORK_COMMAND);
			che.waitForCheStates(states, COMPUTE_WORK_TIMEOUT);
		}
	}
	
	private void pick(PickSimulator che) throws Exception{
		CheStateEnum state = che.getCurrentCheState();
		//If Che immediately arrives at the end-of-work state, stop processing this order
		if (state == CheStateEnum.SETUP_SUMMARY){
			che.logout();
			synchronized (lock) {
				report.append("No work generated for " + che.getCheDeviceLogic().getGuidNoPrefix() + "\n");
			}
			return;
		}
	
		List<WorkInstruction> picksList = che.getAllPicksList();
		LOGGER.info("{} instructions to pick on the path", picksList.size());
		
		//Iterate over instructions, picking items, until no instructions left
		while(true){
			Thread.sleep(pickPauseMs);
			WorkInstruction instruction = che.getFirstActivePick();
			if (instruction == null) {
				break;
			} else if (instruction.isHousekeeping()) {
				//Skip Housekeeping instruction
				che.waitForCheState(CheStateEnum.DO_PICK, WAIT_TIMEOUT);
				che.pickItemAuto();
			} else {
				//Process normal instruction
				che.waitForCheStates(PickSimulator.states(CheStateEnum.SCAN_SOMETHING,  CheStateEnum.DO_PICK), WAIT_TIMEOUT);
				state = che.getCurrentCheState();
				//When picking multiple orders containing same items, UPC scan is only needed once per item
				if (state == CheStateEnum.SCAN_SOMETHING) {
					//Scan UPC or skip it
					if (chance(chanceSkipUpc)) {
						LOGGER.info("Skip UPC scan");
						che.scanSomething(CheDeviceLogic.SKIP_SCAN);
					} else {
						LOGGER.info("Scan UPC");
						che.scanSomething(instruction.getItemId());
					}
					che.waitForCheState(CheStateEnum.DO_PICK, WAIT_TIMEOUT);
				}
				//Pick item or short it
				if (chance(chanceShort)) {
					LOGGER.info("Short Item");
					che.scanCommand("SHORT");
					che.waitForCheState(CheStateEnum.SHORT_PICK, WAIT_TIMEOUT);
					int button = che.buttonFor(instruction);
					che.pick(button,0);
					che.waitForCheState(CheStateEnum.SHORT_PICK_CONFIRM, WAIT_TIMEOUT);
					che.scanCommand("YES");
				} else {
					LOGGER.info("Pick Item");
					che.pickItemAuto();
				}
			}
		}
		String msg = "Completed CHE run";
		LOGGER.info(msg);
	}
		
	private boolean chance(double percentage) {
		double rnd = rnd_gen.nextDouble();
		return rnd < percentage;
	}

	private PickSimulator getChe(String cheName) throws Exception {
		PickSimulator che = ches.get(cheName);
		if (che == null) {
			throw new Exception(String.format("Undefined che '%s'. Execute '%s' first", cheName, TEMPLATE_DEF_CHE));
		}
		return che;
	}
		
	private void throwIncorrectNumberOfArgumentsException(String expected) throws Exception{
		throw new Exception("Incorrect number of arguments. Expected '" + expected + "'");
	}
}
