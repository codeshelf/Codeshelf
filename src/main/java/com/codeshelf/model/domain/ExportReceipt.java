package com.codeshelf.model.domain;

public class ExportReceipt {

	private String	absoluteFilename;
	private int	fileLength;

	public ExportReceipt(String absoluteFilename, int fileLength) {
		this.absoluteFilename = absoluteFilename;
		this.fileLength = fileLength; 
	}

	public String getPath() {
		return absoluteFilename;
	}

	public int getFileLength() {
		return fileLength;
	}
}