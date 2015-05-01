import com.codeshelf.device.CheDeviceLogic;
import com.codeshelf.device.CheStateEnum;
import com.codeshelf.model.domain.WorkInstruction;
import com.codeshelf.sim.worker.PickSimulator;

class PickOrders {
	int WAIT_TIME = 10000
	
	def pickOrders(PickSimulator picker, String order, double chanceSkipUpc, double chanceShort) {
		println "picking order " + order + " " + picker
		//Reset CHE to IDLE
		picker.logout()
		picker.waitForCheState(CheStateEnum.IDLE, WAIT_TIME)

		//Login into CHE
		picker.loginAndSetup("GroovyWorker1")
		picker.waitForCheState(CheStateEnum.CONTAINER_SELECT, WAIT_TIME)
		
		//Set up order on the CHE and try to start pick
		picker.setupContainer(order, "1");
		picker.scanCommand(CheDeviceLogic.STARTWORK_COMMAND);
		boolean usesSummaryState = picker.getCheDeviceLogic().usesSummaryState();
		try {
			if (usesSummaryState) {
				picker.waitForCheState(CheStateEnum.SETUP_SUMMARY, WAIT_TIME);
			} else {
				picker.waitForCheState(CheStateEnum.LOCATION_SELECT, WAIT_TIME);
			}
		} catch (IllegalStateException e) {
			CheStateEnum state = picker.getCurrentCheState();
			//If CHE is in NO_WORK or NO_WORK_CURR_PATH state, exit without crashing.
			if (state == CheStateEnum.NO_WORK || state == CheStateEnum.SETUP_SUMMARY){
				println("No work for this order. Check if order " + order + " exists");
				picker.logout();
				return;
			}
			throw e; 
		}
		picker.scanCommand(CheDeviceLogic.STARTWORK_COMMAND);
		picker.waitForCheState(CheStateEnum.SCAN_SOMETHING, WAIT_TIME);

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
	}
	
	boolean chance(double percentage) {
		double rnd = Math.random();
		return rnd < percentage;
	}
}