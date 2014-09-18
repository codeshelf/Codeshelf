package com.gadgetworks.codeshelf.service;

import java.util.List;

import com.gadgetworks.codeshelf.model.WiSetSummary;
import com.gadgetworks.codeshelf.model.WiSummarizer;

public class WorkService {

	public WorkService() {
		
	}
	
	public List<WiSetSummary> workSummary(String cheId, String facilityId) {
		WiSummarizer summarizer = new WiSummarizer();
		summarizer.computeWiSummariesForChe(cheId, facilityId);
		return summarizer.getSummaries();
	}
}
