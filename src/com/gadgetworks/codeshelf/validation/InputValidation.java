package com.gadgetworks.codeshelf.validation;

import javax.validation.ConstraintViolation;
import javax.validation.Path;
import javax.validation.metadata.ConstraintDescriptor;

import lombok.ToString;

@ToString(of={"property", "message"})
public class InputValidation<T> implements ConstraintViolation<T> {

	private T rootObject;
	private String property;
	private String message;
	private Object	invalidValue;

	public InputValidation(T rootObject, String property, String message, Object invalidValue) {
		super();
		this.rootObject = rootObject;
		this.property = property;
		this.message = message;
		this.invalidValue = invalidValue;
	}
	
	public String getProperty() {
		return property;
	}

	@Override
	public Object getInvalidValue() {
		return invalidValue;
	}
	
	@Override
	public ConstraintDescriptor<?> getConstraintDescriptor() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object[] getExecutableParameters() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object getExecutableReturnValue() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object getLeafBean() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getMessage() {
		return message;
	}

	@Override
	public String getMessageTemplate() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Path getPropertyPath() {
		return null;
	}

	@Override
	public T getRootBean() {
		return rootObject;
	}

	@SuppressWarnings("unchecked")
	@Override
	public Class<T> getRootBeanClass() {
		return (Class<T>) rootObject.getClass();
	}

	@Override
	public <U> U unwrap(Class<U> arg0) {
		// TODO Auto-generated method stub
		return null;
	}

}
