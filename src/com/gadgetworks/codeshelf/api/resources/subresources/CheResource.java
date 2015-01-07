package com.gadgetworks.codeshelf.api.resources.subresources;

import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import lombok.Setter;

import com.gadgetworks.codeshelf.api.BaseResponse;
import com.gadgetworks.codeshelf.api.ErrorResponse;
import com.gadgetworks.codeshelf.api.BaseResponse.UUIDParam;
import com.gadgetworks.codeshelf.api.ObjectResponse;
import com.gadgetworks.codeshelf.model.domain.Che;
import com.gadgetworks.codeshelf.model.domain.Container;
import com.gadgetworks.codeshelf.model.domain.Facility;
import com.gadgetworks.codeshelf.model.domain.WorkInstruction;
import com.gadgetworks.codeshelf.platform.persistence.PersistenceService;

public class CheResource {
	private PersistenceService persistence = PersistenceService.getInstance();

	@Setter
	private UUIDParam mUUIDParam;
	
	@GET
	@Path("/computeinstructions")
	@Produces(MediaType.APPLICATION_JSON)
	public Response computeWorkInstructions(@QueryParam("id") List<String> containers) {
		ErrorResponse errors = new ErrorResponse();
		if (!BaseResponse.isUUIDValid(mUUIDParam, "cheId", errors)){
			return errors.buildResponse();
		}

		try {
			persistence.beginTenantTransaction();
			Che che = Che.DAO.findByPersistentId(mUUIDParam.getUUID());
			if (che == null) {
				errors.addErrorUUIDDoesntExist(mUUIDParam.getRawValue(), "che");
				return errors.buildResponse();
			}
			//User picker = new User();
			//che.setCurrentUser(currentUser);
			Facility facility = che.getFacility();
			List<String> validContainers = new ArrayList<>();
			for (String containerId : containers) {
				Container container = Container.DAO.findByDomainId(facility, containerId);
				if (container != null) {
					validContainers.add(containerId);
				}
			}
			List<WorkInstruction> instructions = facility.computeWorkInstructions(che, validContainers);
			ObjectResponse response = new ObjectResponse(instructions);
			return response.buildResponse();
		} catch (Exception e) {
			errors.processException(e);
			return errors.buildResponse();
		} finally {
			persistence.commitTenantTransaction();
		}
	}
}