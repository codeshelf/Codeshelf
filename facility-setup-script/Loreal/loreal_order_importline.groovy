def OrderImportLineTransformation(orderLine) {
	fields = orderLine.split(",")
	needsScan = determineNeedsScan(fields[7]);
    orderLine = orderLine + ", " + needsScan;
}

def determineNeedsScan(locationId){
	if (locationId.length() < 5 || locationId[0] != "T"){
		return false;
	}
	tierId = locationId[4];
	return [1:"A",2:"C"].containsValue(tierId)
}
