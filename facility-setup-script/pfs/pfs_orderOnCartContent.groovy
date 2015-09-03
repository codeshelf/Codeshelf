def OrderOnCartContent(bean) {
	def fields = [
		'0073',
		'ORDERSTATUS'.padRight(20),
		'0'.padLeft(10,'0'),
		bean.orderId.padRight(20),
		bean.cheId.padRight(7),
		bean.customerId.padRight(2),
		'OPEN'.padRight(15)
	];
	return fields.join('^');
}
