package com.gadgetworks.codeshelf.edi;

import java.io.IOException;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.List;

import au.com.bytecode.opencsv.CSVWriter;

import com.gadgetworks.codeshelf.model.domain.LocationAlias;
import com.gadgetworks.codeshelf.model.domain.OrderGroup;
import com.gadgetworks.codeshelf.model.domain.WorkInstruction;

public class WorkInstructionCSVExporter {

	private static final String     TIME_FORMAT			= "yyyy-MM-dd'T'HH:mm:ss'Z'";

	private static final Integer	DOMAINID_POS		= 0;
	private static final Integer	TYPE_POS			= 1;
	private static final Integer	STATUS_POS			= 2;
	private static final Integer	ORDERGROUPID_POS	= 3;
	private static final Integer	ORDERID_POS			= 4;
	private static final Integer	CONTAINERID_POS		= 5;
	private static final Integer	ITEMID_POS			= 6;
	private static final Integer	LOCATIONID_POS		= 7;
	private static final Integer	PICKERID_POS		= 8;
	private static final Integer	PLAN_QTY_POS		= 9;
	private static final Integer	ACT_QTY_POS			= 10;
	private static final Integer	ASSIGNED_POS		= 11;
	private static final Integer	STARTED_POS			= 12;
	private static final Integer	COMPLETED_POS		= 13;
	private static final Integer	WI_ATTR_COUNT		= 14;											// The total count of these attributes.
	
	public String exportWorkInstructions(List<WorkInstruction> inWorkInstructions) throws IOException {
		// Convert the WI into a CSV string.
		StringWriter stringWriter = new StringWriter();
		CSVWriter csvWriter = new CSVWriter(stringWriter);
		String[] properties = new String[WI_ATTR_COUNT];
		properties[DOMAINID_POS] = "domainId";
		properties[TYPE_POS] = "type";
		properties[STATUS_POS] = "status";
		properties[ORDERGROUPID_POS] = "orderGroupId";
		properties[ORDERID_POS] = "orderId";
		properties[CONTAINERID_POS] = "containerId";
		properties[ITEMID_POS] = "itemId";
		properties[LOCATIONID_POS] = "locationId";
		properties[PICKERID_POS] = "pickerId";
		properties[PLAN_QTY_POS] = "planQuantity";
		properties[ACT_QTY_POS] = "actualQuantity";
		properties[ASSIGNED_POS] = "assigned";
		properties[STARTED_POS] = "started";
		properties[COMPLETED_POS] = "completed";
		csvWriter.writeNext(properties);

		for (WorkInstruction wi : inWorkInstructions) {

			properties = new String[WI_ATTR_COUNT];
			properties[DOMAINID_POS] = wi.getDomainId();
			properties[TYPE_POS] = wi.getTypeEnum().toString();
			properties[STATUS_POS] = wi.getStatusEnum().toString();

			// groups are optional!
			String groupStr = "";
			OrderGroup theGroup = wi.getParent().getParent().getOrderGroup();
			if (theGroup != null)
				groupStr = theGroup.getDomainId();
			properties[ORDERGROUPID_POS] = groupStr;

			properties[ORDERID_POS] = wi.getParent().getOrderId();
			properties[CONTAINERID_POS] = wi.getContainerId();
			properties[ITEMID_POS] = wi.getItemId();

			//Default to location id
			properties[LOCATIONID_POS] = wi.getLocationId();
			//if there is alias use instead
			if (wi.getLocation().getAliases().size() > 0) {
				LocationAlias locAlias = wi.getLocation().getAliases().get(0);
				properties[LOCATIONID_POS] = locAlias.getDomainId();
			}
			
			properties[PICKERID_POS] = wi.getPickerId();
			properties[PLAN_QTY_POS] = Integer.toString(wi.getPlanQuantity());
			properties[ACT_QTY_POS] = Integer.toString(wi.getActualQuantity());
			properties[ASSIGNED_POS] = new SimpleDateFormat(TIME_FORMAT).format(wi.getAssigned());
			properties[STARTED_POS] = new SimpleDateFormat(TIME_FORMAT).format(wi.getStarted());
			properties[COMPLETED_POS] = new SimpleDateFormat(TIME_FORMAT).format(wi.getCompleted());
			csvWriter.writeNext(properties);
		}
		csvWriter.close();
		return stringWriter.toString();
	}
}
