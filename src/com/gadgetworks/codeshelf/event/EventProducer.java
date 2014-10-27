package com.gadgetworks.codeshelf.event;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Objects;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;

/**
 * Helper class for producing events of concern at a business level and sent in an implementation specific way for collection and analysis.  
 * Tags help categorize the events instead of enforcing a single or rigid hierarchy of categories
 */
public class EventProducer {
	//Currently only this one implementation. The design is expected to evolve to extract "EventProducer" as an interface and make a different implementation
	//// For instance this would be a LoggingEventProducer if it is worthwhile to keep around
	
	//May need to turn tags into a set of enums
	
	private static final Logger LOGGER = LoggerFactory.getLogger(EventProducer.class);
	
	public static Set<String> tags(String... inTags) {
		return Sets.newHashSet(inTags);
	}

	public void produceSuccessEvent(Set<String> inTags, Object inRelatedObject) {
		produceViolationEvent(inTags, EventSeverity.INFO, null, inRelatedObject);
	}
	
	public void produceViolationEvent(Set<String> inTags, EventSeverity inSeverity, Exception inException, Object inRelatedObject) {
		produceEvent(EventInterval.INSTANTANEOUS, inTags, inSeverity, inException, inRelatedObject);
	}

	private void produceEvent(EventInterval inInterval, Set<String> inTags, EventSeverity inSeverity, Exception inException, Object inRelatedObject) {
		Map<String, ?> namedValues = Collections.emptyMap();
		if (inRelatedObject != null) {
			namedValues = ImmutableMap.of("relatedObject", inRelatedObject);
		}
		String logMessage = Objects.toStringHelper("Event")
			.add("tags", inTags)
			.add("severity", inSeverity)
			.add("interval", inInterval)
			.add("namedValues", namedValues).toString();
		
		if (inException != null) {
			if (inSeverity.equals(EventSeverity.WARN)) {
				LOGGER.warn(logMessage, inException);
			} else {
				LOGGER.error(logMessage, inException);
			}
		} else {
			LOGGER.info(logMessage);
		}
	}
	
	/**
	 * Produce an event that begins at some point in time and ends at some future point in time.
	 * This interval can be closed explicitly with the "end" method or utilizing resource autoclosing with:
	 * <code>
	 *  try(EventIntervalContext ctxt = produceEventInterval(tags("import", "file"), filepath)) {
	 *  
	 *   	...
	 *  }
	 *  </code>
	 *  Which will produce a begin and end event with the same event context information
	 * 
	 */
	public EventIntervalContext produceEventInterval(final Set<String> inTags, final EventSeverity inEventSeverity, final Object inRelatedObject) {
		produceEvent(EventInterval.BEGIN, inTags, inEventSeverity, null, inRelatedObject);

		return new EventIntervalContext() {
			
			@Override
			public void end() {
				produceEvent(EventInterval.END, inTags, inEventSeverity, null, inRelatedObject);
			}
		};
	}

	/**
	 *  An AutoCloseable context object that produces a corresponding END event for an event that had a BEGIN
	 */
	public static abstract class EventIntervalContext implements AutoCloseable {
		
		public abstract void end();
		
		@Override
		public void close() {
			end();
		}
	}


	
	/* Unused at the moment but planned for validation violations 
	private <T> void produceEvent(Set<ConstraintViolation<T>> violations, List<String> tags, T importBean) {
		String logMessage = Objects.toStringHelper("Event")
			.add("tags", tags)
			.add("interval", EventInterval.INSTANTANEOUS)
			//violations contains the bean that was violated
			.add("violations", violations).toString();
		LOGGER.error(logMessage);
	}
	*/
	
}
