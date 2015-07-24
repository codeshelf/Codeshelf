package com.codeshelf.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.ws.protocol.request.InfoRequest;
import com.codeshelf.ws.protocol.request.InfoRequest.InfoRequestType;

public class InfoService implements IApiService{
	private static final Logger	LOGGER			= LoggerFactory.getLogger(InfoService.class);
	
	public String[] getInfo(InfoRequest request){
		InfoRequestType type = request.getType();
		String info[] = null;
		switch(type){			
			case GET_WALL_LOCATION_INFO:
				info = getWallLocationInfo(request.getLocation());
				return info;

			default:
				String unexpectedRequest[] = new String[3];
				unexpectedRequest[0] = "Unexpected Request";
				unexpectedRequest[1] = "Type = " + (type == null ? "null" : type.toString());
				return unexpectedRequest;
		}
	}
	
	private String[] getWallLocationInfo(String location){
		String[] info = new String[3];
		if (location == null) {
			LOGGER.error("InfoService Error: Received Wall Location Info request with null Location");
			info[0] = "ERROR: Server didn't";
			info[1] = "receive info locagion";
		}
		info[0] = "AAAAAAAAAAAAAAAAA";
		info[1] = "BBBBBBBBBBBBBBBBB";
		return info;
	}
}