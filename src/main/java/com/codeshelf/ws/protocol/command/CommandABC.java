package com.codeshelf.ws.protocol.command;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.application.ContextLogging;
import com.codeshelf.model.domain.Che;
import com.codeshelf.model.domain.Facility;
import com.codeshelf.ws.protocol.request.RequestABC;
import com.codeshelf.ws.protocol.response.ResponseABC;
import com.codeshelf.ws.server.WebSocketConnection;

public abstract class CommandABC<P extends RequestABC> {
	private static final Logger	LOGGER = LoggerFactory.getLogger(CommandABC.class);
	protected P request;
	private String prevFacilityId = null;
	private String deviceGuid = null;
	private long start;
	
	WebSocketConnection wsConnection;
	
	public CommandABC(WebSocketConnection connection, P request) {
		this.wsConnection = connection;
		this.request = request;
	}
	
	protected abstract ResponseABC exec();
	
	public ResponseABC run(){
		beforeExec();
		start = System.currentTimeMillis();
		ResponseABC response = null;
		try {
			response = exec();
		} finally {
			afterExec();
		}
		return response;
	}
	
	protected void beforeExec(){
		prevFacilityId = ContextLogging.getTag(ContextLogging.THREAD_CONTEXT_FACILITY_KEY);
		String deviceId = request.getDeviceIdentifier();
		String facilityId = null;
		if (deviceId != null){
			Che che = Che.staticGetDao().findByPersistentId(deviceId);
			if (che != null) {
				Facility facility = che.getFacility();
				facilityId = facility.getDomainId();
				deviceGuid = che.getDeviceGuidStrNoPrefix();
			}
		}
		
		ContextLogging.setTag(ContextLogging.THREAD_CONTEXT_FACILITY_KEY, facilityId);
		if (deviceGuid == null) {
			LOGGER.info("Execute {}", getClass().getSimpleName());
		} else {
			LOGGER.info("Execute {} for device {}", getClass().getSimpleName(), deviceGuid);
		}
	}

	protected void afterExec(){
		long durationSec = (System.currentTimeMillis() - start) / 1000;
		if (deviceGuid == null) {
			LOGGER.info("Completed {} in {}s", getClass().getSimpleName(), durationSec);
		} else {
			LOGGER.info("Completed {} for device {} in {}s", getClass().getSimpleName(), deviceGuid, durationSec);
		}
		ContextLogging.setTag(ContextLogging.THREAD_CONTEXT_FACILITY_KEY, prevFacilityId);		
	}
}
