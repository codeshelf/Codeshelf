package com.codeshelf.behavior;

import java.util.HashMap;

import com.google.common.collect.Maps;
import com.google.inject.Inject;

public class BehaviorFactory { //BehaviorFactory

	private HashMap<Class<? extends IApiBehavior>, IApiBehavior> mBehaviors;

	@Inject
	public BehaviorFactory(WorkBehavior workBehavior,
		LightBehavior lightBehavior,
		UiUpdateBehavior uiUpdateBehavior,
		OrderBehavior orderBehavior,
		InventoryBehavior inventoryService,
		NotificationBehavior notificationBehavior,
		InfoBehavior infoBehavior,
		PalletizerBehavior palletizerBehavior,
		WorkerHourlyMetricBehavior workerHourlyMetricBehavior,
		PropertyBehavior propertyBehaviorNew) {
		mBehaviors = Maps.newHashMap();
		mBehaviors.put(WorkBehavior.class, workBehavior);
		mBehaviors.put(LightBehavior.class, lightBehavior);
		mBehaviors.put(UiUpdateBehavior.class, uiUpdateBehavior);
		mBehaviors.put(OrderBehavior.class, orderBehavior);
		mBehaviors.put(InventoryBehavior.class, inventoryService);
		mBehaviors.put(NotificationBehavior.class, notificationBehavior);
		mBehaviors.put(InfoBehavior.class, infoBehavior);
		mBehaviors.put(PalletizerBehavior.class, palletizerBehavior);
		mBehaviors.put(WorkerHourlyMetricBehavior.class, workerHourlyMetricBehavior);
		//TestBehavior 
	}

	@SuppressWarnings("unchecked")
	public <T extends IApiBehavior> T getInstance(Class<T> classObject) {
		IApiBehavior behavior = mBehaviors.get(classObject);
		return (T) behavior;
	}
}