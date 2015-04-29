import com.codeshelf.sim.worker.PickSimulator;


class UPCVerifiedPick {
	
	def PickSimulator pickSim
	
	def UPCVerifiedPick(pickSim) {
		this.pickSim = pickSim
		
	}
	
	def skipAll(orderId) {
		def pos = 1
		pickSim.setupOrderIdAsContainer(orderId, String.valueOf(pos))	
		pickSim.scanCommand("START");
		pickSim.scanCommand("START");
		if(pickSim.activePick.isHousekeeping()) {
			pickSim.buttonPress(pos, 0)
		} else {
			pickSim.scanCommand("SKIPSCAN");
		}	
		pickSim.buttonPress(pos, 
	}
}