package com.codeshelf.service;

import java.io.IOException;
import java.io.InputStream;

import lombok.Getter;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.model.domain.OrderHeader;

/**
 * Example scripts are read from the src/main/resources/com/codeshelf/service for use as default examples.
 * 
 * @author jon ranstrom
 *
 */
public enum ExtensionPointType {

	OrderImportBeanTransformation,
	OrderImportHeaderTransformation,
	OrderImportCreateHeader,
	OrderImportLineTransformation,
	OrderOnCartContent,
	WorkInstructionExportContent,
	WorkInstructionExportCreateHeader,
	WorkInstructionExportCreateTrailer,
	WorkInstructionExportLineTransformation,
	ParameterSetDataQuantityHealthCheck,
	ParameterSetDataPurge;
;

	/**
	 * Example script associated with the type 
	 */
	@Getter
	private String	exampleScript;
	
	private final Logger LOGGER	= LoggerFactory.getLogger(ExtensionPointType.class);

	private ExtensionPointType() {
		String resourceName = this.name() + ".groovy.example";
		try (InputStream resource = this.getClass().getResourceAsStream(resourceName)) {
			if (resource != null) {
				this.exampleScript = IOUtils.toString(resource, "UTF-8");
			} else {
				this.exampleScript = generateDefaultScript(this.name());
				LOGGER.error("Unable to load example script for the ExtensionPointType at " + resourceName + ". Defaulting to an empty script instead.");
			}
		} catch (IOException e) {
			throw new RuntimeException("Unable to load example script for the ExtensionPointType at " + resourceName, e);
		}
	}

	// Import/export not so symetrical, but probably most useful this way.
	// We expect to have an inventory import group
	public static enum ExtensionPointGroup {
		OrderImportGroup,
		EdiExportGroup,
		ParameterSetGroup,
		ExtensionUnknownGroup
	}

	public ExtensionPointGroup getExtensionGroup() {
		switch (this) {
			case OrderImportBeanTransformation:
			case OrderImportHeaderTransformation:
			case OrderImportCreateHeader:
			case OrderImportLineTransformation:
				return ExtensionPointGroup.OrderImportGroup;

			case OrderOnCartContent:
			case WorkInstructionExportContent:
			case WorkInstructionExportCreateHeader:
			case WorkInstructionExportCreateTrailer:
			case WorkInstructionExportLineTransformation:
				return ExtensionPointGroup.EdiExportGroup;

			case ParameterSetDataQuantityHealthCheck:
			case ParameterSetDataPurge:
				return ExtensionPointGroup.ParameterSetGroup;

			default:
				return ExtensionPointGroup.ExtensionUnknownGroup;
		}
	}

	public boolean isParameterSetExtension() {
		return getExtensionGroup() == ExtensionPointGroup.ParameterSetGroup;
	}

	private String generateDefaultScript(String name) {
		return 
				"def " + name + "(parameter) {\n" +
				"	//Provide the appropriate script before activating this extension\n"+
				"}";
	}
}
