import com.codeshelf.device.CheDeviceLogic;
import com.codeshelf.device.CheStateEnum;
import com.codeshelf.model.domain.WorkInstruction;
import com.codeshelf.sim.worker.PickSimulator;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

class Picker {
	int WAIT_TIME = 10000
	
	def pickOrders(PickSimulator picker, String container, double chanceSkipUpc, double chanceShort) {
		println "picking order in container " + container + " " + picker
		//Reset CHE to IDLE
		picker.logout()
		picker.waitForCheState(CheStateEnum.IDLE, WAIT_TIME)
	
		//Login into CHE
		picker.loginAndSetup("GroovyWorker1")
		picker.waitForCheState(CheStateEnum.CONTAINER_SELECT, WAIT_TIME)
		//Set up order on the CHE and try to start pick
		picker.setupContainer(container, "1");
		picker.scanCommand(CheDeviceLogic.STARTWORK_COMMAND);
		boolean usesSummaryState = picker.getCheDeviceLogic().usesSummaryState();
		if (usesSummaryState) {
			picker.waitForOneOfCheStates([CheStateEnum.SETUP_SUMMARY, CheStateEnum.NO_WORK], WAIT_TIME);
		} else {
			picker.waitForOneOfCheStates([CheStateEnum.LOCATION_SELECT, CheStateEnum.NO_WORK], WAIT_TIME);
		}
		CheStateEnum state = picker.getCurrentCheState();
		//If CHE is in NO_WORK or NO_WORK_CURR_PATH state, exit without crashing.
		if (state == CheStateEnum.NO_WORK){
			println("No work for this order. Check if order in container " + container + " exists");
			picker.logout();
			return "No work found (during preview) for container " + container;
		}
	
		picker.scanCommand(CheDeviceLogic.STARTWORK_COMMAND);
		picker.waitForOneOfCheStates([CheStateEnum.SCAN_SOMETHING, CheStateEnum.NO_WORK,  CheStateEnum.SETUP_SUMMARY], WAIT_TIME);
		state = picker.getCurrentCheState();
		//If Che immediately arrives at the end-of-work state, stop processing this order
		if (state == CheStateEnum.NO_WORK || (usesSummaryState && state == CheStateEnum.SETUP_SUMMARY)){
			println("No work for this order. Check if order in container " + container + " exists");
			picker.logout();
			return "No work found (during start pick) for container " + container;
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
				picker.waitForCheState(CheStateEnum.DO_PICK, WAIT_TIME);
				picker.pickItemAuto();
			} else {
				//Process normal instruction
				picker.waitForCheState(CheStateEnum.SCAN_SOMETHING, WAIT_TIME);
				//Scan UPC or skip it
				if (chance(chanceSkipUpc)) {
					println("Skip UPC scan");
					picker.scanSomething(CheDeviceLogic.SKIP_SCAN);
				} else {
					println("Scan UPC");
					picker.scanSomething(instruction.getItemId());
				}
				picker.waitForCheState(CheStateEnum.DO_PICK, WAIT_TIME);
				//Pick item or short it
				if (chance(chanceShort)) {
					println("Short Item");
					picker.scanCommand("SHORT");
					picker.waitForCheState(CheStateEnum.SHORT_PICK, WAIT_TIME);
					picker.pick(1,0);
					picker.waitForCheState(CheStateEnum.SHORT_PICK_CONFIRM, WAIT_TIME);
					picker.scanCommand("YES");
				} else {
					println("Pick Item");
					picker.pickItemAuto();
				}
			}
		}
		
		//Finish test by logging out
		picker.logout();
		picker.waitForCheState(CheStateEnum.IDLE, WAIT_TIME);
		return "Finished picking container " + container;
	}
	
	boolean chance(double percentage) {
		double rnd = Math.random();
		return rnd < percentage;
	}
	
	def importAndPickOrders(PickSimulator picker, String filename, String outFile) {
		def sout = new StringBuffer(), serr = new StringBuffer()
		def proc = ['src/test/resources/scripts/importOrders.sh', 'simulate@example.com', 'testme', '7d16e862-32db-4270-8eda-aff14629aceb', filename, outFile].execute();
		proc.consumeProcessOutput(sout, serr)
		//Can kill site controller
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
		
		//PickOrders pickClass = new PickOrders();
		//pickClass.pickOrders(picker, "26768711", 0, 0);
		for (String container : containers) {
			pickOrders(picker, container, (double)0.0, (double)0.0);
		}	
		return;
	}
}

picker = new PickSimulator(deviceManager, "0x0000009B");
filename = '../../Desktop/Orders/orders_10.csv';
outFile = 'importedOrders.txt'
new Picker().importAndPickOrders(picker, filename, 'importedOrders.txt')