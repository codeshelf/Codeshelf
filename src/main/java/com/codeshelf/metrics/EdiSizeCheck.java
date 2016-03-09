package com.codeshelf.metrics;

import java.util.List;

import com.codeshelf.model.domain.EdiGateway;
import com.codeshelf.model.domain.Facility;
import com.codeshelf.model.domain.SftpGateway;
import com.codeshelf.service.ExtensionPointEngine;

public class EdiSizeCheck extends HealthCheckRefreshJob{
	@Override
	public void check(Facility facility) throws Exception {
		StringBuilder errors = new StringBuilder();
		try {
			List<EdiGateway> ediGateways = facility.getEdiGateways();
			for (EdiGateway ediGateway : ediGateways){
				String header = String.format("Issue checking free space on EDI service %s.%s: ", ediGateway.getFacility().getDomainId(), ediGateway.getDomainId());
				if (!ediGateway.isActive()){
					continue;
				}
				if (ediGateway instanceof SftpGateway) {
					try {
						ExtensionPointEngine theService = ExtensionPointEngine.getInstance(ediGateway.getFacility());
						EdiFreeSpaceHealthCheckParamaters params = theService.getEdiFreeSpaceParameters();
						int minAvailableFiles = params.getMinAvailableFilesValue();
						int minAvailableSpaceMB = params.getMinAvailableSpaceMBValue();
						List<String> availableSpaceIssues = ((SftpGateway)ediGateway).checkForAvailableSpaceIssues(minAvailableFiles, minAvailableSpaceMB);
						for (String availableSpaceIssue : availableSpaceIssues){
							errors.append(header).append(availableSpaceIssue).append(". ");
						}
					} catch (Exception e) {
						errors.append(header).append(e.getMessage()).append(". ");
					}
				}
			}
		} catch (Exception e) {
			errors.append(e.getMessage());
		}
		if (errors.length() == 0) {
			saveResults(facility, true, "Success");
		} else {
			saveResults(facility, false, errors.toString());
		}
	}
}
