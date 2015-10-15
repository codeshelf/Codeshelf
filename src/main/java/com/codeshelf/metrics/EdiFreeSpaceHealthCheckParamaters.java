package com.codeshelf.metrics;

import com.codeshelf.service.ParameterSetBeanABC;
import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.Getter;
import lombok.Setter;

public class EdiFreeSpaceHealthCheckParamaters extends ParameterSetBeanABC {
	// default values
	final static int			MIN_AVAILABLE_FILES		= 500;
	final static int			MIN_AVAILABLE_SPACE_MB	= 20;
	
	@Getter
	@Setter
	@JsonIgnore
	protected String			minAvailableFiles;
	
	@Getter
	@Setter
	@JsonIgnore
	protected String			minAvailableSpaceMB;

	public EdiFreeSpaceHealthCheckParamaters() {
		super();
		minAvailableFiles = Integer.toString(MIN_AVAILABLE_FILES);
		minAvailableSpaceMB = Integer.toString(MIN_AVAILABLE_SPACE_MB);
	}

	public int getMinAvailableFilesValue() {
		return getCleanValue("minAvailableFiles", getMinAvailableFiles(), MIN_AVAILABLE_FILES);
	}

	public int getMinAvailableSpaceMBValue() {
		return getCleanValue("miinAvailableSpaceMB", getMinAvailableSpaceMB(), MIN_AVAILABLE_SPACE_MB);
	}

	@Override
	public String getParametersDescription() {
		return String.format("minAvailableFiles: %d; minAvailableSpaceMB: %d;",
			getMinAvailableFilesValue(),
			getMinAvailableSpaceMBValue());
	}
}
