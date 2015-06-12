/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: IEdiProcessor.java,v 1.3 2013/03/19 01:19:59 jeffw Exp $
 *******************************************************************************/
package com.codeshelf.edi;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import lombok.Getter;
import lombok.Setter;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author jeffw
 *
 */
public abstract class ImportCsvBeanABC {

	private static final Logger	LOGGER	= LoggerFactory.getLogger(ImportCsvBeanABC.class);
	
	@Setter @Getter
	private Integer 	lineNumber;

	public ImportCsvBeanABC() {
		super();
	}

	/**
	 * Validate the bean and return a status message is it's not valid.
	 * @return an error message or NULL.
	 */
	public final String validateBean() {
		StringBuilder result = new StringBuilder("Errors on line " + lineNumber + ": \n");
		int badCount = 0;
		
		// Iterate over all of the fields and see which are required, but have no value.
		for (Field field : this.getClass().getDeclaredFields()) {
			try {
				Object fieldValue = field.get(this);
				String fieldName = field.getName();
				
				//Check for NULL values
				Annotation nullAnnotation = field.getAnnotation(NotNull.class);
				if ((nullAnnotation != null) && (fieldValue == null)) {
					result.append(String.format("Required field '%s' is null\n", fieldName));
					badCount++;
				}
				
				//Check for too short (at this time - empty) values
				Size sizeAnnotation = field.getAnnotation(Size.class);
				if (sizeAnnotation != null) {
					int min = sizeAnnotation.min();
					if (min > 0) {
						if (fieldValue == null) {
							result.append(String.format("Required field '%s' is null\n", fieldName));
							badCount++;
						} else {
							int fieldLen = fieldValue.toString().length();
							if (fieldLen == 0){
								result.append(String.format("Required field '%s' is empty\n", fieldName));
								badCount++;
							} else if (fieldLen < min) {
								result.append(String.format("Field '%s' is size %d, but at least %d is required.\n", fieldName, fieldLen, min));
								badCount++;					
							}
						}
					}
				}
			} catch (IllegalArgumentException | IllegalAccessException e) {
				LOGGER.error("validateBean", e);
			}
			// If a record is horribly blank, stop the verbosity after 3 fields are named
			if (badCount >= 3) {
				break;
			}
		}

		return badCount > 0 ? result.toString() : null;
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
