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

package com.codeshelf.validation;

import java.io.Serializable;

import lombok.ToString;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Preconditions;

/**
 * Encapsulates an object error, that is, a global reason for rejecting
 * an object.
 *
 * <p>See the {@link DefaultMessageCodesResolver} javadoc for details on
 * how a message code list is built for an {@code ObjectError}.
 *
 * @author Juergen Hoeller
 * @see FieldError
 * @see DefaultMessageCodesResolver
 * @since 10.03.2003
 */
@SuppressWarnings("serial")
@ToString()
@JsonAutoDetect(fieldVisibility = Visibility.NONE, getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE)
public class ObjectError implements Serializable {

	private final String objectName;
	
	private ErrorCode[]	codes;
	private Object[]	arguments;

	private String	defaultMessage;


	/**
	 * Create a new instance of the ObjectError class.
	 * @param objectName the name of the affected object
	 * @param defaultMessage the default message to be used to resolve this message
	 */
	public ObjectError(String objectName, String defaultMessage) {
		this(objectName, null, null, defaultMessage);
	}

	
	/**
	 * Create a new instance of the ObjectError class.
	 * @param objectName the name of the affected object
	 * @param codes the codes to be used to resolve this message
	 * @param arguments	the array of arguments to be used to resolve this message
	 * @param defaultMessage the default message to be used to resolve this message
	 */
	public ObjectError(String objectName, ErrorCode[] codes, Object[] arguments, String defaultMessage) {
		Preconditions.checkNotNull(objectName, "Object name must not be null");
		this.objectName = objectName;
		this.codes = codes;
		this.arguments = arguments;
		this.defaultMessage = defaultMessage;
	}

	@JsonProperty
	public String getMessage() {
		return defaultMessage;
	}
	
	/**
	 * Return the name of the affected object.
	 */
	public String getObjectName() {
		return this.objectName;
	}

	@Override
	public boolean equals(Object other) {
		if (other == null) {
			return false;
		}
		if (this == other) {
			return true;
		}
		if (!(getClass().equals(other.getClass())) || !super.equals(other)) {
			return false;
		}
		ObjectError otherError = (ObjectError) other;
		return getObjectName().equals(otherError.getObjectName());
	}

	@Override
	public int hashCode() {
		return super.hashCode() * 29 + getObjectName().hashCode();
	}
	
	protected ErrorCode[] getCodes() {
		return this.codes;
	}

}
