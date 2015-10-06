package com.codeshelf.ws.protocol.command;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

import org.apache.commons.beanutils.ConvertUtilsBean;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.service.IApiBehavior;
import com.codeshelf.service.BehaviorFactory;
import com.codeshelf.validation.DefaultErrors;
import com.codeshelf.validation.ErrorCode;
import com.codeshelf.validation.InputValidationException;
import com.codeshelf.ws.protocol.request.ServiceMethodRequest;
import com.codeshelf.ws.protocol.response.ResponseABC;
import com.codeshelf.ws.protocol.response.ResponseStatus;
import com.codeshelf.ws.protocol.response.ServiceMethodResponse;
import com.codeshelf.ws.server.WebSocketConnection;
import com.google.common.base.Strings;

@RequiresPermissions("ux:method")
public class ServiceMethodCommand extends CommandABC {

	private static final Logger	LOGGER = LoggerFactory.getLogger(ServiceMethodCommand.class);

	private ServiceMethodRequest	request;

	private BehaviorFactory	behaviorFactory;

	private ConvertUtilsBean	converter;

	public ServiceMethodCommand(WebSocketConnection connection, ServiceMethodRequest request, BehaviorFactory serviceFactory, ConvertUtilsBean converter) {
		super(connection);
		this.request = request;
		this.behaviorFactory = serviceFactory;
		this.converter = converter;
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
		if (!className.startsWith("com.codeshelf.service.")) {
			className = "com.codeshelf.service." + className;
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
			Class<IApiBehavior> classObject = (Class<IApiBehavior>) Class.forName(className).asSubclass(IApiBehavior.class);
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
					Object behaviorObject = behaviorFactory.getInstance(classObject);
					Object[] convertedArgs = convertArguments(method, methodArgs);
					Object methodResult = method.invoke(behaviorObject, convertedArgs);
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
			convertedArgs[i] = converter.convert(arg, paramType);
			i++;
		}
		return convertedArgs;
	}
}
