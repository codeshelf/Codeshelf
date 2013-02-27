/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: WebSessionReqCmdObjectMethod.java,v 1.9 2013/02/27 01:17:02 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.web.websession.command.req;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import lombok.libs.org.objectweb.asm.tree.ClassNode;
import lombok.libs.org.objectweb.asm.tree.LocalVariableNode;
import lombok.libs.org.objectweb.asm.tree.MethodNode;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ObjectNode;
import org.codehaus.jackson.type.TypeReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.avaje.ebean.enhance.asm.ClassReader;
import com.avaje.ebean.enhance.asm.ClassVisitor;
import com.avaje.ebean.enhance.asm.Type;
import com.gadgetworks.codeshelf.model.dao.IDaoProvider;
import com.gadgetworks.codeshelf.model.dao.ITypedDao;
import com.gadgetworks.codeshelf.model.domain.IDomainObject;
import com.gadgetworks.codeshelf.web.websession.command.resp.IWebSessionRespCmd;
import com.gadgetworks.codeshelf.web.websession.command.resp.WebSessionRespCmdObjectMethod;

/**
 * 
 * INBOUND COMMAND STRUCTURE:
 * 
 * command {
 * 	id: <cmd_id>,
 * 	type: OBJ_METH_RQ,
 * 	data {
 *		className:			<class_name>,
 *		peristentID:		<id>,  (null for class method, ID for instance method.)
 *		methodName:			<methodName>,
 *		methodArguments [
 *			argument: {
 *				name:	<argumentName>
 *				value:	<argumentValue>
 *			}
 *		]
 * 	}
 * }
 * 
 * OUTBOUND COMMAND STRUCTURE:
 * 
 * command {
 * 	id: <cmd_id>,
 * 	type: OBJ_METH_RS,
 * 	data {
 * 		result: 			<result>
 *  }
 * }
 *
 * @author jeffw
 *
 */
public class WebSessionReqCmdObjectMethod extends WebSessionReqCmdABC {

	private static final Logger	LOGGER	= LoggerFactory.getLogger(WebSessionReqCmdObjectUpdate.class);

	private IDaoProvider		mDaoProvider;
	private List<ArgsClass>		mMethodArguments;

	/**
	 * @param inCommandId
	 * @param inDataNodeAsJson
	 */
	public WebSessionReqCmdObjectMethod(final String inCommandId, final JsonNode inDataNodeAsJson, final IDaoProvider inDaoProvider) {
		super(inCommandId, inDataNodeAsJson);
		mDaoProvider = inDaoProvider;
		mMethodArguments = new ArrayList<ArgsClass>();
	}

	public final WebSessionReqCmdEnum getCommandEnum() {
		return WebSessionReqCmdEnum.OBJECT_UPDATE_REQ;
	}

