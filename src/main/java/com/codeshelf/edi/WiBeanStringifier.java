/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2015, Codeshelf, All rights reserved
 *  Author Jon Ranstrom
 *  
 *******************************************************************************/
package com.codeshelf.edi;

import java.util.ArrayList;

import lombok.Getter;

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
	}

	public String stringify() {

		if (!wiBeanList.isEmpty() && getExtensionPointService() != null) {
			// transform order bean with groovy script, if enabled
			if (getExtensionPointService().hasExtensionPoint(ExtensionPointType.WorkInstructionExportBeanTransformation)) {
				// Transform the content of our beans
				for (WorkInstructionCsvBean wiBean : wiBeanList) {
					Object[] params = { wiBean };
					try {
						wiBean = (WorkInstructionCsvBean) getExtensionPointService().eval(ExtensionPointType.WorkInstructionExportBeanTransformation,
							params);
					} catch (Exception e) {
						LOGGER.error("Failed to evaluate WorkInstructionExportBeanTransformation extension point", e);
					}
				}
			}
		}
		// Now we have the list of transformed beans, or original if no WorkInstructionExportBeanTransformation.

		return "";
	}

}
