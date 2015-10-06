package com.codeshelf.service;

import java.util.HashMap;

import com.google.common.collect.Maps;
import com.google.inject.Inject;

public class BehaviorFactory { //BehaviorFactory

	private HashMap<Class<? extends IApiBehavior>, IApiBehavior> mServices;

	@Inject
	public BehaviorFactory(WorkBehavior workService,
		LightBehavior lightService,
		IPropertyBehavior propertyService,
		UiUpdateBehavior uiUpdateService,
		OrderBehavior orderService,
		InventoryBehavior inventoryService,
		NotificationBehavior notificationService,
		InfoBehavior infoService,
		PalletizerBehavior palletizerService) {
		mServices = Maps.newHashMap();
		mServices.put(WorkBehavior.class, workService); //WorkBehavior
		mServices.put(LightBehavior.class, lightService); //LightBehavior
		mServices.put(PropertyService.class, propertyService);  //PropertyBehavior
		mServices.put(UiUpdateBehavior.class, uiUpdateService); //UiUpdateBehavior
		mServices.put(OrderBehavior.class, orderService); //OrderBehavior
		mServices.put(InventoryBehavior.class, inventoryService); //InventoryBehavior
		mServices.put(NotificationBehavior.class, notificationService); //NotificationBehavior
		mServices.put(InfoBehavior.class, infoService); //InfoBehavior
		mServices.put(PalletizerBehavior.class, palletizerService); //PalletizerBehavior
		//TestBehavior 
	}

	@SuppressWarnings("unchecked")
	public <T extends IApiBehavior> T getServiceInstance(Class<T> classObject) {
		IApiBehavior service = mServices.get(classObject);
		return (T) service;
	}
}