	public final IWebSessionRespCmd doExec() {
		IWebSessionRespCmd result = null;

		// CRITICAL SECUTIRY CONCEPT.
		// The remote end can NEVER get object results outside of it's own scope.
		// Today, the scope is set by the user's ORGANIZATION.
		// That means we can never return objects not part of the current (logged in) user's organization.
		// THAT MEANS WE MUST ALWAYS ADD A WHERE CLAUSE HERE THAT LOCKS US INTO THIS.

		try {

			JsonNode dataJsonNode = getDataJsonNode();
			JsonNode classNode = dataJsonNode.get(CLASSNAME);
			String className = classNode.getTextValue();
			if (!className.startsWith("com.gadgetworks.codeshelf.model.domain.")) {
				className = "com.gadgetworks.codeshelf.model.domain." + className;
			}
			JsonNode idNode = dataJsonNode.get(PERSISTENT_ID);
			UUID objectId = UUID.fromString(idNode.getTextValue());

			JsonNode methodNameNode = dataJsonNode.get(METHODNAME);
			String methodName = methodNameNode.getTextValue();

			ObjectMapper mapper = new ObjectMapper();
			JsonNode argumentsNode = dataJsonNode.get(METHODARGS);
			//			List<Map<String, Object>> objectArray = mapper.readValue(argumentsNode, new TypeReference<List<Map<String, Object>>>() {
			//			});
			//			for (Map<String, Object> map : objectArray) {
			//				String name = (String) map.get("name");
			//				Object value = map.get("value");
			//				mMethodArguments.put(name, value);
			//			}

			mMethodArguments = mapper.readValue(argumentsNode, new TypeReference<List<ArgsClass>>() {
			});

			//			JsonNode argumentsNode = dataJsonNode.get(ARGUMENTS);
			//
			//			ObjectMapper mapper = new ObjectMapper();
			//			List<Object> list = mapper.readValue(argumentsNode, new TypeReference<List<Object>>() { });
			//
			//			if (argumentsNode.isArray()) {
			//				ArrayNode argumentsArrayNode = (ArrayNode) argumentsNode;
			//			}

			// First we find the parent object (by it's ID).
			Class<?> classObject = Class.forName(className);
			if (IDomainObject.class.isAssignableFrom(classObject)) {

				// First locate an instance of the parent class.
				ITypedDao<IDomainObject> dao = mDaoProvider.getDaoInstance((Class<IDomainObject>) classObject);
				IDomainObject targetObject = dao.findByPersistentId(objectId);

				if (targetObject != null) {

					// Loop over all the arguments, setting each one.
					List<Class<?>> signatureClasses = new ArrayList<Class<?>>();
					List<Object> cookedArguments = new ArrayList<Object>();
					for (ArgsClass arg : mMethodArguments) {
						// (The method *must* start with "get" to ensure other methods don't get called.)
						String argumentName = arg.getName();
						Object argumentValue = arg.getValue();
						Class classType = Class.forName(arg.getClassType());
						signatureClasses.add(classType);

						Object typedArg;
						try {
							Constructor<?> ctor = classType.getConstructor(String.class);
							if (argumentValue == null) {
								typedArg = null;
							} else {
								typedArg = ctor.newInstance(new Object[] { argumentValue.toString() });
							}
							cookedArguments.add(typedArg);
						} catch (IllegalArgumentException e) {
							LOGGER.error("", e);
						} catch (InstantiationException e) {
							LOGGER.error("", e);
						} catch (IllegalAccessException e) {
							LOGGER.error("", e);
						} catch (InvocationTargetException e) {
							LOGGER.error("", e);
						}
					}

					Object methodResult = null;
					java.lang.reflect.Method method = classObject.getMethod(methodName, signatureClasses.toArray(new Class[0]));
					if (method != null) {
						try {
							methodResult = method.invoke(targetObject, cookedArguments.toArray(new Object[0]));
						} catch (IllegalArgumentException e) {
							LOGGER.error("", e);
						} catch (IllegalAccessException e) {
							LOGGER.error("", e);
						} catch (InvocationTargetException e) {
							LOGGER.error("", e);
						}
					}

					// Create the result JSon object.
					mapper = new ObjectMapper();
					ObjectNode dataNode = mapper.createObjectNode();
					JsonNode searchListNode = mapper.valueToTree(methodResult);
					dataNode.put(RESULTS, searchListNode);

					result = new WebSessionRespCmdObjectMethod(dataNode);
				}
			}
		} catch (SecurityException e) {
			LOGGER.error("", e);
		} catch (NoSuchMethodException e) {
			LOGGER.error("", e);
		} catch (JsonParseException e) {
			LOGGER.error("", e);
		} catch (JsonMappingException e) {
			LOGGER.error("", e);
		} catch (IOException e) {
			LOGGER.error("", e);
		} catch (ClassNotFoundException e) {
			LOGGER.error("", e);
		}

		return result;
	}

	/**
	 * Returns a list containing one parameter name for each argument accepted
	 * by the given constructor. If the class was compiled with debugging
	 * symbols, the parameter names will match those provided in the Java source
	 * code. Otherwise, a generic "arg" parameter name is generated ("arg0" for
	 * the first argument, "arg1" for the second...).
	 * 
	 * This method relies on the constructor's class loader to locate the
	 * bytecode resource that defined its class.
	 * 
	 * @param inMethod
	 * @return 
	 * @throws IOException
	 */
	public static List<String> getParameterNames(Method inMethod) throws IOException {
		Class<?> declaringClass = inMethod.getDeclaringClass();
		ClassLoader declaringClassLoader = declaringClass.getClassLoader();

		Type declaringType = Type.getType(declaringClass);
		String methodDescriptor = Type.getMethodDescriptor(inMethod);
		String url = declaringType.getInternalName() + ".class";

		InputStream classFileInputStream = declaringClassLoader.getResourceAsStream(url);
		if (classFileInputStream == null) {
			throw new IllegalArgumentException("The constructor's class loader cannot find the bytecode that defined the constructor's class (URL: " + url + ")");
		}

		ClassNode classNode;
		try {
			classNode = new ClassNode();
			ClassReader classReader = new ClassReader(classFileInputStream);
			classReader.accept((ClassVisitor) classNode, 0);
		} finally {
			classFileInputStream.close();
		}

		@SuppressWarnings("unchecked")
		List<MethodNode> methodNodes = classNode.methods;
		for (MethodNode methodNode : methodNodes) {
			if (methodNode.desc.equals(methodDescriptor)) {
				Type[] argumentTypes = Type.getArgumentTypes(methodNode.desc);
				List<String> parameterNames = new ArrayList<String>(argumentTypes.length);

				@SuppressWarnings("unchecked")
				List<LocalVariableNode> localVariables = methodNode.localVariables;
				for (int i = 0; i < argumentTypes.length; i++) {
					// The first local variable actually represents the "this" object
					parameterNames.add(localVariables.get(i + 1).name);
				}
				return parameterNames;
			}
		}
		return null;
	}
}
