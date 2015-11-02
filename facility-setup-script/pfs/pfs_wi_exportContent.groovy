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
		bean.pickerId.padRight(11),
		bean.pickerName.padRight(8)
	];
	return fields.join('^');
}
