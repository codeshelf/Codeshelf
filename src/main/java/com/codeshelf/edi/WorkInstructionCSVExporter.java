package com.codeshelf.edi;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import com.codeshelf.model.domain.LocationAlias;
import com.codeshelf.model.domain.OrderGroup;
import com.codeshelf.model.domain.WorkInstruction;
import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;
import com.opencsv.bean.HeaderColumnNameMappingStrategy;

public class WorkInstructionCSVExporter {

	private final SimpleDateFormat timestampFormatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
	private static final String CURRENT_VERSION = "1.0";
	
	private static final Integer	FACILITYID_POS			= 0;
	private static final Integer	WORKINSTRUCTIONID_POS	= 1;
	private static final Integer	TYPE_POS				= 2;
	private static final Integer	STATUS_POS				= 3;
	private static final Integer	ORDERGROUPID_POS		= 4;
	private static final Integer	ORDERID_POS				= 5;
	private static final Integer	CONTAINERID_POS			= 6;
	private static final Integer	ITEMID_POS				= 7;
	private static final Integer	UOM_POS					= 8;
	private static final Integer	LOTID_POS				= 9;
	private static final Integer	LOCATIONID_POS			= 10;
	private static final Integer	PICKERID_POS			= 11;
	private static final Integer	PLAN_QTY_POS			= 12;
	private static final Integer	ACT_QTY_POS				= 13;
	private static final Integer	CHEID_POS				= 14;
	private static final Integer	ASSIGNED_POS			= 15;
	private static final Integer	STARTED_POS				= 16;
	private static final Integer	COMPLETED_POS			= 17;
	private static final Integer	VERSION_POS				= 18;
	private static final Integer	WI_ATTR_COUNT			= 19;							// The total count of these attributes.

	public String exportWorkInstructions(List<WorkInstruction> inWorkInstructions) throws IOException {
		// Convert the WI into a CSV string.
		StringWriter stringWriter = new StringWriter();
		CSVWriter csvWriter = new CSVWriter(stringWriter);
		String[] properties = new String[WI_ATTR_COUNT];
		properties[FACILITYID_POS] = "facilityId";
		properties[WORKINSTRUCTIONID_POS] = "workInstructionId";
		properties[TYPE_POS] = "type";
		properties[STATUS_POS] = "status";
		properties[ORDERGROUPID_POS] = "orderGroupId";
		properties[ORDERID_POS] = "orderId";
		properties[CONTAINERID_POS] = "containerId";
		properties[ITEMID_POS] = "itemId";
		properties[UOM_POS] = "uom";
		properties[LOTID_POS] = "lotId";
		properties[CHEID_POS] = "cheId";
		properties[LOCATIONID_POS] = "locationId";
		properties[PICKERID_POS] = "pickerId";
		properties[PLAN_QTY_POS] = "planQuantity";
		properties[ACT_QTY_POS] = "actualQuantity";
		properties[ASSIGNED_POS] = "assigned";
		properties[STARTED_POS] = "started";
		properties[COMPLETED_POS] = "completed";
		properties[VERSION_POS] = "version-" + CURRENT_VERSION;
		csvWriter.writeNext(properties);

		for (WorkInstruction wi : inWorkInstructions) {

			properties = new String[WI_ATTR_COUNT];
			properties[FACILITYID_POS] = wi.getParent().getDomainId();
			properties[WORKINSTRUCTIONID_POS] = wi.getDomainId();
			properties[TYPE_POS] = wi.getType().toString();
			properties[STATUS_POS] = wi.getStatus().toString();

			// groups are optional!
			String groupStr = "";
			// from v5, housekeeping wi may have no detail
			if (wi.getOrderDetail() != null) {
				OrderGroup theGroup = wi.getOrderDetail().getParent().getOrderGroup();
				if (theGroup != null)
					groupStr = theGroup.getDomainId();
			}
			properties[ORDERGROUPID_POS] = groupStr;

			// from v5, housekeeping wi may have no detail
			String orderStr = "";
			if (wi.getOrderDetail() != null) 
				orderStr = wi.getOrderDetail().getOrderId();
			properties[ORDERID_POS] = orderStr;
			
			properties[CONTAINERID_POS] = wi.getContainerId();
			properties[ITEMID_POS] = wi.getItemId();
			properties[UOM_POS] = wi.getUomMasterId();
			properties[LOTID_POS] = "";
			properties[ITEMID_POS] = wi.getItemId();

			//Default to location id
			properties[LOCATIONID_POS] = wi.getLocationId();
			//if there is alias use instead
			if (wi.getLocation().getAliases().size() > 0) {
				LocationAlias locAlias = wi.getLocation().getAliases().get(0);
				properties[LOCATIONID_POS] = locAlias.getDomainId();
			}

			properties[PICKERID_POS] = wi.getPickerId();

			if (wi.getPlanQuantity() != null) {
				properties[PLAN_QTY_POS] = String.valueOf(wi.getPlanQuantity());
			}
			if (wi.getActualQuantity() != null) {
				properties[ACT_QTY_POS] = String.valueOf(wi.getActualQuantity());
			}

			properties[CHEID_POS] = wi.getAssignedCheName();
			properties[ASSIGNED_POS] = formatDate(wi.getAssigned());
			properties[STARTED_POS] = formatDate(wi.getStarted());
			properties[COMPLETED_POS] = formatDate(wi.getCompleted());
			properties[VERSION_POS] = ""; //always blank in the data
			csvWriter.writeNext(properties);
		}
		csvWriter.close();
		return stringWriter.toString();
	}
	
