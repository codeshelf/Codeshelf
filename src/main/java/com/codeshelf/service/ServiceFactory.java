package com.codeshelf.service;

import java.util.HashMap;

import com.google.common.collect.Maps;
import com.google.inject.Inject;

public class ServiceFactory {

	private HashMap<Class<? extends IApiService>, IApiService>	mServices;

	@Inject
	public ServiceFactory(WorkService workService, LightService lightService, PropertyService propertyService, UiUpdateService uiUpdateService) {
		mServices = Maps.newHashMap();
		mServices.put(WorkService.class, workService);
		mServices.put(LightService.class, lightService);
		mServices.put(PropertyService.class, propertyService);
		mServices.put(UiUpdateService.class, uiUpdateService);
	}

	@SuppressWarnings("unchecked")
	public <T extends IApiService> T getServiceInstance(Class<T> classObject) {
		IApiService service = mServices.get(classObject);
		return (T) service;
	}

}