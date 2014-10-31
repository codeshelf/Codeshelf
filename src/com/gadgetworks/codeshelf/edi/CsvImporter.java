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
import com.gadgetworks.codeshelf.event.EventTag;
import com.gadgetworks.codeshelf.validation.DefaultErrors;
import com.gadgetworks.codeshelf.validation.ErrorCode;
import com.gadgetworks.codeshelf.validation.InputValidationException;
import com.gadgetworks.codeshelf.validation.Violation;

public abstract class CsvImporter<T> {

	private final EventProducer	mEventProducer;

	public CsvImporter(EventProducer inProducer) {
		mEventProducer = inProducer;
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
		mEventProducer.produceEvent(getEventTagsForImporter(), EventSeverity.INFO, inRelatedObject);
	}

	protected void produceRecordViolationEvent(EventSeverity inSeverity, List<Violation> violations, Object inRelatedObject) {
		mEventProducer.produceViolationEvent(getEventTagsForImporter(), inSeverity, violations, inRelatedObject);
	}

	protected void produceRecordViolationEvent(EventSeverity inSeverity, InputValidationException e, Object inRelatedObject) {
		mEventProducer.produceViolationEvent(getEventTagsForImporter(), inSeverity, e.getErrors(), inRelatedObject);
	}
	
	protected void produceRecordViolationEvent(EventSeverity inSeverity, Exception e, Object inRelatedObject) {
		mEventProducer.produceViolationEvent(getEventTagsForImporter(), inSeverity, e, inRelatedObject);
	}

	protected void produceRecordViolationEvent(Object inRelatedObject, String generalMessage) {
		DefaultErrors errors = new DefaultErrors(inRelatedObject.getClass());
		errors.reject(ErrorCode.GENERAL, generalMessage);
		mEventProducer.produceViolationEvent(getEventTagsForImporter(), EventSeverity.WARN, errors, inRelatedObject);
	}

	protected void produceRecordViolationEvent(Object inRelatedObject, String inFieldName, Object inRejectedValue, ErrorCode inErrorCode) {
		DefaultErrors errors = new DefaultErrors(inRelatedObject.getClass());
		errors.rejectValue(inFieldName, inRejectedValue, inErrorCode);
		mEventProducer.produceViolationEvent(getEventTagsForImporter(), EventSeverity.WARN, errors, inRelatedObject);
	}
	
	protected abstract Set<EventTag> getEventTagsForImporter();

}
