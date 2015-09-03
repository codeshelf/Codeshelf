def WorkInstructionExportCreateTrailer(bean) {
	def fields = [
		'0057',
		'ENDORDER'.padRight(20),
		'0'.padLeft(10,'0'),
		bean.orderId.padRight(20),
		bean.worker
	]
	return fields.join('^');
}
