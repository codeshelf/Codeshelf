package com.gadgetworks.codeshelf.validation;

import java.util.Arrays;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.base.Objects;
import com.google.common.base.Preconditions;

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

/**
 * Encapsulates a field error, that is, a reason for rejecting a specific
 * field value.
 *
 * <p>See the {@link DefaultMessageCodesResolver} javadoc for details on
 * how a message code list is built for a {@code FieldError}.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @since 10.03.2003
 * @see DefaultMessageCodesResolver
 */
@SuppressWarnings("serial")
@JsonAutoDetect(fieldVisibility = Visibility.NONE, getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE)
public class FieldError extends ObjectError {

	private final String field;

	private final Object rejectedValue;

	@JsonIgnore
	private final boolean bindingFailure;


	/**
	 * Create a new FieldError instance.
	 * @param objectName the name of the affected object
	 * @param field the affected field of the object
	 * @param defaultMessage the default message to be used to resolve this message
	 */
	public FieldError(String objectName, String field, Object rejectedValue, String defaultMessage) {
		this(objectName, field, rejectedValue, false, null, null, defaultMessage);
	}

	/**
	 * Create a new FieldError instance.
	 * @param objectName the name of the affected object
	 * @param field the affected field of the object
	 * @param defaultMessage the default message to be used to resolve this message
	 */
	public FieldError(String objectName, String field, Object rejectedValue, ErrorCode errorCode) {
		this(objectName, field, rejectedValue, false, new ErrorCode[]{errorCode}, null, errorCode.toDefaultMessage(field, rejectedValue));
	}
	
	/**
	 * Create a new FieldError instance.
	 * @param objectName the name of the affected object
	 * @param field the affected field of the object
	 * @param rejectedValue the rejected field value
	 * @param bindingFailure whether this error represents a binding failure
	 * (like a type mismatch); else, it is a validation failure
	 * @param codes the codes to be used to resolve this message
	 * @param arguments the array of arguments to be used to resolve this message
	 * @param defaultMessage the default message to be used to resolve this message
	 */
	public FieldError(
			String objectName, String field, Object rejectedValue, boolean bindingFailure,
			ErrorCode[] codes, Object[] arguments, String defaultMessage) {

		super(objectName, codes, arguments, defaultMessage);
		Preconditions.checkNotNull(field, "Field must not be null");
		this.field = field;
		this.rejectedValue = rejectedValue;
		this.bindingFailure = bindingFailure;
	}


	/**
	 * Return the affected field of the object.
	 */
	public String getField() {
		return this.field;
	}

	/**
	 * Return the rejected field value.
	 */
	public Object getRejectedValue() {
		return this.rejectedValue;
	}

	/**
	 * Return whether this error represents a binding failure
	 * (like a type mismatch); otherwise it is a validation failure.
	 */
	public boolean isBindingFailure() {
		return this.bindingFailure;
	}

	@Override
	public String toString() {
		return Objects.toStringHelper(this)
			.add("field", getField())
			.add("codes", Arrays.toString(getCodes()))
			.add("message", getMessage())
			.toString();
	}
	
	@Override
	public boolean equals(Object other) {
		if (this == other) {
			return true;
		}
		if (!super.equals(other)) {
			return false;
		}
		FieldError otherError = (FieldError) other;
		return getField().equals(otherError.getField()) &&
				Objects.equal(getRejectedValue(), otherError.getRejectedValue()) &&
				isBindingFailure() == otherError.isBindingFailure();
	}

	@Override
	public int hashCode() {
		int hashCode = super.hashCode();
		hashCode = 29 * hashCode + getField().hashCode();
		hashCode = 29 * hashCode + Objects.hashCode(getRejectedValue());
		hashCode = 29 * hashCode + (isBindingFailure() ? 1 : 0);
		return hashCode;
	}

}
