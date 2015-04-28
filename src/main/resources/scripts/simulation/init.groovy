//deviceManager default binding

import com.codeshelf.sim.worker.PickSimulator
import org.slf4j.Logger;
import org.slf4j.LoggerFactory; 

def LOGGER	= LoggerFactory.getLogger("scripting");

def sim(deviceManager, cheGuid) {
	return new PickSimulator(deviceManager, cheGuid);
}
println "init simulation script loaded"