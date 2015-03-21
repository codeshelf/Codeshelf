package com.codeshelf.api.resources.subresources;

import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import lombok.Setter;

import com.codeshelf.api.BaseResponse;
import com.codeshelf.api.BaseResponse.UUIDParam;
import com.codeshelf.api.ErrorResponse;
import com.codeshelf.manager.Tenant;
import com.codeshelf.model.domain.Che;
import com.codeshelf.model.domain.Container;
import com.codeshelf.model.domain.Facility;
import com.codeshelf.model.domain.WorkPackage.WorkList;
import com.codeshelf.platform.persistence.ITenantPersistenceService;
import com.codeshelf.platform.persistence.TenantPersistenceService;
import com.codeshelf.security.CodeshelfSecurityManager;
import com.codeshelf.service.WorkService;
import com.google.inject.Inject;

public class CheResource {
	private ITenantPersistenceService persistence = TenantPersistenceService.getInstance();

	@Setter
	private UUIDParam mUUIDParam;
	
	private WorkService workService;
	
	@Inject
	public CheResource(WorkService workService) {
		this.workService = workService;
	}
	
	@GET
	@Path("/computeinstructions")
	@Produces(MediaType.APPLICATION_JSON)
	public Response computeWorkInstructions(@QueryParam("containers") List<String> containers) {
		ErrorResponse errors = new ErrorResponse();
		if (!BaseResponse.isUUIDValid(mUUIDParam, "cheId", errors)){
			return errors.buildResponse();
		}
		Tenant tenant = CodeshelfSecurityManager.getCurrentTenant();

		try {
			persistence.beginTransaction(tenant);
			Che che = Che.staticGetDao().findByPersistentId(tenant,mUUIDParam.getUUID());
			if (che == null) {
				errors.addErrorUUIDDoesntExist(mUUIDParam.getRawValue(), "che");
				return errors.buildResponse();
			}
			//User picker = new User();
			//che.setCurrentUser(currentUser);
			Facility facility = che.getFacility();
			List<String> validContainers = new ArrayList<>();
			for (String containerId : containers) {
				Container container = Container.staticGetDao().findByDomainId(tenant,facility, containerId);
				if (container != null) {
					validContainers.add(containerId);
				}
			}
			WorkList workList = workService.computeWorkInstructions(tenant,che, validContainers);
			return BaseResponse.buildResponse(workList);
		} catch (Exception e) {
			errors.processException(e);
			return errors.buildResponse();
		} finally {
			persistence.commitTransaction(tenant);
		}
	}
}
