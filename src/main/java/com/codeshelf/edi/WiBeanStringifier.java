/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2015, Codeshelf, All rights reserved
 *  Author Jon Ranstrom
 *  
 *******************************************************************************/
package com.codeshelf.edi;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.List;

import lombok.Getter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.model.domain.Che;
import com.codeshelf.model.domain.OrderHeader;
import com.codeshelf.model.domain.WorkInstruction;
import com.codeshelf.model.domain.Worker;
import com.codeshelf.service.ExtensionPointService;
import com.codeshelf.service.ExtensionPointType;
import com.google.common.collect.ImmutableList;
import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;
import com.opencsv.bean.HeaderColumnNameMappingStrategy;

/**
 * Built first for PFSWeb, this accumulates complete work instruction beans for sending later as small files organized by order.
 * Currently only a memory list, lost upon server restart.
 * Later, change to a persistent list of the serialized bean to survive server restart.
 */
public class WiBeanStringifier {
	private static final String NEWLINE = "\r\n";
	
	@Getter
	private ExtensionPointService				extensionPointService;

	private static final Logger			LOGGER		= LoggerFactory.getLogger(WiBeanStringifier.class);

	public WiBeanStringifier() {
	}

	public WiBeanStringifier(ExtensionPointService inExtensionPointService) {
		extensionPointService = inExtensionPointService;
		if (extensionPointService == null) {
			LOGGER.info("null extension service passed to WiBeanStringifier");
		}
	}
	
	@SuppressWarnings({ "deprecation" })
	public String stringifyWorkInstruction(WorkInstruction inWorkInstructions) throws IOException {
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
		
		WorkInstructionCsvBean bean = new WorkInstructionCsvBean(inWorkInstructions);
		try {
			CsvExporter<WorkInstructionCsvBean> exporter = new CsvExporter<WorkInstructionCsvBean>();
			exporter.setBeanList(ImmutableList.of(bean));
			exporter.setStrategy(strategy);
			exporter.writeRecords(csvWriter);
			
		} catch (RuntimeException e) {
			csvWriter.close();
			throw new RuntimeException("writing records: ", e);
		}
		
		csvWriter.close();
		return stringWriter.toString();

	}

	public String stringifyOrderOnCartAdded(OrderHeader order, Che che) {

		if (!hasExtensionPoint(ExtensionPointType.OrderOnCartContent)) {
			return "";
		}
		String content = "";
		OrderAndCheBean bean = new OrderAndCheBean(order, che);
		Object[] params = {bean};
		try {
			content = (String) getExtensionPointService().eval(ExtensionPointType.OrderOnCartContent, params);
		} catch (Exception e) {
			LOGGER.error("Failed to evaluate OrderOnCartContent extension point", e);
		}

		return content;
	}

	public String stringifyOrderOnCartFinished(OrderHeader inOrder, Che inChe, List<WorkInstructionCsvBean> inWiBeanList) {

		if (inWiBeanList.isEmpty()) {
			LOGGER.error("Nothing in bean list for WiBeanStringifier.stringifyOrderComplete()");
			return "";
		}

		boolean needContentExtension = false;
		ExtensionPointService groovyService = getExtensionPointService();
		if (hasExtensionPoint(ExtensionPointType.WorkInstructionExportContent))
			needContentExtension = true;

		// Get header, trailer.
		String returnStr = "";
		String header = getWiHeader(inOrder, inChe);
		String trailer = getWiTrailer(inOrder, inChe);
		if (header != null && !header.isEmpty())
			returnStr += header + NEWLINE;

		// contents.. Add the new line
		for (WorkInstructionCsvBean wiBean : inWiBeanList) {
			if (needContentExtension) {
				returnStr += getWiCustomContent(groovyService, wiBean);
			} else {
				returnStr += wiBean.getDefaultCsvContent();
			}
			returnStr += NEWLINE;
		}

		if (trailer != null && !trailer.isEmpty())
			returnStr += trailer + NEWLINE;
		return returnStr;
	}

	/**
	 * Our default header is all the fields of the our native export bean.
	 * But groovy extension may override. If override is null or empty, we do not add to the output file
	 */
	private String getWiHeader(OrderHeader inOrder, Che inChe) {
		if (hasExtensionPoint(ExtensionPointType.WorkInstructionExportCreateHeader)) {
			OrderAndCheBean bean = new OrderAndCheBean(inOrder, inChe);
			Object[] params = {bean};
			String header = "";
			try {
				header = (String) getExtensionPointService().eval(ExtensionPointType.WorkInstructionExportCreateHeader, params);
			} catch (Exception e) {
				LOGGER.error("Failed to evaluate WorkInstructionExportCreateHeader extension point", e);
			}
			return header;
		} else {
			return WorkInstructionCsvBean.getCsvHeaderMatchingBean();
		}
	}

	/**
	 * Our default trailer is null.
	 * But groovy extension may override. If trailer is null or empty, we do not add to the output file
	 */
	private String getWiTrailer(OrderHeader inOrder, Che inChe) {
		if (hasExtensionPoint(ExtensionPointType.WorkInstructionExportCreateTrailer)) {
			OrderAndCheBean bean = new OrderAndCheBean(inOrder, inChe);
			Object[] params = {bean};
			String header = "";
			try {
				header = (String) getExtensionPointService().eval(ExtensionPointType.WorkInstructionExportCreateTrailer, params);
			} catch (Exception e) {
				LOGGER.error("Failed to evaluate WorkInstructionExportCreateTrailer extension point", e);
			}
			return header;
		} else {
			return "";
		}
	}

	/**
	 * Remember, there might be no header. Or the header may not at all specify the order of field in content lines.
	 * We need a easily understood (brain-dead) way for groovy extension to write out lines using data from our bean.
	 */
	private String getWiCustomContent(ExtensionPointService inServiceType, WorkInstructionCsvBean inWiBean) {
		String content = "";
		Object[] params = { inWiBean };
		try {
			content = (String) getExtensionPointService().eval(ExtensionPointType.WorkInstructionExportContent, params);
		} catch (Exception e) {
			LOGGER.error("Failed to evaluate WorkInstructionExportContent extension point", e);
		}

		return content;
	}

	
	private boolean hasExtensionPoint(ExtensionPointType type) {
		ExtensionPointService extensionService = getExtensionPointService();
		if (extensionService  == null)
			LOGGER.error("null extension point service when checking for: " + type);
		return (extensionService != null && extensionService.hasExtensionPoint(type));
	}
	
	@SuppressWarnings("unused")
	private class OrderAndCheBean{
		private String orderId;
		private String customerId;
		private String cheId;
		private String worker;
		
		public OrderAndCheBean(OrderHeader order, Che che) {
			orderId = order.getOrderId();
			customerId = order.getCustomerId();
			if (customerId == null){
				customerId = "";
			}
			cheId = che.getDomainId();
			Worker workerObj = che.getWorker();
			worker = workerObj == null ? "" : workerObj.getDomainId();
		}
	}
}
