/*
 * Copyright 2002-2012 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.gadgetworks.codeshelf.validation;

import java.net.BindException;
import java.util.Collection;
import java.util.List;

/**
 * Stores and exposes information about data-binding and validation
 * errors for a specific object.
 *
 * <p>Field names can be properties of the target object (e.g. "name"
 * when binding to a customer object), or nested fields in case of
 * subobjects (e.g. "address.street"). Supports subtree navigation
 * via {@link #setNestedPath(String)}: for example, an
 * {@code AddressValidator} validates "address", not being aware
 * that this is a subobject of customer.
 *
 * <p>Note: {@code Errors} objects are single-threaded.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @see #setNestedPath
 * @see BindException
 * @see DataBinder
 * @see ValidationUtils
 */
public interface Errors {

	/**
	 * The separator between path elements in a nested path,
	 * for example in "customer.name" or "customer.address.street".
	 * <p>"." = same as the
	 * {@link org.springframework.beans.PropertyAccessor#NESTED_PROPERTY_SEPARATOR nested property separator}
	 * in the beans package.
	 */
	String NESTED_PATH_SEPARATOR = ".";


	/**
	 * Return the name of the bound root object.
	 */
	String getObjectName();

	/**
	 * Return if there were any errors.
	 */
	boolean hasErrors();

	/**
	 * Get all errors, both global and field ones.
	 * @return List of {@link ObjectError} instances
	 */
	List<ObjectError> getAllErrors();

	/**
	 * Get all errors associated with a field.
	 * @return a List of {@link FieldError} instances
	 */
	List<FieldError> getFieldErrors();

	
	/**
	 * Are there any errors associated with the given field?
	 * @param field the field name
	 * @return {@code true} if there were any errors associated with the given field
	 */
	boolean hasFieldErrors(String field);

	Collection<? extends ObjectError> getGlobalErrors();

	Collection<? extends FieldError> getFieldErrors(String field);

}
