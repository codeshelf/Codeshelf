package com.gadgetworks.codeshelf.device;

import lombok.EqualsAndHashCode;

@EqualsAndHashCode()
public class LedCmdPath {

	private String ledControllerId;
	
	private Short ledChannel;

	public LedCmdPath(String ledControllerId, Short ledChannel) {
		super();
		this.ledControllerId = ledControllerId;
		this.ledChannel = ledChannel;
	}
	
	
}
