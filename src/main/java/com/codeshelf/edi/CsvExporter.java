package com.codeshelf.edi;

import java.util.ArrayList;
import java.util.List;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import com.opencsv.CSVWriter;
import com.opencsv.bean.BeanToCsv;
import com.opencsv.bean.HeaderColumnNameMappingStrategy;
import com.opencsv.bean.MappingStrategy;

public class CsvExporter<ExportCsvBeanABC> {
	List<ExportCsvBeanABC>			mBeanList;

	@Accessors(prefix = "m")
	@Setter
	@Getter
	HeaderColumnNameMappingStrategy<ExportCsvBeanABC>	mStrategy;

	//public CsvExporter() {}

	public void setBeanList(List<ExportCsvBeanABC> inBeanList) {
		mBeanList = inBeanList;
	}

	/**
	 * 
	 */
	public void writeHeader(CSVWriter csvWriter) {		
		ArrayList<ExportCsvBeanABC> emptyList = new ArrayList<ExportCsvBeanABC>();	
		BeanToCsv<ExportCsvBeanABC> csv = new BeanToCsv<ExportCsvBeanABC>();
		csv.write((MappingStrategy<ExportCsvBeanABC>) getStrategy(), csvWriter, emptyList);
	}

	/**
	 * 
	 */
	public void writeRecords(CSVWriter csvWriter) {
		BeanToCsv<ExportCsvBeanABC> csv = new BeanToCsv<ExportCsvBeanABC>();
		csv.write((MappingStrategy<ExportCsvBeanABC>) getStrategy(), csvWriter, mBeanList);
	}

}
