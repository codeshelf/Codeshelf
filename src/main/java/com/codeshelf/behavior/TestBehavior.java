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
		StringBuilder loginCommands = new StringBuilder(), setupCommands = new StringBuilder();

		String countStr = input.get("ordersPerChe");
		int ordersPerChe = 0;
		try {
			ordersPerChe = Integer.valueOf(countStr);
		} catch (NumberFormatException e) {
			// ordersPerChe will be 0, which logs the error
		}
		if (ordersPerChe < 1 || ordersPerChe > 100) {
			throw new IllegalArgumentException(String.format("bad count parameter for setupManyCartsWithOrders ordersPerChe: %s", countStr));
		}

		String ches = input.get("ches");
		// 1: parse the tokens
		String delims = "[ ]+";
		String[] cheNames = ches.split(delims);
		int numberOfChe = cheNames.length;
		if (numberOfChe < 1) {
			throw new IllegalArgumentException(String.format("bad count parameter for setupManyCartsWithOrders ches: %s", ches));
		}
		
		
		int ordersNeeded = ordersPerChe * numberOfChe;
		DomainObjectManager doMananager = new DomainObjectManager(inFacility);
		List<OrderHeader> orders = doMananager.getSomeUncompletedOrders(ordersNeeded);
		int ordersRetrieved = orders.size();
		int ordersUsed = 0;
		for (int cheIndex = 0; cheIndex < numberOfChe; cheIndex++) {
			if (ordersUsed < ordersRetrieved) {
				loginCommands.append(String.format("loginSetup %s %s\n", cheNames[cheIndex], cheNames[cheIndex]));
				setupCommands.append(String.format("setupCart %s ", cheNames[cheIndex]));
			}
			for (int orderIndex = 1; orderIndex <= ordersPerChe; orderIndex++) {
				// add orders, unless we have run out
				if (ordersUsed < ordersRetrieved) {
					OrderHeader oh = orders.get(ordersUsed);
					ordersUsed++;
					setupCommands.append(oh.getOrderId()).append(" ");
				}
			}
			setupCommands.append("\n");
		}

		LOGGER.info("setupManyCartsWithOrders called with: {}", input);
		LOGGER.info("setupManyCartsWithOrders is returning the following line(s)");
		String returnStr = loginCommands.toString() + "setupMany start\n" + setupCommands.toString() + "setupMany stop\n";
		LOGGER.info(returnStr);
		return returnStr;
	}
}
