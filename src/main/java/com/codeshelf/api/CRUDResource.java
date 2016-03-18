package com.codeshelf.api;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import javax.persistence.EntityExistsException;
import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriInfo;

import org.apache.shiro.SecurityUtils;

import com.codeshelf.model.dao.ITypedDao;
import com.codeshelf.model.dao.ResultDisplay;
import com.codeshelf.model.domain.IDomainObject;
import com.codeshelf.model.domain.IDomainObjectTree;
import com.codeshelf.persistence.TenantPersistenceService;
import com.google.common.base.Optional;
import com.sun.jersey.api.representation.Form;

import lombok.Getter;
import lombok.Setter;

public abstract class CRUDResource<T extends IDomainObjectTree<P>, P extends IDomainObject> {

	@Getter @Setter
	private P parent;
	private String permissionPrefix;
	private Class<T> daoClass;
	private Set<String> updateableProperties;
	
	public CRUDResource(Class<T> daoClass, String permissionPrefix, Optional<Set<String>> updateableProperties) {
		this.daoClass = daoClass;
		this.permissionPrefix = permissionPrefix;
		this.updateableProperties = updateableProperties.or(Collections.emptySet());
	}

	protected abstract String getNewDomainIdExistsMessage(String inDomainId);
	
	private ITypedDao<T> getDao() {
		ITypedDao<T> dao = TenantPersistenceService.getInstance().getDao(daoClass);
		return dao;
	}

	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public ResultDisplay<T> getAll(@Context UriInfo uriInfo) {
		SecurityUtils.getSubject().checkPermission(permissionPrefix + ":view");
		MultivaluedMap<String, String> queryParameters = uriInfo.getQueryParameters();
		return doFindByParent(parent, queryParameters);
	}
	
	@POST
	@Consumes("application/x-www-form-urlencoded")
	@Produces(MediaType.APPLICATION_JSON)
	public Response create(MultivaluedMap<String, String> params) throws EntityExistsException {
		SecurityUtils.getSubject().checkPermission(permissionPrefix + ":create");
		
		try {
			T newObject = doNewObject(params);
			//isValid
			Set<String> violations = validateObject(newObject);
			if(!violations.isEmpty()) {
				ErrorResponse errorResponse = new ErrorResponse();
				errorResponse.addErrors(violations);
				return errorResponse.buildResponse();
			}

			//already Exists - TODO should be handled on commit instead
			String inDomainId = ParameterUtils.getDomainId(params);
			T existing = doGetById(inDomainId);
			if (existing != null) {
				ErrorResponse error = new ErrorResponse(getNewDomainIdExistsMessage(inDomainId));
				return error.buildResponse();
			}

			ITypedDao<T> dao = getDao();
			dao.store(newObject);
			return Response.ok(newObject).build();

		} catch (ReflectiveOperationException e) {
			throw new WebApplicationException(e, Status.BAD_REQUEST);
		}
	}
	
	private Set<String> validateObject(T newObject) throws ReflectiveOperationException {
		HashSet<String> errorMessages = new HashSet<>(); 
		ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
		Validator validator = factory.getValidator();
		Set<ConstraintViolation<T>> violations = validator.validate(newObject);
		for (ConstraintViolation<T> constraintViolation : violations) {
			errorMessages.add(constraintViolation.getMessage());
		}
		return errorMessages;
	}

	@GET
	@Path("/{id}")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getById(@PathParam("id") String id) {
		SecurityUtils.getSubject().checkPermission(permissionPrefix + ":view");
		T object = doGetById(id);
		if (object == null) {
			return Response.status(Response.Status.NOT_FOUND).build();
		} else {
			return Response.ok(object).build();
		}
	}
	
	protected T doGetById(String id) {
		T object = null;
		try {
			UUID uuid = UUID.fromString(id);
			object = getDao().findByPersistentId(uuid);
		} catch (IllegalArgumentException e ) {
			object = getDao().findByDomainId(parent, id);
		}
		return object;
	}

	@POST
	@Path("/{id}")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	public Response update(@PathParam("id") String id, MultivaluedMap<String, String> params) {
		SecurityUtils.getSubject().checkPermission(permissionPrefix + ":edit");
		T object = doGetById(id);
		if (object == null) {
			throw new WebApplicationException(Status.NOT_FOUND);
		}
		try {
			String inDomainId = ParameterUtils.getDomainId(params);
			if (newDomainIdExists(object, inDomainId)) {
				ErrorResponse error = new ErrorResponse(getNewDomainIdExistsMessage(inDomainId));
				return error.buildResponse();
			}
			T objectToUpdate = doUpdate(object, params);
			Set<String> violations = validateObject(objectToUpdate);
			if(!violations.isEmpty()) {
				ErrorResponse errorResponse = new ErrorResponse();
				errorResponse.addErrors(violations);
				return errorResponse.buildResponse();
			}

			getDao().store(objectToUpdate);
			return Response.ok(objectToUpdate).build();
		} catch(ReflectiveOperationException e) {
			throw new WebApplicationException(e, Status.BAD_REQUEST);
		}
	}

	private boolean newDomainIdExists(T object, String inDomainId) {
		if (!inDomainId.equals(object.getDomainId())) {
			T existing = doGetById(inDomainId);
			return (existing != null && !existing.equals(object));
		} else {
			return false;
		}
	}

	@DELETE
	@Path("/{id}")
	@Produces(MediaType.APPLICATION_JSON)
	public void delete(@PathParam("id") String id) {
		SecurityUtils.getSubject().checkPermission(permissionPrefix + ":edit");
		T object = doGetById(id);
		getDao().delete(object);
	}
	
	protected  ResultDisplay<T> doFindByParent(P parent, MultivaluedMap<String, String> params) {
		List<T> findByParent = getDao().findByParent(parent);
		return new ResultDisplay<>(findByParent);
	}
	
	protected T doUpdate(T bean, MultivaluedMap<String, String> params) throws ReflectiveOperationException {
		Form form = new Form();
		for (String key : params.keySet()) {
			if(updateableProperties.contains(key)) {
				form.put(key, params.get(key));
			}
		}

		T updatedBean = ParameterUtils.populate(bean, form);
		return updatedBean;
	}

	protected void doValidate(MultivaluedMap<String, String> params) {
		
	}

	protected T doNewObject(MultivaluedMap<String, String> params) throws ReflectiveOperationException {
		T updatedWorker = ParameterUtils.populate(daoClass.newInstance(), params);
		updatedWorker.setParent(getParent());
		return updatedWorker;
	}



}
