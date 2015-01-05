package com.gadgetworks.codeshelf.api;

import lombok.Getter;
import lombok.Setter;

public class ObjectResponse extends BaseResponse{
	@Getter
	@Setter
	Object response = null;
	
	public ObjectResponse(Object response) {
		this.response = response;
	}
}
