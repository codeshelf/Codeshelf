package com.gadgetworks.codeshelf.service;

public class ServiceFactory {

	public <T> T getServiceInstance(Class<T> classObject) {
		try {
			return classObject.newInstance();
		}
		catch(InstantiationException | IllegalAccessException e) {
			throw new IllegalArgumentException(classObject + " cannot be instantiated", e);
		}
	}

}
