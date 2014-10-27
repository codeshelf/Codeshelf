package com.gadgetworks.codeshelf.edi;

import java.io.IOException;
import java.io.Reader;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import au.com.bytecode.opencsv.CSVReader;
import au.com.bytecode.opencsv.bean.CsvToBean;
import au.com.bytecode.opencsv.bean.HeaderColumnNameMappingStrategy;

import com.gadgetworks.codeshelf.event.EventProducer;
import com.gadgetworks.codeshelf.event.EventSeverity;

public abstract class CsvImporter<T> {

	private final EventProducer	mEventProducer;

	public CsvImporter() {
		mEventProducer = new EventProducer();
	}

	protected List<T> toCsvBean(Reader inCsvReader, Class<T> inClass) {
		List<T> crossBatchBeanList = Collections.emptyList();
		try(CSVReader csvReader = new CSVReader(inCsvReader)) {
			HeaderColumnNameMappingStrategy<T> strategy = new HeaderColumnNameMappingStrategy<T>();
			strategy.setType(inClass);
	
			CsvToBean<T> csv = new CsvToBean<T>();
			crossBatchBeanList = csv.parse(strategy, csvReader);
		} catch(IOException e) {
			throw  new EdiFileReadException("unable to import csv with bean class: " + inClass, e);
		}
		return crossBatchBeanList;
	}

	protected void produceRecordSuccessEvent(Object inRelatedObject) {
		mEventProducer.produceSuccessEvent(getEventTagsForImporter(), inRelatedObject);
	}

	protected void produceRecordViolationEvent(EventSeverity inSeverity, Exception e, Object inRelatedObject) {
		mEventProducer.produceViolationEvent(getEventTagsForImporter(), inSeverity, e, inRelatedObject);
	}
	
	protected Set<String> getEventTagsForImporter() {
		String classTag = this.getClass().getSimpleName().toLowerCase().replace("csvimporter", "");
		return EventProducer.tags("import", classTag);
	}
}
