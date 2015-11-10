def WorkInstructionExportContent(bean) {
	def fields = [
		'0090',
		'PICKMISSIONSTATUS'.padRight(20),
		'0'.padLeft(10,'0'),
		bean.orderId.padRight(20),
		bean.locationId.padRight(20),
		bean.planQuantity.padLeft(15,'0'),
		bean.actualQuantity.padLeft(15,'0'),
		bean.itemId.padRight(25),
		bean.assigned,
		bean.completed,
		bean.orderId.padRight(20),
		padRightAndTruncateTo(bean.badge, 11),
		padRightAndTruncateTo(bean.worker, 11)
	];
	return fields.join('^');
}

def padRightAndTruncateTo(str, size) {
	if (str.length() >= size){
		return str.substring(0, size);
	} else {
		return str.padRight(size);
	}
}