	/**
	 * Redone using an export bean
	 */
	public String exportWorkInstructions2(List<WorkInstruction> inWorkInstructions) throws IOException {
		// Convert the WI into a CSV string.
		StringWriter stringWriter = new StringWriter();
		CSVWriter csvWriter = new CSVWriter(stringWriter);
		
		// Set up the mapper
		// We are doing this via an import match the order shown
		String headerString =WorkInstructionCsvBean.getCsvHeaderMatchingBean();
		/*
		String headerString = "facilityId, workInstructionId, type, status, orderGroupId, orderId, containerId,"
				+ "itemId, uom, lotId, locationId, pickerId, planQuantity, actualQuantity, cheId,"
				+ "assigned, started, completed"; // no version here
		

		private static final Integer	FACILITYID_POS			= 0;
		private static final Integer	WORKINSTRUCTIONID_POS	= 1;
		private static final Integer	TYPE_POS				= 2;
		private static final Integer	STATUS_POS				= 3;
		private static final Integer	ORDERGROUPID_POS		= 4;
		private static final Integer	ORDERID_POS				= 5;
		private static final Integer	CONTAINERID_POS			= 6;
		private static final Integer	ITEMID_POS				= 7;
		private static final Integer	UOM_POS					= 8;
		private static final Integer	LOTID_POS				= 9;
		private static final Integer	LOCATIONID_POS			= 10;
		private static final Integer	PICKERID_POS			= 11;
		private static final Integer	PLAN_QTY_POS			= 12;
		private static final Integer	ACT_QTY_POS				= 13;
		private static final Integer	CHEID_POS				= 14;
		private static final Integer	ASSIGNED_POS			= 15;
		private static final Integer	STARTED_POS				= 16;
		private static final Integer	COMPLETED_POS			= 17;
		private static final Integer	VERSION_POS				= 18;
		 */
		
		
		HeaderColumnNameMappingStrategy<WorkInstructionCsvBean> strategy = null;
		try  {
			StringReader reader = new StringReader(headerString);
			CSVReader csvReader = new CSVReader(reader);
			strategy = new HeaderColumnNameMappingStrategy<WorkInstructionCsvBean>();
			strategy.setType(WorkInstructionCsvBean.class);
			strategy.captureHeader(csvReader);

		} catch (IOException e) {
			csvWriter.close();
			throw new RuntimeException("create mapping strategy: ", e);
		}
		
		// This process writes out header and the contents of each bean to match.
		// It relies on reflection, then calling the getter for each name in the mapping strategy.
		// This might be good for serializing out work instruction beans to persist in case the server restarts.
		ArrayList<WorkInstructionCsvBean> beanList = new ArrayList<WorkInstructionCsvBean>();
		for (WorkInstruction wi : inWorkInstructions) {
			WorkInstructionCsvBean bean = new WorkInstructionCsvBean(wi);
			beanList.add(bean);
		}
		try {
			CsvExporter<WorkInstructionCsvBean> exporter = new CsvExporter<WorkInstructionCsvBean>();
			exporter.setBeanList(beanList);
			exporter.setStrategy(strategy);
			exporter.writeRecords(csvWriter);
			
		} catch (RuntimeException e) {
			csvWriter.close();
			throw new RuntimeException("writing records: ", e);
		}
		
		csvWriter.close();
		return stringWriter.toString();

	}

	private String formatDate(Timestamp time) {
		if (time == null) {
			return "";
		} else {
			synchronized(timestampFormatter) {
				return timestampFormatter.format(time);
			}
		}
	}
}
