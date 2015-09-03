def WorkInstructionExportCreateHeader(bean) {
	def fields = [
		'0073',
		'ORDERSTATUS'.padRight(20),
		'0'.padLeft(10,'0'),
		bean.orderId.padRight(20),
		'CLOSED'.padRight(15)
	]
	return fields.join('^');
}
