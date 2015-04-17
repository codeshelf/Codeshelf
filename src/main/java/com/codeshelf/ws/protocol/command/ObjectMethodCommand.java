package com.codeshelf.ws.protocol.command;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.apache.commons.lang.ClassUtils;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.model.dao.ITypedDao;
import com.codeshelf.model.domain.IDomainObject;
import com.codeshelf.model.domain.Organization;
import com.codeshelf.persistence.TenantPersistenceService;
import com.codeshelf.validation.DefaultErrors;
import com.codeshelf.validation.ErrorCode;
import com.codeshelf.validation.InputValidationException;
import com.codeshelf.ws.protocol.request.ObjectMethodRequest;
import com.codeshelf.ws.protocol.response.ObjectMethodResponse;
import com.codeshelf.ws.protocol.response.ResponseABC;
import com.codeshelf.ws.protocol.response.ResponseStatus;
import com.codeshelf.ws.server.WebSocketConnection;

@RequiresPermissions("ux:method")
public class ObjectMethodCommand extends CommandABC {

	private static final Logger	LOGGER	= LoggerFactory.getLogger(ObjectMethodCommand.class);

	private ObjectMethodRequest	request;

	public ObjectMethodCommand(WebSocketConnection connection, ObjectMethodRequest request) {
		super(connection);
		this.request = request;
	}

	@Override
	public ResponseABC exec() {
		ObjectMethodResponse response = new ObjectMethodResponse();

		String className = request.getClassName();
		if (className == null) {
			LOGGER.error("Object method command failed: Class name is undefined");

			response.setStatus(ResponseStatus.Fail);
			response.setStatusMessage("Class name is undefined");
			return response;
		}
		String methodName = request.getMethodName();
		if (methodName == null) {
			response.setStatus(ResponseStatus.Fail);
			response.setStatusMessage("Method name is undefined");
			return response;
		}

		List<ArgsClass> methodArgs = request.getMethodArgs();

		if (className.equals("Organization")) {
			// special... ignore ID
			try {
				return executeObjectMethodRequest(Organization.class, new Organization(), methodName, methodArgs);
			} catch (NoSuchMethodException | ClassNotFoundException e) {
				LOGGER.error("Failed to execute Organization method", e);
			}
			response.setStatus(ResponseStatus.Fail);
			return response;
		} else if (!className.startsWith("com.codeshelf.model.domain.")) {
			className = "com.codeshelf.model.domain." + className;
		}

		UUID objectId = UUID.fromString(request.getPersistentId());
		try {
			// First we find the parent object (by it's ID).
			@SuppressWarnings("unchecked")
			Class<? extends IDomainObject> classObject = (Class<? extends IDomainObject>) Class.forName(className);
			if (IDomainObject.class.isAssignableFrom(classObject)) {
				ITypedDao<? extends IDomainObject> dao = TenantPersistenceService.getInstance().getDao(classObject);
				// First locate an instance of the parent class.
				IDomainObject targetObject = dao.findByPersistentId(objectId);

				if (targetObject != null) {
					LOGGER.info("calling {}.{}()", classObject.getSimpleName(), methodName);
					return executeObjectMethodRequest(classObject, targetObject, methodName, methodArgs);
				} else {
					response.setStatus(ResponseStatus.Fail);
					response.setStatusMessage("Instance " + objectId + " of type " + classObject + " not found");
					return response;
				}
			}
		} catch (NoSuchMethodException e) {
			LOGGER.error("Method " + methodName + " does not exist on " + className, e);
			response.setStatusMessage("Method does not exist");
		} catch (Exception e) {
			LOGGER.error("Failed to invoke " + methodName + " on " + className, e);
		}
		response.setStatus(ResponseStatus.Fail);
		return response;
	}

	ObjectMethodResponse executeObjectMethodRequest(Class<?> classObject,
		Object targetObject,
		String methodName,
		List<ArgsClass> methodArgs) throws NoSuchMethodException, ClassNotFoundException {
		ObjectMethodResponse response = new ObjectMethodResponse();

		// Loop over all the arguments, setting each one.
		List<Class<?>> signatureClasses = new ArrayList<Class<?>>();
		List<Object> cookedArguments = new ArrayList<Object>();
		for (ArgsClass arg : methodArgs) {
			// (The method *must* start with "get" to ensure other methods don't get called.)
			Object argumentValue = arg.getValue();
			//Class classType = Class.forName(arg.getClassType());
			Class<?> classType = ClassUtils.getClass(arg.getClassType());
			signatureClasses.add(classType);
			if (Double.class.isAssignableFrom(classType)) {
				if (argumentValue == null) {
					LOGGER.error("Failed to invoke " + classObject.getSimpleName() + "." + methodName + ": Argument "
							+ arg.getName() + " is undefined.");
					response.setStatus(ResponseStatus.Fail);
					return response;
				}
				argumentValue = Double.valueOf(argumentValue.toString());
			} else if (int.class.isAssignableFrom(classType)) {
				if (argumentValue == null) {
					LOGGER.error("Failed to invoke " + classObject.getSimpleName() + "." + methodName + ": Argument "
							+ arg.getName() + " is undefined.");
					response.setStatus(ResponseStatus.Fail);
					return response;
				}
				argumentValue = Integer.valueOf(argumentValue.toString());
			}
			cookedArguments.add(argumentValue);
		}

		Object methodResult = null;
		java.lang.reflect.Method method = classObject.getMethod(methodName, signatureClasses.toArray(new Class[0]));
		if (method != null) {
			DefaultErrors errors = new DefaultErrors(classObject);
			try {
				methodResult = method.invoke(targetObject, cookedArguments.toArray(new Object[0]));
				response.setResults(methodResult);
				response.setStatus(ResponseStatus.Success);
				return response;
			} catch (InvocationTargetException e) {
				Throwable targetException = e.getTargetException();
				if (targetException instanceof InputValidationException) {
					LOGGER.error("Failed to invoke " + classObject.getSimpleName() + "." + method + ", with arguments: "
							+ cookedArguments, targetException);
					errors.addAllErrors(((InputValidationException) targetException).getErrors());
					response.setStatus(ResponseStatus.Fail);
					response.setStatusMessage(errors.toString());
					response.setErrors(errors);
					return response;

				} else {
					LOGGER.error("Failed to invoke " + classObject.getSimpleName() + "." + method + ", with arguments: "
							+ cookedArguments, targetException);
					errors.reject(ErrorCode.GENERAL, targetException.toString());
					response.setStatus(ResponseStatus.Fail);
					response.setStatusMessage(errors.toString());
					response.setErrors(errors);
					return response;
				}
			} catch (Exception e) {
				LOGGER.error("Failed to invoke " + classObject.getSimpleName() + "." + method + ", with arguments: "
						+ cookedArguments, e);
				errors.reject(ErrorCode.GENERAL, e.toString());
				response.setStatus(ResponseStatus.Fail);
				response.setErrors(errors);
				return response;
			}
		}
		response.setStatus(ResponseStatus.Fail);
		return response;
	}

}
