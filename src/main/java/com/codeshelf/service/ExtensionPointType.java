package com.codeshelf.service;

import java.io.IOException;
import java.io.InputStream;

import lombok.Getter;

import org.apache.commons.io.IOUtils;

/**
 * Example scripts are read from the src/main/resources/com/codeshelf/service for use as default examples.
 * 
 * @author pmonteiro
 *
 */
public enum ExtensionPointType {
	
	OrderImportBeanTransformation,
	OrderImportHeaderTransformation,
	OrderImportCreateHeader,
	OrderImportLineTransformation,
	WorkInstructionExportContent,
	WorkInstructionExportCreateHeader,
	WorkInstructionExportCreateTrailer,
	WorkInstructionExportLineTransformation;

	/**
	 * Example script associated with the type 
	 */
	@Getter
	private String exampleScript;
	
	private ExtensionPointType() {
		String resourceName = this.name() + ".groovy.example";
		try (InputStream resource = this.getClass().getResourceAsStream(resourceName)) {
			if (resource != null) { 
				this.exampleScript = IOUtils.toString(resource, "UTF-8");
			} else {
				throw new RuntimeException("Unable to load example script for the ExtensionPointType at " + resourceName);
			}
		} catch (IOException e) {
			throw new RuntimeException("Unable to load example script for the ExtensionPointType at " + resourceName, e);
		}
	}
}
