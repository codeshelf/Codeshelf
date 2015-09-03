package com.codeshelf.model.domain;

public class FileExportReceipt implements ExportReceipt {

	private String	absoluteFilename;
	private int	fileLength;

	public FileExportReceipt(String absoluteFilename, int fileLength) {
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