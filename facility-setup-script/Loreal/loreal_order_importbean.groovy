def OrderImportBeanTransformation(bean) {
	needsScan = determineNeedsScan(bean.locationId);
	bean.needsScan = needsScan.toString();
	return bean;
}

def determineNeedsScan(locationId){
	if (locationId.length() < 5 || locationId[0] != "T"){
		return false;
	}
	tierId = locationId[4];
	return [1:"A",2:"C"].containsValue(tierId)
}
