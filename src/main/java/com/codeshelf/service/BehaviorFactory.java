package com.codeshelf.service;

import java.util.HashMap;

import com.google.common.collect.Maps;
import com.google.inject.Inject;

public class BehaviorFactory { //BehaviorFactory

	private HashMap<Class<? extends IApiBehavior>, IApiBehavior> mBehaviors;

	@Inject
	public BehaviorFactory(WorkBehavior workBehavior,
		LightBehavior lightBehavior,
		IPropertyBehavior propertyBehavior,
		UiUpdateBehavior uiUpdateBehavior,
		OrderBehavior orderBehavior,
		InventoryBehavior inventoryService,
		NotificationBehavior notificationBehavior,
		InfoBehavior infoBehavior,
		PalletizerBehavior palletizerBehavior) {
		mBehaviors = Maps.newHashMap();
		mBehaviors.put(WorkBehavior.class, workBehavior); //WorkBehavior
		mBehaviors.put(LightBehavior.class, lightBehavior); //LightBehavior
		mBehaviors.put(PropertyService.class, propertyBehavior);  //PropertyBehavior
		mBehaviors.put(UiUpdateBehavior.class, uiUpdateBehavior); //UiUpdateBehavior
		mBehaviors.put(OrderBehavior.class, orderBehavior); //OrderBehavior
		mBehaviors.put(InventoryBehavior.class, inventoryService); //InventoryBehavior
		mBehaviors.put(NotificationBehavior.class, notificationBehavior); //NotificationBehavior
		mBehaviors.put(InfoBehavior.class, infoBehavior); //InfoBehavior
		mBehaviors.put(PalletizerBehavior.class, palletizerBehavior); //PalletizerBehavior
		//TestBehavior 
	}

	@SuppressWarnings("unchecked")
	public <T extends IApiBehavior> T getInstance(Class<T> classObject) {
		IApiBehavior behavior = mBehaviors.get(classObject);
		return (T) behavior;
	}
}