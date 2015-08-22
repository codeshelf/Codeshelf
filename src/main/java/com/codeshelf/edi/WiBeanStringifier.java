/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2015, Codeshelf, All rights reserved
 *  Author Jon Ranstrom
 *  
 *******************************************************************************/
package com.codeshelf.edi;

import groovy.lang.GroovyRuntimeException;

import java.util.ArrayList;

import lombok.Getter;
import lombok.Setter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.model.domain.Che;
import com.codeshelf.model.domain.OrderHeader;
import com.codeshelf.service.ExtensionPointService;
import com.codeshelf.service.ExtensionPointType;

/**
 * Built first for PFSWeb, this accumulates complete work instruction beans for sending later as small files organized by order.
 * Currently only a memory list, lost upon server restart.
 * Later, change to a persistent list of the serialized bean to survive server restart.
 */
public class WiBeanStringifier {
	@Getter
	ArrayList<WorkInstructionCsvBean>	wiBeanList	= new ArrayList<WorkInstructionCsvBean>();
	@Getter
	ExtensionPointService				extensionPointService;
	@Getter
	Che									che;
	@Getter
	OrderHeader							order;
	@Getter
	@Setter
	boolean								notifyingOrderOnCart;

	private static final Logger			LOGGER		= LoggerFactory.getLogger(WiBeanStringifier.class);

	public WiBeanStringifier() {
	}

	public WiBeanStringifier(OrderHeader inOrder,
		Che inChe,
		ArrayList<WorkInstructionCsvBean> inWiBeanList,
		ExtensionPointService inExtensionPointService) {
		extensionPointService = inExtensionPointService;
		wiBeanList = inWiBeanList;
		che = inChe;
		order = inOrder;
		// Some diagnostics
		if (che == null || order == null) {
			LOGGER.info("null objects passed to WiBeanStringifier");
		}
		if (extensionPointService == null) {
			LOGGER.info("null extension service passed to WiBeanStringifier");
		}
		this.setNotifyingOrderOnCart(false);
	}

	public String stringify() {
		try {
		if (isNotifyingOrderOnCart()) {
			return stringifyOrderOnCart();
		} else {
			return stringifyOrderComplete();
		}
		}
		catch (GroovyRuntimeException e) {
			LOGGER.warn("Groovy error in WiBeanStringifier: {}", e);
			return "";
		}
	}

	public String stringifyOrderOnCart() {

		if (!hasExtensionPoint(ExtensionPointType.OrderOnCartContent)) {
			return "";
		}
		OrderHeader order = getOrder();
		String orderId = order.getOrderId();
		String customerId = order.getCustomerId();
		if (customerId == null)
				customerId = "";
		String cheId = getChe().getDomainId();
		String content = "";
		Object[] params = { orderId, cheId, customerId};
		try {
			content = (String) getExtensionPointService().eval(ExtensionPointType.OrderOnCartContent, params);
		} catch (Exception e) {
			LOGGER.error("Failed to evaluate OrderOnCartContent extension point", e);
		}

		return content;
	}

	public String stringifyOrderComplete() {

		if (wiBeanList.isEmpty()) {
			LOGGER.error("Nothing in bean list for WiBeanStringifier.stringifyOrderComplete()");
			return "";
		}

		boolean needContentExtension = false;
		ExtensionPointService groovyService = getExtensionPointService();
		if (hasExtensionPoint(ExtensionPointType.WorkInstructionExportContent))
			needContentExtension = true;

		// Get header, trailer.
		String returnStr = "";
		String header = getWiHeader();
		String trailer = getWiTrailer();
		if (header != null && !header.isEmpty())
			returnStr += header + "\n";

		// contents.. Add the new line
		for (WorkInstructionCsvBean wiBean : wiBeanList) {
			if (needContentExtension) {
				returnStr += getWiCustomContent(groovyService, wiBean);
			} else {
				returnStr += wiBean.getDefaultCsvContent();
			}
			returnStr += "\n";
		}

		if (trailer != null && !trailer.isEmpty())
			returnStr += trailer + "\n";
		return returnStr;
	}

	/**
	 * Our default header is all the fields of the our native export bean.
	 * But groovy extension may override. If override is null or empty, we do not add to the output file
	 */
	private String getWiHeader() {
		if (hasExtensionPoint(ExtensionPointType.WorkInstructionExportCreateHeader)) {
			String theOrderId = getOrder().getOrderId();
			String theCheId = getChe().getDomainId();
			Object[] params = { theOrderId, theCheId };
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
	private String getWiTrailer() {
		if (hasExtensionPoint(ExtensionPointType.WorkInstructionExportCreateTrailer)) {
			String theOrderId = getOrder().getOrderId();
			String theCheId = getChe().getDomainId();
			Object[] params = { theOrderId, theCheId };
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
}
