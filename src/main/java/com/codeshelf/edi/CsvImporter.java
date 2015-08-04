package com.codeshelf.edi;

import java.io.IOException;
import java.io.Reader;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import com.codeshelf.event.EventProducer;
import com.codeshelf.event.EventSeverity;
import com.codeshelf.event.EventTag;
import com.codeshelf.validation.DefaultErrors;
import com.codeshelf.validation.ErrorCode;
import com.codeshelf.validation.InputValidationException;
import com.opencsv.CSVReader;
import com.opencsv.bean.CsvToBean;
import com.opencsv.bean.HeaderColumnNameMappingStrategy;

public abstract class CsvImporter<T> {

	private final EventProducer	mEventProducer;

	public CsvImporter(EventProducer inProducer) {
		mEventProducer = inProducer;
	}

	protected List<T> toCsvBean(Reader inCsvReader, Class<T> inClass) {
		List<T> crossBatchBeanList = Collections.emptyList();
		try (CSVReader csvReader = new CSVReader(inCsvReader)) {
			HeaderColumnNameMappingStrategy<T> strategy = new HeaderColumnNameMappingStrategy<T>();
			strategy.setType(inClass);

			CsvToBean<T> csv = new CsvToBean<T>();
			crossBatchBeanList = csv.parse(strategy, csvReader);
		} catch (IOException e) {
			throw new EdiFileReadException("unable to import csv with bean class: " + inClass, e);
		}
		
		int lineNumber = 2;
		for (T bean : crossBatchBeanList) {
			if (bean instanceof ImportCsvBeanABC){
				((ImportCsvBeanABC) bean).setLineNumber(lineNumber++);
			}
		}
		return crossBatchBeanList;
	}

	protected void produceRecordSuccessEvent(Object inRelatedObject) {
		mEventProducer.produceEvent(getEventTagsForImporter(), EventSeverity.INFO, inRelatedObject);
	}

	protected void produceRecordViolationEvent(EventSeverity inSeverity, List<?> violations, Object inRelatedObject) {
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

	protected void produceRecordViolationEvent(Object inRelatedObject,
		String inFieldName,
		Object inRejectedValue,
		ErrorCode inErrorCode) {
		DefaultErrors errors = new DefaultErrors(inRelatedObject.getClass());
		errors.rejectValue(inFieldName, inRejectedValue, inErrorCode);
		mEventProducer.produceViolationEvent(getEventTagsForImporter(), EventSeverity.WARN, errors, inRelatedObject);
	}

	protected abstract Set<EventTag> getEventTagsForImporter();

	public int toInteger(final String inString) {
		// Integer.valueOf will throw a NumberFormatException if anything at all is wrong with the string. 
		// Let's clean up the obvious. But this still throws NumberFormatException for something like "09x " and many other bad numbers.
		
		String cleanString = inString;
		cleanString = cleanString.trim();
		// would also want leading zeros removed, but may need to leave one 0 for 0000.
		cleanString = cleanString.replaceFirst("^0+(?!$)", ""); 
		return Integer.valueOf(cleanString);

	}

}
