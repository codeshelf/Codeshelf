def WorkInstructionExportCreateHeader(orderId,cheId) {
	def returnStr = "" //
		+ "'0073' +'^'" //
		+ "+ 'ORDERSTATUS'.padRight(20) +'^'" //
		+ "+ '0'.padLeft(10,'0') +'^'" //
		+ "+ orderId.padRight(20) +'^'" //
		+ "+ 'CLOSED'.padRight(15);" //
    return returnStr;
}
