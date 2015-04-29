import com.codeshelf.device.CheDeviceLogic;
import com.codeshelf.device.CheStateEnum;
import com.codeshelf.model.domain.WorkInstruction;

class PickOrders {
	int WAIT_TIME = 4000
	int UPC_SKIP_FREQ = 2;
	
	def pickOrders(picker, order) {
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
		try {
			picker.waitForCheState(CheStateEnum.LOCATION_SELECT, WAIT_TIME);
		} catch (IllegalStateException e) {
			CheStateEnum state = picker.getCurrentCheState();
			//If CHE is in NO_WORK or NO_WORK_CURR_PATH state, exit without crashing.
			if (state == CheStateEnum.NO_WORK){
				println("No work for this order. Check if order " + order + " exists");
				picker.logout();
				return;
			} else if (state == CheStateEnum.NO_WORK_CURR_PATH) {
				println("No work for this order on current path.");
				picker.logout();
				return;
			}
			throw e; 
		}
		picker.scanCommand(CheDeviceLogic.STARTWORK_COMMAND);
		picker.waitForCheState(CheStateEnum.SCAN_SOMETHING, WAIT_TIME);

		def List<WorkInstruction> picksList = picker.getAllPicksList();
		println(picksList.size() + " instructions to pick on the path");
		
		int count = 1;
		//Iterate until no instructions left
		while(true){
			WorkInstruction instruction = picker.getActivePick();
			if (instruction == null) {
				print("No active picks left");
				break;
			} else if (!instruction.isHousekeeping()) {
				picker.waitForCheState(CheStateEnum.SCAN_SOMETHING, WAIT_TIME);
				if (count++ % UPC_SKIP_FREQ == 0) {
					println("Skip UPC scan");
					picker.scanSomething(CheDeviceLogic.SKIP_SCAN);
				} else {
					println("Scan UPC");
					picker.scanSomething(instruction.getItemId());
				}
				picker.waitForCheState(CheStateEnum.DO_PICK, WAIT_TIME);
			}
			//Pick item or skip Housekeeping instruction
			picker.pickItemAuto();
		}
		
		//Finish test by logging out
		picker.logout();
		picker.waitForCheState(CheStateEnum.IDLE, WAIT_TIME);
	}
}