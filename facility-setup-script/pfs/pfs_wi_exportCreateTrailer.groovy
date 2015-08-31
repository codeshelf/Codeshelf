def WorkInstructionExportCreateTrailer(orderId,cheId) {
	def fields = [
		'0057',
		'ENDORDER'.padRight(20),
		'0'.padLeft(10,'0'),
		orderId.padRight(20)
	]
	return fields.join('^');
}
