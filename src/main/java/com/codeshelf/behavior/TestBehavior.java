package com.codeshelf.behavior;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.model.DomainObjectManager;
import com.codeshelf.model.domain.Facility;
import com.codeshelf.model.domain.OrderHeader;

public class TestBehavior {

	private static final Logger	LOGGER = LoggerFactory.getLogger(TestBehavior.class);

	/**
	 * This takes a string in that is really a list of parameters
	 * "24 CHE1, CHE2, CHE3, CHE4"
	 * And produces an output string that is really separate usable script lines
	 * "setupCart CHE1 26768709 26768711 26768712 26768717 etc. /n
	 * setupCart CHE2 26768718 26768719 26768720 etc."
	 * 
	 * It queries for uncompleted orders in the current facility and distributes them among the CHEs
	 * It does not (currently) validate that the CHE names are valid
	 */
	public String setupManyCartsWithOrders(Facility inFacility, Map<String, String> input) {
		String returnStr = "";

		String countStr = input.get("ordersPerChe");
		int ordersPerChe = 0;
		try {
			ordersPerChe = Integer.valueOf(countStr);
		} catch (NumberFormatException e) {
			// ordersPerChe will be 0, which logs the error
		}
		if (ordersPerChe < 1 || ordersPerChe > 100) {
			LOGGER.error("bad count parameter for setupManyCartsWithOrders oredersPerChe: {}", countStr);
			return returnStr;
		}

		String ches = input.get("ches");
		// 1: parse the tokens
		String delims = "[ ]+";
		String[] cheNames = ches.split(delims);
		int numberOfChe = cheNames.length;
		if (numberOfChe < 1) {
			LOGGER.error("bad parameter for setupManyCartsWithOrders ches parameter: {}", ches);
			return returnStr;
		}
		
		
		int ordersNeeded = ordersPerChe * numberOfChe;
		DomainObjectManager doMananager = new DomainObjectManager(inFacility);
		List<OrderHeader> orders = doMananager.getSomeUncompletedOrders(ordersNeeded);
		int ordersRetrieved = orders.size();
		int ordersUsed = 0;
		for (int cheIndex = 0; cheIndex <= numberOfChe; cheIndex++) {
			if (ordersUsed < ordersRetrieved) {
				// start a new line, unless it is the first one
				if (ordersUsed > 0)
					returnStr += "\n";
				returnStr += "loginSetup " + cheNames[cheIndex] + " " + cheNames[cheIndex] + "\n";
				returnStr += "setupCart " + cheNames[cheIndex] + " "; 
			}
			for (int orderIndex = 1; orderIndex <= ordersPerChe; orderIndex++) {
				// add orders, unless we have run out
				if (ordersUsed < ordersRetrieved) {
					OrderHeader oh = orders.get(ordersUsed);
					returnStr += oh.getOrderId() + " "; // add orderId(space)
					ordersUsed++;
				}
			}
		}

		LOGGER.info("setupManyCartsWithOrders called with: {}", input);
		LOGGER.info("setupManyCartsWithOrders is returning the following line(s)");
		LOGGER.info(returnStr);
		return returnStr;
	}
}