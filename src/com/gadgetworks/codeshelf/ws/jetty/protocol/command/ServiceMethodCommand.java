package com.gadgetworks.codeshelf.ws.jetty.protocol.command;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gadgetworks.codeshelf.service.IApiService;
import com.gadgetworks.codeshelf.service.ServiceFactory;
import com.gadgetworks.codeshelf.validation.DefaultErrors;
import com.gadgetworks.codeshelf.validation.ErrorCode;
import com.gadgetworks.codeshelf.validation.InputValidationException;
import com.gadgetworks.codeshelf.ws.jetty.protocol.request.ServiceMethodRequest;
import com.gadgetworks.codeshelf.ws.jetty.protocol.response.ResponseABC;
import com.gadgetworks.codeshelf.ws.jetty.protocol.response.ResponseStatus;
import com.gadgetworks.codeshelf.ws.jetty.protocol.response.ServiceMethodResponse;
import com.gadgetworks.codeshelf.ws.jetty.server.UserSession;
import com.google.common.base.Strings;

public class ServiceMethodCommand extends CommandABC {

	private static final Logger	LOGGER = LoggerFactory.getLogger(ServiceMethodCommand.class);

	private ServiceMethodRequest	request;

	private ServiceFactory	serviceFactory;
	
	public ServiceMethodCommand(UserSession session, ServiceMethodRequest request, ServiceFactory serviceFactory) {
		super(session);
		this.request = request;
		this.serviceFactory = serviceFactory;
	}
	
	@Override
	public ResponseABC exec() {
		ServiceMethodResponse response = new ServiceMethodResponse();
		DefaultErrors errors = new DefaultErrors(this.getClass());
		String className = request.getClassName();
		if (className==null) {
			LOGGER.error("Class name is undefined");

			response.setStatus(ResponseStatus.Fail);
			response.setStatusMessage("Class name is undefined");
			errors.reject(ErrorCode.GENERAL, "Class name is undefined");
			response.setErrors(errors);
			return response;
		}
		if (!className.startsWith("com.gadgetworks.codeshelf.service.")) {
			className = "com.gadgetworks.codeshelf.service." + className;
		}
		
		String methodName = request.getMethodName();
		if (Strings.isNullOrEmpty(methodName)) {
			response.setStatus(ResponseStatus.Fail);
			response.setStatusMessage("Method name is undefined");
			errors.reject(ErrorCode.GENERAL, "Method name is undefined");
			response.setErrors(errors);
			return response;
		}
		List<?> methodArgs = request.getMethodArgs();
		try {
			Method method = null;
			// First we find the parent object (by it's ID).
			@SuppressWarnings("unchecked")
			Class<IApiService> classObject = (Class<IApiService>) Class.forName(className).asSubclass(IApiService.class);
			for (Method classMethod : classObject.getMethods()) {
				if (classMethod.getName().equals(methodName)) {
					if (methodArgs.size() == classMethod.getParameterTypes().length) {
						method = classMethod;
						break;
					}
				}
			};	
			if (method != null) {
				try {
					Object serviceObject = serviceFactory.getServiceInstance(classObject);
					Object[] convertedArgs = convertArguments(method, methodArgs);
					Object methodResult = method.invoke(serviceObject, convertedArgs);
					response.setResults(methodResult);
					response.setStatus(ResponseStatus.Success);
					return response;
				} catch (InvocationTargetException e ) {
					Throwable t = e.getTargetException();
					if (t instanceof InputValidationException) {
						errors.addAllErrors(((InputValidationException)t).getErrors());
						response.setStatus(ResponseStatus.Fail);
						response.setStatusMessage("Failed to invoke " + methodName + " for args " + methodArgs + " on type " + classObject);
						response.setErrors(errors);
						return response;
						
					} else {
						String message = "Failed to invoke " + methodName + " for args " + methodArgs + " on type " + classObject; 
						LOGGER.error(message, e);
						response.setStatus(ResponseStatus.Fail);
						response.setStatusMessage(message);
						errors.reject(ErrorCode.GENERAL, message);
						response.setErrors(errors);
						return response;
					}
				} catch (Exception e) {
					String message = "Failed to invoke " + methodName + " for args " + methodArgs + " on type " + classObject; 
					LOGGER.error(message, e);
					response.setStatus(ResponseStatus.Fail);
					response.setStatusMessage(message);
					errors.reject(ErrorCode.GENERAL, message);
					response.setErrors(errors);
					return response;
				}
			}
			else {
				String message = "Method " + methodName + " for args " + methodArgs + " does not exist on type " + classObject;
				response.setStatus(ResponseStatus.Fail);
				response.setStatusMessage(message);
				errors.reject(ErrorCode.GENERAL, message);
				response.setErrors(errors);
				return response;
			}
		} catch (Exception e) {
			String message = "Failed to invoke " + methodName + " for args " + methodArgs + " on type " + className;
			LOGGER.error(message, e);
			errors.reject(ErrorCode.GENERAL, message);
			response.setStatus(ResponseStatus.Fail);
			response.setStatusMessage(message);
			response.setErrors(errors);
			return response;
		}
	}
	
	/**
	 * Inelegant converter for the time being
	 */
	private Object[] convertArguments(Method method, List<?> methodArgs) {
		Class<?>[] parameterTypes = method.getParameterTypes();
		Object[] convertedArgs = new Object[parameterTypes.length]; 
		int i = 0;
		for (Object arg : methodArgs) {
			Class<?> paramType = parameterTypes[i];
			if (paramType.isAssignableFrom(methodArgs.getClass())) {
				convertedArgs[i] = arg;
			} else if (UUID.class.isAssignableFrom(paramType)) {
				convertedArgs[i] = UUID.fromString(String.valueOf(arg));
			} else if (Double.class.isAssignableFrom(paramType)){
				convertedArgs[i] = Double.valueOf(String.valueOf(arg));
			}
			else {
				throw new IllegalArgumentException("could not convert argument: " + arg + " to " + paramType);
			}
			i++;
		}
		return convertedArgs;
	}
}
