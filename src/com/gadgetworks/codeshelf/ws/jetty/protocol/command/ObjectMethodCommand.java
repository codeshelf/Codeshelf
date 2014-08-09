package com.gadgetworks.codeshelf.ws.jetty.protocol.command;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gadgetworks.codeshelf.model.dao.ITypedDao;
import com.gadgetworks.codeshelf.model.domain.IDomainObject;
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
		
		try {
			String className = request.getClassName();
			if (!className.startsWith("com.gadgetworks.codeshelf.model.domain.")) {
				className = "com.gadgetworks.codeshelf.model.domain." + className;
			}
			UUID objectId = UUID.fromString(request.getPersistentId());
			String methodName = request.getMethodName();
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
						Class classType = Class.forName(arg.getClassType());
						signatureClasses.add(classType);
						
						cookedArguments.add(argumentValue);

						/*

						Object typedArg = null;
						try {
							typedArg = argumentValue;
							if (!classType.isArray()) {
								// create object for simple data types
								if (argumentValue.getClass().equals(classType)) {
									typedArg = argumentValue;
								} else {
									Constructor<?> ctor = classType.getConstructor(String.class);
									typedArg = ctor.newInstance(argumentValue.toString());
								}
							} else {
								// create object for composite data types
								typedArg = argumentValue;
								/ *
								Array array = (Array) argumentValue;

								Class<?> arrayType = classType.getComponentType();
								typedArg = Array.newInstance(arrayType, array.getLength(arrayType).size());
								* /
								
								/ *
								//ArrayNode arrayNode = mapper.readValue(argumentValue, ArrayNode.class);
								Class<?> arrayType = classType.getComponentType();
								typedArg = Array.newInstance(arrayType, arrayNode.size());
								int i = 0;
								for (Iterator<JsonNode> iter = arrayNode.getElements(); iter.hasNext();) {
									JsonNode node = iter.next();
									Object nodeItem = mapper.readValue(node, arrayType);
									Array.set(typedArg, i++, nodeItem);
								}
								* /
							}
							cookedArguments.add(typedArg);
						} catch (Exception e) {
							response.setStatus(ResponseStatus.Fail);
							return response;
						}
							*/
					}

					Object methodResult = null;
					java.lang.reflect.Method method = classObject.getMethod(methodName, signatureClasses.toArray(new Class[0]));
					if (method != null) {
						try {
							methodResult = method.invoke(targetObject, cookedArguments.toArray(new Object[0]));
							response.setResults(methodResult);
							response.setStatus(ResponseStatus.Success);
							return response;
						} catch (Exception e) {
							LOGGER.error("Failed to invoke "+className+"."+methodName,e);
							response.setStatus(ResponseStatus.Fail);
							return response;
						}
					}
				} else {
					response.setStatus(ResponseStatus.Fail);
					response.setStatusMessage("Instance: " + objectId + " not found for type: " + classObject);
					return response;
				}
			}
		} catch (Exception e) {
			LOGGER.error("", e);
		}
		response.setStatus(ResponseStatus.Fail);
		return response;
	}

}
