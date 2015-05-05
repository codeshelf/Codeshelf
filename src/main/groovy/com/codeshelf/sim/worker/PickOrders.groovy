import com.codeshelf.device.CheDeviceLogic;
import com.codeshelf.device.CheStateEnum;
import com.codeshelf.model.domain.WorkInstruction;
import com.codeshelf.sim.worker.PickSimulator;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

class PickOrders {
	int WAIT_TIMEOUT = 10000
	
	def pickOrders(PickSimulator picker, ArrayList<String> containerList, double chanceSkipUpc, double chanceShort) {
		if (containerList == null || containerList.isEmpty()) {
			return "Enpty container list";
		}
				
		println "picking order in containers " + containerList; 
		//Reset CHE to IDLE
		picker.logout()
		picker.waitForCheState(CheStateEnum.IDLE, WAIT_TIMEOUT)
	
		//Login into CHE
		picker.loginAndSetup("GroovyWorker1")
		picker.waitForCheState(CheStateEnum.CONTAINER_SELECT, WAIT_TIMEOUT)
		//Set up order on the CHE and try to start pick
		//StringBuilder containersStr = new StringBuilder();
		int position = 1;
		for (container in containerList) {
			//containersStr.append(container).append(" ");
			picker.setupContainer(container, position++ + "");
		}
		//picker.setupContainer(container, "1");
		picker.scanCommand(CheDeviceLogic.STARTWORK_COMMAND);
		boolean usesSummaryState = picker.getCheDeviceLogic().usesSummaryState();
		if (usesSummaryState) {
			picker.waitForOneOfCheStates([CheStateEnum.SETUP_SUMMARY, CheStateEnum.NO_WORK], WAIT_TIMEOUT);
		} else {
			picker.waitForOneOfCheStates([CheStateEnum.LOCATION_SELECT, CheStateEnum.NO_WORK], WAIT_TIMEOUT);
		}
		CheStateEnum state = picker.getCurrentCheState();
		String noWorkMsg = "No work for these containers: " + containerList;
		if (state == CheStateEnum.NO_WORK){
			picker.logout();
			println(noWorkMsg);
			return noWorkMsg;
		}
	
		picker.scanCommand(CheDeviceLogic.STARTWORK_COMMAND);
		picker.waitForOneOfCheStates([CheStateEnum.SCAN_SOMETHING, CheStateEnum.NO_WORK,  CheStateEnum.SETUP_SUMMARY], WAIT_TIMEOUT);
		state = picker.getCurrentCheState();
		//If Che immediately arrives at the end-of-work state, stop processing this order
		if (state == CheStateEnum.NO_WORK || (usesSummaryState && state == CheStateEnum.SETUP_SUMMARY)){
			picker.logout();
			println(noWorkMsg);
			return noWorkMsg;
		}
	
		def List<WorkInstruction> picksList = picker.getAllPicksList();
		println(picksList.size() + " instructions to pick on the path");
		
		//Iterate until no instructions left
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
		
		//Finish test by logging out
		picker.logout();
		picker.waitForCheState(CheStateEnum.IDLE, WAIT_TIMEOUT);
		return "Finished picking containers " + containerList;
	}
	
	boolean chance(double percentage) {
		double rnd = Math.random();
		return rnd < percentage;
	}
		
	def importAndPickOrders(PickSimulator picker, String facilityId, String filename, int batchSize, double chanceSkipUpc, double chanceShort) {
		String outFile = 'importedOrders.txt';
		def sout = new StringBuffer(), serr = new StringBuffer()
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
		
		JsonParser parser = new JsonParser();
		Object parsedOrdersRaw = parser.parse(new FileReader(outFile));
		JsonObject parsedOrders = (JsonObject) parsedOrdersRaw;
		JsonArray violations = parsedOrders.getAsJsonArray("violations");
		if (violations != null && violations.size() > 0) {
			println(violations);
			return "Unable to import orders. See console for details.";
		}
		
		JsonArray ordersList = parsedOrders.getAsJsonArray("result");
		HashSet<String> containers = new HashSet<>();
		for (JsonObject order : ordersList) {
			println(order);
			String containerId = order["preAssignedContainerId"].getAsString();
			if (containerId != null) {
				containers.add(containerId);
			}
		}
		
		ArrayList containerList = new ArrayList<String>();
		for (String container : containers) {
			containerList.add(container);
			if (containerList.size >= batchSize) {
				pickOrders(picker, containerList, chanceSkipUpc, chanceShort);
				containerList = new ArrayList<String>();
			}
		}
		pickOrders(picker, containerList, chanceSkipUpc, chanceShort);
		return;
	}
}