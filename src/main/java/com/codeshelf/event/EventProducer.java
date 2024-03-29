package com.codeshelf.event;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.validation.Errors;
import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableMap;

/**
 * Helper class for producing events of concern at a business level and sent in an implementation specific way for collection and analysis.  
 * Tags help categorize the events instead of enforcing a single or rigid hierarchy of categories
 */
public class EventProducer {
	//Currently only this one implementation. The design is expected to evolve to extract "EventProducer" as an interface and make a different implementation
	//// For instance this would be a LoggingEventProducer if it is worthwhile to keep around

	private static final Logger	LOGGER	= LoggerFactory.getLogger(EventProducer.class);

	public void produceEvent(Set<EventTag> inTags, EventSeverity inEventSeverity, Object inRelatedObject) {
		produceEvent(EventInterval.INSTANTANEOUS, inTags, inEventSeverity, inRelatedObject);
	}

	public void produceViolationEvent(Set<EventTag> inTags, EventSeverity inSeverity, Exception inException, Object inRelatedObject) {
		produceExceptionEvent(EventInterval.INSTANTANEOUS, inTags, inSeverity, inException, inRelatedObject);
	}

	public void produceViolationEvent(Set<EventTag> inTags, EventSeverity inSeverity, Errors inErrors, Object inRelatedObject) {
		produceErrorsEvent(EventInterval.INSTANTANEOUS, inTags, inSeverity, inErrors, inRelatedObject);
	}

	public void produceViolationEvent(Set<EventTag> inTags, EventSeverity inSeverity, List<?> inViolations, Object inRelatedObject) {
		produceErrorsEvent(EventInterval.INSTANTANEOUS, inTags, inSeverity, inViolations, inRelatedObject);
	}

	private void produceErrorsEvent(EventInterval inInterval,
		Set<EventTag> inTags,
		EventSeverity inSeverity,
		Object inErrors,
		Object inRelatedObject) {
		MoreObjects.ToStringHelper messageHelper = baseEventSerialization(inInterval, inTags, inSeverity, inRelatedObject);

		String logMessage = messageHelper.add("violations", inErrors).toString();
		log(inSeverity, logMessage);
	}

	private void produceExceptionEvent(EventInterval inInterval,
		Set<EventTag> inTags,
		EventSeverity inSeverity,
		Exception inException,
		Object inRelatedObject) {
		MoreObjects.ToStringHelper messageHelper = baseEventSerialization(inInterval, inTags, inSeverity, inRelatedObject);
		String logMessage = messageHelper.toString();

		if (inException != null) {
			if (inSeverity.equals(EventSeverity.WARN)) {
				LOGGER.warn(logMessage, inException);
			} else {
				LOGGER.error(logMessage, inException);
			}
		} else {
			if (inSeverity.equals(EventSeverity.WARN)) {
				LOGGER.warn(logMessage, inException);
			} else if (inSeverity.equals(EventSeverity.ERROR)) {
				LOGGER.error(logMessage, inException);
			} else {
				LOGGER.info(logMessage);
			}
		}
	}

	/**
	 * An obvious goal is to log at the level of the severity. We don't have that hooked up very well. 
	 * This first kludgy bit for DEV-1261
	 */
	private void produceEvent(EventInterval inInterval, Set<EventTag> inTags, EventSeverity inSeverity, Object inRelatedObject) {
		// DEV-1261 from v24, these events are DEBUG level
		// Let's skip the extraneous processing if we will not log it. If it comes in as DEBUG, don't log unless this logger is set to debug.
		if (!inSeverity.equals(EventSeverity.DEBUG) || LOGGER.isDebugEnabled()) {
			MoreObjects.ToStringHelper messageHelper = baseEventSerialization(EventInterval.INSTANTANEOUS,
				inTags,
				inSeverity,
				inRelatedObject);
			// here we would want to call LOGGER.info, LOGGER.warn, etc based on inSeverity.
			LOGGER.debug(messageHelper.toString());
		}
	}

	private void log(EventSeverity inSeverity, String logMessage) {
		if (inSeverity.equals(EventSeverity.WARN)) {
			LOGGER.warn(logMessage);
		} else if (inSeverity.equals(EventSeverity.ERROR)) {
			LOGGER.error(logMessage);
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
	public EventIntervalContext produceEventInterval(final Set<EventTag> inTags,
		final EventSeverity inEventSeverity,
		final Object inRelatedObject) {
		produceEvent(EventInterval.BEGIN, inTags, inEventSeverity, inRelatedObject);

		return new EventIntervalContext() {

			@Override
			public void end() {
				produceEvent(EventInterval.END, inTags, inEventSeverity, inRelatedObject);
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

	private MoreObjects.ToStringHelper baseEventSerialization(EventInterval inInterval,
		Set<EventTag> inTags,
		EventSeverity inSeverity,
		Object inRelatedObject) {
		Map<String, ?> namedValues = Collections.emptyMap();
		if (inRelatedObject != null) {
			namedValues = ImmutableMap.of("relatedObject", inRelatedObject);
		}
		return MoreObjects.toStringHelper("Event")
			.add("tags", inTags)
			.add("severity", inSeverity)
			.add("interval", inInterval)
			.add("namedValues", namedValues);
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
