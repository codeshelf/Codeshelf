package com.gadgetworks.codeshelf.ws.jetty.protocol.command;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.apache.commons.lang.ClassUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gadgetworks.codeshelf.model.dao.ITypedDao;
import com.gadgetworks.codeshelf.model.domain.IDomainObject;
import com.gadgetworks.codeshelf.validation.DefaultErrors;
import com.gadgetworks.codeshelf.validation.ErrorCode;
import com.gadgetworks.codeshelf.validation.Errors;
import com.gadgetworks.codeshelf.validation.InputValidationException;
import com.gadgetworks.codeshelf.ws.command.req.ArgsClass;
import com.gadgetworks.codeshelf.ws.jetty.protocol.request.ObjectMethodRequest;
import com.gadgetworks.codeshelf.ws.jetty.protocol.response.ObjectMethodResponse;
import com.gadgetworks.codeshelf.ws.jetty.protocol.response.ResponseABC;
import com.gadgetworks.codeshelf.ws.jetty.protocol.response.ResponseStatus;
import com.gadgetworks.codeshelf.ws.jetty.server.CsSession;

public class ObjectMethodCommand extends CommandABC {

	private static final Logger	LOGGER = LoggerFactory.getLogger(ObjectMethodCommand.class);

	private ObjectMethodRequest	request;
	
	public ObjectMethodCommand(CsSession session, ObjectMethodRequest request) {
		super(session);
		this.request = request;
	}
	
	@Override
	public ResponseABC exec() {
		ObjectMethodResponse response = new ObjectMethodResponse();
		
		String className = request.getClassName();
		if (className==null) {
			LOGGER.error("Object method command failed: Class name is undefined");

			response.setStatus(ResponseStatus.Fail);
			response.setStatusMessage("Class name is undefined");
			return response;
		}
		if (!className.startsWith("com.gadgetworks.codeshelf.model.domain.")) {
			className = "com.gadgetworks.codeshelf.model.domain." + className;
		}
		
		String methodName = request.getMethodName();
		if (methodName==null) {
			response.setStatus(ResponseStatus.Fail);
			response.setStatusMessage("Method name is undefined");
			return response;
		}

		try {
			UUID objectId = UUID.fromString(request.getPersistentId());
			List<ArgsClass> methodArgs = request.getMethodArgs();

			// First we find the parent object (by it's ID).
			Class<?> classObject = Class.forName(className);
			if (IDomainObject.class.isAssignableFrom(classObject)) {

				// First locate an instance of the parent class.
				ITypedDao<IDomainObject> dao = daoProvider.getDaoInstance((Class<IDomainObject>) classObject);
				IDomainObject targetObject = dao.findByPersistentId(objectId);

				if (targetObject != null) {

					// Loop over all the arguments, setting each one.
					List<Class<?>> signatureClasses = new ArrayList<Class<?>>();
					List<Object> cookedArguments = new ArrayList<Object>();
					for (ArgsClass arg : methodArgs) {
						// (The method *must* start with "get" to ensure other methods don't get called.)
						Object argumentValue = arg.getValue();
						//Class classType = Class.forName(arg.getClassType());
						Class<?> classType = ClassUtils.getClass(arg.getClassType());
						signatureClasses.add(classType);
						if (Double.class.isAssignableFrom(classType)){
								argumentValue = Double.valueOf(argumentValue.toString());
						}
						cookedArguments.add(argumentValue);	
						
					}

					Object methodResult = null;
					java.lang.reflect.Method method = classObject.getMethod(methodName, signatureClasses.toArray(new Class[0]));
					if (method != null) {
						Errors errors = new DefaultErrors(classObject);
						try {
							methodResult = method.invoke(targetObject, cookedArguments.toArray(new Object[0]));
							response.setResults(methodResult);
							response.setStatus(ResponseStatus.Success);
							return response;
						} catch (InvocationTargetException e ) {
							Throwable t = e.getTargetException();
							if (t instanceof InputValidationException) {
								errors.addAllErrors(((InputValidationException)t).getErrors());
								response.setStatus(ResponseStatus.Fail);
								response.setErrors(errors);
								return response;
								
							}
						} catch (Exception e) {
							LOGGER.error("Failed to invoke "+className+"."+method + ", with arguments: " + cookedArguments,e);
							errors.reject(ErrorCode.GENERAL, e.toString());
							response.setStatus(ResponseStatus.Fail);
							response.setErrors(errors);
							return response;
						}
					}
				} else {
					response.setStatus(ResponseStatus.Fail);
					response.setStatusMessage("Instance " + objectId + " of type " + classObject+" not found");
					return response;
				}
			}
		} catch (NoSuchMethodException e) {
			LOGGER.error("Method "+methodName+" does not exist on "+className, e);
			response.setStatusMessage("Method does not exist");
		} catch (Exception e) {
			LOGGER.error("Failed to invoke "+methodName+" on "+className, e);
		}
		response.setStatus(ResponseStatus.Fail);
		return response;
	}

}
