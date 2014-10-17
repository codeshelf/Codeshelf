package com.gadgetworks.codeshelf.device;

import lombok.EqualsAndHashCode;
import lombok.ToString;

@EqualsAndHashCode()
@ToString()
public class LedCmdPath {

	private String ledControllerId;
	
	private Short ledChannel;

	public LedCmdPath(String ledControllerId, Short ledChannel) {
		super();
		this.ledControllerId = ledControllerId;
		this.ledChannel = ledChannel;
	}
	
	
}
