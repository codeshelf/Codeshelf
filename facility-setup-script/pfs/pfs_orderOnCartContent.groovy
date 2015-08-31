def OrderOnCartContent(orderId,cheId, customerId) {
	def fields = [
		'0073',
		'ORDERSTATUS'.padRight(20),
		'0'.padLeft(10,'0'),
		orderId.padRight(20),
		cheId.padRight(7),
		customerId.padRight(2),
		'OPEN'.padRight(15)
	];
	return fields.join('^');
}
