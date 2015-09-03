package com.codeshelf.model.domain;

public interface ExportReceipt {

	
	
	public static class FailExportReceipt implements ExportReceipt {

		public FailExportReceipt(Throwable e) {
			// TODO Auto-generated constructor stub
		}

	}

	public static class UnhandledExportReceipt implements ExportReceipt {

	}

	public static class FileExportReceipt implements ExportReceipt {

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
	
	
}