package com.codeshelf.ws.client;

import java.util.Date;

import lombok.Getter;

import com.codeshelf.ws.protocol.request.RequestABC;

public class RequestInfo {

	@Getter
	RequestABC request;
	
	@Getter
	Date creationTime;
	
	public RequestInfo(RequestABC request) {
		this.request = request;
		this.creationTime = new Date();
	}
}
