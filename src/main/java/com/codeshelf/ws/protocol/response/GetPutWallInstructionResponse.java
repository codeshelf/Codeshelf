package com.codeshelf.ws.protocol.response;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import lombok.Getter;
import lombok.Setter;

import com.codeshelf.model.domain.WorkInstruction;
import com.codeshelf.persistence.TenantPersistenceService;

public class GetPutWallInstructionResponse extends ResponseABC {
	private static final Logger		LOGGER	= LoggerFactory.getLogger(GetPutWallInstructionResponse.class);
	// Initially a clone of GetWorkResponse, but we expect it will drift.

	@Getter
	@Setter
	String							networkGuid;

	@Getter
	private List<WorkInstruction>	workInstructions;

	public void setWorkInstructions(List<WorkInstruction> wis) {
		if (wis == null) {
			this.workInstructions = new ArrayList<WorkInstruction>();
		} else {
			LOGGER.info("Temp: GetPutWallInstructionResponse given {} work instructions", wis.size());

			workInstructions = new ArrayList<WorkInstruction>(wis.size());
			for (WorkInstruction wi : wis) {
				workInstructions.add(TenantPersistenceService.<WorkInstruction> deproxify(wi));
			}
			LOGGER.info("Temp: GetPutWallInstructionResponse converted {} work instructions", workInstructions.size());
		}
	}

	public void logTheCount() {
		if (workInstructions == null)
			LOGGER.info("Temp: GetPutWallInstructionResponse has null list and therefore no work instructions");
		else
			LOGGER.info("Temp: GetPutWallInstructionResponse has {} work instructions", workInstructions.size());
	}
}
