package com.codeshelf.metrics;

import com.codeshelf.model.domain.DropboxGateway;
import com.codeshelf.model.domain.Facility;

public class DropboxGatewayHealthCheck extends HealthCheckRefreshJob {
	@Override
	public void check(Facility facility) throws Exception {
		boolean success = true;
		DropboxGateway service = facility.getDropboxGateway();
		if (service != null) {
			if (service.testConnection() == false) {
				success = false;
			}
		} 
		if (success){
			saveResults(facility, true, "Dropbox connection OK");
		} else {
			saveResults(facility, false, "Dropbox connection failed");
		}
	}
}
