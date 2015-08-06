/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2015, Codeshelf, All rights reserved
 *  Author Jon Ranstrom
 *  
 *******************************************************************************/
package com.codeshelf.edi;


import java.sql.Timestamp;
import java.text.SimpleDateFormat;

import lombok.Getter;
import lombok.Setter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author jonranstrom
 *
 */
public abstract class ExportCsvBeanABC {

	protected final SimpleDateFormat timestampFormatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
	
	@SuppressWarnings("unused")
	private static final Logger	LOGGER	= LoggerFactory.getLogger(ExportCsvBeanABC.class);
	
	@Setter @Getter
	private Integer 	lineNumber;

	public ExportCsvBeanABC() {
		super();
	}
	
	protected String formatDate(Timestamp time) {
		if (time == null) {
			return "";
		} else {
			synchronized(timestampFormatter) {
				return timestampFormatter.format(time);
			}
		}
	}


}
