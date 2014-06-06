/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: IEdiProcessor.java,v 1.3 2013/03/19 01:19:59 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.edi;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;

import javax.validation.constraints.NotNull;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author jeffw
 *
 */
public abstract class ImportCsvBeanABC {

	private static final Logger	LOGGER	= LoggerFactory.getLogger(ImportCsvBeanABC.class);

	public ImportCsvBeanABC() {
		super();
	}

	/**
	 * Validate the bean and return a status message is it's not valid.
	 * @return an error message or NULL.
	 */
	public final String validateBean() {
		String result = null;

		// Iterate over all of the fields and see which are required, but have no value.
		for (Field field : this.getClass().getDeclaredFields()) {
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

	// --------------------------------------------------------------------------
	/**
	 * If it's not null then strip off any leading/trailing spaces.
	 * @param inValue
	 * @return
	 */
	protected String strip(String inValue) {
		if (inValue == null) {
			return null;
		} else {
			return StringUtils.strip(inValue);
		}
	}
	
	// --------------------------------------------------------------------------
	/**
	 * Same as strip, but guaranteed to return a string. Empty string if null passed in.
	 * @param inValue
	 * @return
	 */
	protected String stripNull(String inValue) {
		if (inValue == null) {
			return "";
		} else {
			return strip(inValue);
		}
	}

}
