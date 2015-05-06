import com.codeshelf.device.CheDeviceLogic;
import com.codeshelf.device.CheStateEnum;
import com.codeshelf.model.domain.WorkInstruction;
import com.codeshelf.sim.worker.PickSimulator;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

class PickOrders {
	int WAIT_TIMEOUT = 10000
	String workerId = "GroovyWorker1";
	
	def pickOrders(PickSimulator picker, ArrayList<String> containerList, double chanceSkipUpc, double chanceShort) {
		if (containerList == null || containerList.isEmpty()) {
			return "Empty container list";
		}
		println "Picking orders in containers " + containerList;
		 
		//Reset CHE to IDLE
		picker.logout()
		picker.waitForCheState(CheStateEnum.IDLE, WAIT_TIMEOUT)
	
		//Login into CHE
		picker.loginAndSetup(workerId);
		
		//Setup containers and compute work
		setupChe(picker, containerList)
		
		//Pick orders
		pick(picker, chanceSkipUpc, chanceShort);

		//Finish test by logging out
		picker.logout();
		picker.waitForCheState(CheStateEnum.IDLE, WAIT_TIMEOUT);
		return "Finished picking containers " + containerList;
	}
	
	boolean chance(double percentage) {
		double rnd = Math.random();
		return rnd < percentage;
	}
	
	/**
	 * This function places Containers on a CHE. It can be called from an IDLE or CONTAINER_SELECT state
	 */
	def setupChe(PickSimulator picker, ArrayList<String> containerList) {
		//Get to CONTAINER_SELECT state
		picker.waitForOneOfCheStates([CheStateEnum.IDLE, CheStateEnum.CONTAINER_SELECT], WAIT_TIMEOUT);
		CheStateEnum state = picker.getCurrentCheState();
		if (state == CheStateEnum.IDLE) {
			picker.loginAndSetup(workerId);
		}
		//Assign containers to positions
		int position = 1;
		for (container in containerList) {
			picker.setupContainer(container, position++ + "");
		}
		//Compute work
		picker.scanCommand(CheDeviceLogic.STARTWORK_COMMAND);
		boolean usesSummaryState = picker.getCheDeviceLogic().usesSummaryState();
		picker.waitForOneOfCheStates([CheStateEnum.SETUP_SUMMARY, CheStateEnum.LOCATION_SELECT, CheStateEnum.NO_WORK], WAIT_TIMEOUT);
		state = picker.getCurrentCheState();
		String noWorkMsg = "No work for these containers: " + containerList;
		if (state == CheStateEnum.NO_WORK){
			picker.logout();
			println(noWorkMsg);
			return noWorkMsg;
		}

	}
	
