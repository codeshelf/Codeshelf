/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: IEdiProcessor.java,v 1.3 2013/03/19 01:19:59 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.edi;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.avaje.ebean.validation.NotNull;

/**
 * @author jeffw
 *
 */
public abstract class CsvImportBeanABC {

	private static final Logger	LOGGER	= LoggerFactory.getLogger(CsvImportBeanABC.class);

	public CsvImportBeanABC() {
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

}