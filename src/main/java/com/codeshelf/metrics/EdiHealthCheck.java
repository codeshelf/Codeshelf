package com.codeshelf.metrics;

import com.codeshelf.edi.EdiProcessorService;

public class EdiHealthCheck extends CodeshelfHealthCheck {
	final static int EDI_SERVICE_CYCLE_TIMEOUT_SECONDS = 60*5; // timeout if EDI takes longer than 5 mins
	
	EdiProcessorService ediService;
	
	
	public EdiHealthCheck(EdiProcessorService ediService) {
		super("EDI");
		
		this.ediService = ediService;
	}

	@Override
	protected Result check() throws Exception {
		if(!ediService.isRunning()) {
			return Result.unhealthy("EDI not running");
		} //else
		String err = ediService.getErrorStatus();
		if(err != null) {
			return Result.unhealthy(err);
		} //else
		long lastSuccessTime = ediService.getLastSuccessTime();
		long secondsSinceLastSuccess = (System.currentTimeMillis() - lastSuccessTime)/1000; 
		if(secondsSinceLastSuccess > EDI_SERVICE_CYCLE_TIMEOUT_SECONDS) {
			return Result.unhealthy(String.format("%d secs since last success", secondsSinceLastSuccess));
		} //else			
		return Result.healthy(OK);
	}

}
