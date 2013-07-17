/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: DdcInventoryCsvImportBean.java,v 1.5 2013/07/17 05:48:13 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.edi;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import lombok.Data;
import lombok.ToString;

import com.avaje.ebean.validation.NotNull;

/**
 * @author jeffw
 *
 */
@ToString
@Data
public class DdcInventoryCsvImportBean {

	private static final Logger		LOGGER	= LoggerFactory.getLogger(DdcInventoryCsvImportBean.class);

	@NotNull
	private String	itemId;
	@NotNull
	private String	itemDetailId;
	private String	description;
	@NotNull
	private String	ddcId;
	@NotNull
	private String	quantity;
	@NotNull
	private String	uom;
	
	// --------------------------------------------------------------------------
	/**
	 * Validate the bean and return a status message is it's not valid.
	 * @return an error message or NULL.
	 */
	public final String validateBean() {
		String result = null;
		
		// Iterate over all of the fields and see which are required, but have no value.
		for (Field field : DdcInventoryCsvImportBean.class.getDeclaredFields()) {
			Annotation annotation = field.getAnnotation(NotNull.class);
			try {
				Object fieldValue = field.get(this);
				if ((annotation != null) && (fieldValue == null)) {
					if (result == null) {
						result = "";
					}
					result = result + "field " + field.getName() + " is null, but is required";
				}
			} catch (IllegalArgumentException | IllegalAccessException e) {
				LOGGER.error("", e);
			}
		}
		
		return result;
	}
}
