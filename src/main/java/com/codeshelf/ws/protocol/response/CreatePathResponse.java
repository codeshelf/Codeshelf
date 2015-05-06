package com.codeshelf.ws.protocol.response;

import lombok.Getter;
import lombok.Setter;

import com.codeshelf.model.domain.Path;

public class CreatePathResponse extends ResponseABC {
	@Getter @Setter
	Path path;

	@Override
	public String getDeviceIdentifier() {
		return null;
	}
}