	def pick(PickSimulator picker, double chanceSkipUpc, double chanceShort){
		//Attempt to navigate from several initial states to the Pick state
		CheStateEnum state = picker.getCurrentCheState();
		if (state == CheStateEnum.IDLE) {
			picker.loginAndSetup(workerId);
		}
		picker.scanCommand(CheDeviceLogic.STARTWORK_COMMAND);
		picker.waitForOneOfCheStates([CheStateEnum.SETUP_SUMMARY, CheStateEnum.LOCATION_SELECT, CheStateEnum.NO_WORK, CheStateEnum.DO_PICK, CheStateEnum.SCAN_SOMETHING], WAIT_TIMEOUT);
		state = picker.getCurrentCheState();
		if (state == CheStateEnum.SETUP_SUMMARY || state == CheStateEnum.LOCATION_SELECT){
			picker.scanCommand(CheDeviceLogic.STARTWORK_COMMAND);
			picker.waitForOneOfCheStates([CheStateEnum.SETUP_SUMMARY, CheStateEnum.NO_WORK, CheStateEnum.DO_PICK, CheStateEnum.SCAN_SOMETHING], WAIT_TIMEOUT);
		}
		
		state = picker.getCurrentCheState();
		//If Che immediately arrives at the end-of-work state, stop processing this order
		if (state == CheStateEnum.NO_WORK || state == CheStateEnum.SETUP_SUMMARY){
			picker.logout();
			println("No work generated for the CHE");
			return "No work generated for the CHE";
		}
	
		def List<WorkInstruction> picksList = picker.getAllPicksList();
		println(picksList.size() + " instructions to pick on the path");
		
		//Iterate over instructions, picking items, until no instructions left
		while(true){
			WorkInstruction instruction = picker.getActivePick();
			if (instruction == null) {
				println("No active picks left");
				break;
			} else if (instruction.isHousekeeping()) {
				//Skip Housekeeping instruction
				picker.waitForCheState(CheStateEnum.DO_PICK, WAIT_TIMEOUT);
				picker.pickItemAuto();
			} else {
				//Process normal instruction
				picker.waitForOneOfCheStates([CheStateEnum.SCAN_SOMETHING, CheStateEnum.DO_PICK], WAIT_TIMEOUT);
				state = picker.getCurrentCheState();
				//When picking multiple orders containing same items, UPC scan is only needed once per item
				if (state == CheStateEnum.SCAN_SOMETHING) {
					//Scan UPC or skip it
					if (chance(chanceSkipUpc)) {
						println("Skip UPC scan");
						picker.scanSomething(CheDeviceLogic.SKIP_SCAN);
					} else {
						println("Scan UPC");
						picker.scanSomething(instruction.getItemId());
					}
					picker.waitForCheState(CheStateEnum.DO_PICK, WAIT_TIMEOUT);
				}
				//Thread.sleep(1000);
				//Pick item or short it
				if (chance(chanceShort)) {
					println("Short Item");
					picker.scanCommand("SHORT");
					picker.waitForCheState(CheStateEnum.SHORT_PICK, WAIT_TIMEOUT);
					int button = picker.buttonFor(picker.getActivePick());
					picker.pick(button,0);
					picker.waitForCheState(CheStateEnum.SHORT_PICK_CONFIRM, WAIT_TIMEOUT);
					picker.scanCommand("YES");
				} else {
					println("Pick Item");
					picker.pickItemAuto();
				}
			}
		}
	}
		
	def importOrders(String facilityId, String filename){
		String outFile = 'importedOrders.txt';
		def sout = new StringBuffer(), serr = new StringBuffer()
		//Run script to import order file
		def proc = ['src/test/resources/scripts/importOrders.sh', 'simulate@example.com', 'testme', facilityId, filename, outFile].execute();
		proc.consumeProcessOutput(sout, serr)
		//Warning: Can kill site controller for import isn't done by then
		proc.waitForOrKill(1000 * 60 * 25);
		if (sout.length() > 0) {
			println("out> $sout");
		}
		if (serr.length() > 0) {
			println("err> $serr");
		}
		//Retrieve results of importing orders
		JsonParser parser = new JsonParser();
		Object parsedOrdersRaw = parser.parse(new FileReader(outFile));
		JsonObject parsedOrders = (JsonObject) parsedOrdersRaw;
		JsonArray violations = parsedOrders.getAsJsonArray("violations");
		if (violations != null && violations.size() > 0) {
			println(violations);
			return "Unable to import orders. See console for details.";
		}
		//Generate a list of imported containers
		JsonArray ordersList = parsedOrders.getAsJsonArray("result");
		HashSet<String> containers = new HashSet<>();
		for (JsonObject order : ordersList) {
			String containerId = order["preAssignedContainerId"].getAsString();
			if (containerId != null) {
				containers.add(containerId);
			}
		}
		println("Imported containers: " + containers);
		return containers;
	}
	
	def importAndPickOrders(PickSimulator picker, String facilityId, String filename, int batchSize, double chanceSkipUpc, double chanceShort) {
		HashSet<String> containers = importOrders(facilityId, filename);
		
		//Assign containers to CHE and pick them in batches
		ArrayList containerBatch = new ArrayList<String>();
		for (String container : containers) {
			containerBatch.add(container);
			if (containerBatch.size >= batchSize) {
				pickOrders(picker, containerBatch, chanceSkipUpc, chanceShort);
				containerBatch = new ArrayList<String>();
			}
		}
		pickOrders(picker, containerBatch, chanceSkipUpc, chanceShort);
		return;
	}	
}