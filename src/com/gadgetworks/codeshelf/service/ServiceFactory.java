package com.gadgetworks.codeshelf.service;

import java.util.HashMap;

import com.google.common.collect.Maps;
import com.google.inject.Inject;

public class ServiceFactory {

	private HashMap<Class<? extends IApiService>, IApiService>	mServices;
	
	@Inject
	public ServiceFactory(WorkService workService,
						  LightService lightService) {
		mServices = Maps.newHashMap();
		mServices.put(workService.getClass(), workService);
		mServices.put(lightService.getClass(), lightService);
	}
	
	@SuppressWarnings("unchecked")
	public <T extends IApiService> T getServiceInstance(Class<T> classObject) {
		IApiService service = mServices.get(classObject);
		return (T) service;
	}

}
