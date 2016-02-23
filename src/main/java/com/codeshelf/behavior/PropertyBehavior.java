package com.codeshelf.behavior;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Restrictions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.flyweight.command.ColorEnum;
import com.codeshelf.model.FacilityPropertyType;
import com.codeshelf.model.HousekeepingInjector.BayChangeChoice;
import com.codeshelf.model.HousekeepingInjector.RepeatPosChoice;
import com.codeshelf.model.domain.Facility;
import com.codeshelf.model.domain.FacilityProperty;
import com.codeshelf.validation.DefaultErrors;
import com.codeshelf.validation.ErrorCode;
import com.codeshelf.validation.InputValidationException;

public class PropertyBehavior{
	private static final Logger			LOGGER				= LoggerFactory.getLogger(PropertyBehavior.class);
	
	public static String getProperty(Facility facility, FacilityPropertyType type){
		FacilityProperty property = retrieveProperty(facility, type);
		if (property == null) {
			return type.getDefaultValue();
		} else {
			return property.getValue();
		}
	}
	
	public static boolean getPropertyAsBoolean(Facility inFacility, FacilityPropertyType type) {
		String property = getProperty(inFacility, type);
		return Boolean.parseBoolean(property);
	}

	public static ColorEnum getPropertyAsColor(Facility facility, FacilityPropertyType type, ColorEnum defaultColor) {
		String property = getProperty(facility, type);
		if (property == null) {
			return defaultColor;
		}
		return ColorEnum.valueOf(property);
	}

	public static int getPropertyAsInt(Facility facility, FacilityPropertyType type, int defaultValue) {
		String property = getProperty(facility, type);
		return Integer.parseInt(property);
	}
	
	public static void setProperty(Facility facility, FacilityPropertyType type, String value) {
		// The UI may have passed a string that is close enough. But we want to force it to our canonical forms.
		String canonicalForm = toCanonicalForm(type, value);
		String validationResult = validateNewStringValue(type, value, canonicalForm);
		// null means no error
		if (validationResult != null) {
			LOGGER.warn("Property validation rejection: " + validationResult);
			DefaultErrors errors = new DefaultErrors(PropertyBehavior.class);
			String instruction = type.name() + " valid values: " + type.getValidValues();
			errors.rejectValue(value, instruction, ErrorCode.FIELD_INVALID); // "{0} invalid. {1}"
			throw new InputValidationException(errors);
		}

		// storing the string version, so type does not matter. We assume all validation happened so the value is ok to go to the database.
		FacilityProperty property = retrieveProperty(facility, type);
		if (property == null) {
			property = new FacilityProperty(facility, type);
		}
		property.setValue(canonicalForm);
		FacilityProperty.staticGetDao().store(property);			
	}
		
	public static List<FacilityProperty> getAllProperties(Facility facility){
		List<FacilityProperty> properties = new ArrayList<>();
		FacilityProperty property = null;
		for (FacilityPropertyType type : FacilityPropertyType.values()){
			property = retrieveProperty(facility, type);
			if (property == null) {
				property = new FacilityProperty(facility, type);
			}
			property.updateDefaultValue();
			properties.add(property);
		}
		return properties;
	}
	
	public static void restoreHKDefaults(Facility inFacility) {
		setRepeatPosChoice(inFacility, RepeatPosChoice.RepeatPosContainerOnly);
		setBayChangeChoice(inFacility, BayChangeChoice.BayChangeBayChange);
	}

	public static void turnOffHK(Facility facility) {
		setRepeatPosChoice(facility, RepeatPosChoice.RepeatPosNone);
		setBayChangeChoice(facility, BayChangeChoice.BayChangeNone);
	}
	
	private static FacilityProperty retrieveProperty(Facility facility, FacilityPropertyType type){
		List<Criterion> filterParams = new ArrayList<Criterion>();
		filterParams.add(Restrictions.eq("parent", facility));
		filterParams.add(Restrictions.eq("name", type.name()));
		List<FacilityProperty> properties = FacilityProperty.staticGetDao().findByFilter(filterParams);
		int size = properties.size();
		if (size > 1) {
			LOGGER.warn("{} {} properies found for facility {}", size, type.name(), facility.getDomainId());
		}
		return size == 0 ? null : properties.get(0);
	}

	private static void setBayChangeChoice(Facility facility, BayChangeChoice inBayChangeChoice) {
		switch(inBayChangeChoice){
			case BayChangeNone:
				setProperty(facility, FacilityPropertyType.BAYCHANG, "None");
				break;
			case BayChangeBayChange:
				setProperty(facility, FacilityPropertyType.BAYCHANG, "BayChange");
				break;
			case BayChangePathSegmentChange:
				setProperty(facility, FacilityPropertyType.BAYCHANG, "PathSegmentChange");
				break;
			case BayChangeExceptSamePathDistance:
				setProperty(facility, FacilityPropertyType.BAYCHANG, "BayChangeExceptAcrossAisle");
				break;
			default:
				LOGGER.error("unknown value in setBayChangeChoice");
		}
	}
	
	private static void setRepeatPosChoice(Facility facility, RepeatPosChoice inRepeatPosChoice) {
		switch(inRepeatPosChoice){
			case RepeatPosNone:
				setProperty(facility, FacilityPropertyType.RPEATPOS, "None");
				break;
			case RepeatPosContainerOnly:
				setProperty(facility, FacilityPropertyType.RPEATPOS, "ContainerOnly");
				break;
			case RepeatPosContainerAndCount:
				setProperty(facility, FacilityPropertyType.RPEATPOS, "ContainerAndCount");
				break;
			default:
				LOGGER.error("unknown value in setRepeatPosChoice");
		}
	}

	/**
	 * Converts things like "baychange" to "BayChange" or "Red" to "RED".
	 * Return null if there is no likely match.
	 */
	public static String toCanonicalForm(FacilityPropertyType type, String inValue) {
		if (inValue == null)
			return null;
		String trimmedValue = inValue.trim();
		if (trimmedValue.isEmpty())
			return null;
		// Find out which one we are
		switch (type){
			case BAYCHANG:
				return validate_baychang(trimmedValue);
			case RPEATPOS:
				return validate_rpeatpos(trimmedValue);
			case WORKSEQR:
				return validate_workseqr(trimmedValue);
			case LIGHTSEC:
				return validate_integer_in(trimmedValue, 2, 30); // mininum 2, max 30
			case LIGHTCLR:
				return validate_color_not_black(trimmedValue);
			case CROSSBCH:
				return validate_boolean(trimmedValue);
			case AUTOSHRT:
				return validate_boolean(trimmedValue);
			case LOCAPICK:
				return validate_boolean(trimmedValue);
			case EACHMULT:
				return validate_boolean(trimmedValue);
			case PICKINFO:
				return validate_pickinfo(trimmedValue);
			case CNTRTYPE:
				return validate_containertype(trimmedValue);
			case SCANPICK:
				return validate_scantype(trimmedValue);
			case PICKMULT:
				return validate_boolean(trimmedValue);
			case INVTMENU:
				return validate_boolean(trimmedValue);
			case BADGEAUTH:
				return validate_boolean(trimmedValue);
			case PRODUCTION:
				return validate_boolean(trimmedValue);
			case PROTOCOL:
				return validate_protocol(trimmedValue);
			case ORDERSUB:
				return validate_ordersub(trimmedValue);
			case TIMEZONE:
				return validate_timezone(trimmedValue);
			default:
				LOGGER.error("new DomainObjectProperty: " + type.name() + " has no toCanonicalForm implementation");
		}
		return null;
	}
	
	private static String validate_scantype(String inValue) {
		// valid values are "Disabled, SKU, UPC"
		final String disabled = "Disabled";
		final String sku = "SKU";
		final String upc = "UPC";
		
		if (inValue.equalsIgnoreCase(disabled))
			return disabled;
		else if (inValue.equalsIgnoreCase(sku))
			return sku;
		else if (inValue.equalsIgnoreCase(upc))
			return upc;
		else
			return null;
	}

	private static String validate_baychang(String inValue) {
		// valid values are "None, BayChange, BayChangeExceptAcrossAisle, PathSegmentChange"
		final String noneStr = "None";
		final String bayStr = "BayChange";
		final String aisleStr = "BayChangeExceptAcrossAisle";
		final String pathStr = "PathSegmentChange";
		String returnStr = inValue;
		if (returnStr.equalsIgnoreCase(noneStr))
			return noneStr;
		else if (returnStr.equalsIgnoreCase(bayStr))
			return bayStr;
		else if (returnStr.equalsIgnoreCase(aisleStr))
			return aisleStr;
		else if (returnStr.equalsIgnoreCase(pathStr))
			return pathStr;
		else
			return null;
	}

	private static String validate_pickinfo(String inValue) {
		// valid values are "SKU, Description, Both"
		final String sku = "SKU";
		final String desc = "Description";
		final String both = "Both";

		if (sku.equalsIgnoreCase(inValue)) {
			return sku;
		} else if (desc.equalsIgnoreCase(inValue)) {
			return desc;
		} else if (both.equalsIgnoreCase(inValue)) {
			return both;
		}
		return null;
	}

	private static String validate_containertype(String inValue) {
		// valid values are "SKU, Description, Both"
		final String order = "Order";
		final String container = "Container";

		if (order.equalsIgnoreCase(inValue)) {
			return order;
		} else if (container.equalsIgnoreCase(inValue)) {
			return container;
		}
		return null;
	}

	private static String validate_rpeatpos(String inValue) {
		// valid values"
		final String noneStr = "None";
		final String cntrStr = "ContainerOnly";
		final String countStr = "ContainerAndCount";
		String returnStr = inValue;
		if (returnStr.equalsIgnoreCase(noneStr))
			return noneStr;
		else if (returnStr.equalsIgnoreCase(cntrStr))
			return cntrStr;
		else if (returnStr.equalsIgnoreCase(countStr))
			return countStr;
		else
			return null;
	}

	private static String validate_workseqr(String inValue) {
		// valid values
		// We had a "bay distance top tier last" for Accu, but then they decided not to use it. We thought it was a bad idea.
		// Still, there will certainly be other work sequences in the future.
		final String bayDistanceStr = "BayDistance";
		final String workSequencesStr = "WorkSequence";
		String returnStr = inValue;
		if (returnStr.equalsIgnoreCase(bayDistanceStr))
			return bayDistanceStr;
		else if (returnStr.equalsIgnoreCase(workSequencesStr))
			return workSequencesStr;
		else
			return null;
	}

	private static String validate_integer_in(String inValue, Integer minValue, Integer maxValue) {
		// return null if value is not a valid integer or is not in range.
		Integer theValue;
		try {
			theValue = Integer.valueOf(inValue);
		} catch (NumberFormatException e) {
			return null;
		}
		if (theValue >= minValue && theValue <= maxValue)
			return theValue.toString(); // usually same as inValue. But these routines are about converting to a canonical form.
		else
			return null;
	}

	private static String validate_color_not_black(String inValue) {
		String colorStr = inValue.toUpperCase(Locale.ENGLISH); // English as it will match the Enum as we have it in code.
		ColorEnum theColor = null;
		try {
			theColor = ColorEnum.valueOf(colorStr);
		} catch (IllegalArgumentException e) {
			return null;
		}
		switch (theColor) {
			case INVALID:
			case BLACK:
				return null;
			default: {
				return theColor.getName();
			}
		}
	}
	
	private static String validate_ordersub(String inValue) {
		String defaultORDERSUB = FacilityPropertyType.ORDERSUB.getDefaultValue();
		if (defaultORDERSUB.equalsIgnoreCase(inValue)) {
			return inValue;
		}
		String parts[] = inValue.split("-");
		if (parts.length != 2) {
			return null;
		}
		try {
			int start = Integer.parseInt(parts[0].trim());
			int end   = Integer.parseInt(parts[1].trim());
			if (start == 0 || end == 0){
				return null;
			}
			if (start <= end) {
				return start + "-" + end;
			}
		} catch (NumberFormatException e) {
		}
		return null;
	}
	
	private static String validate_protocol(String inValue) {
		if ("0".equals(inValue) || "1".equals(inValue)){
			return inValue;
		}
		return null;
	}
	
	private static String validate_timezone(String inValue) {
		String validIds[] = TimeZone.getAvailableIDs();
		for (String validId : validIds) {
			if (validId.equalsIgnoreCase(inValue)){
				return inValue;
			}
		}
		return null;
	}

	private static String validate_boolean(String inValue) {
		// Our liquidbase table/default definitions is using lower case
		final String trueStr = "true";
		final String falseStr = "false";
		String returnStr = inValue;
		if (returnStr.equalsIgnoreCase(trueStr))
			return trueStr;
		else if (returnStr.equalsIgnoreCase(falseStr))
			return falseStr;
		else
			return null;
	}

	/**
	 * Validation API. Simple validation types. Then specifics. Eventually there may be fairly elaborate application-level checks.
	 * Return null if no errors.
	 */
	private static String validateNewStringValue(FacilityPropertyType type, String inValue, String inCanonicalForm) {
		String returnValue = null;
		if (inValue == null)
			return ("null value");
		if (inValue.isEmpty())
			return ("Empty string");
		if (inCanonicalForm == null) { // no likely match found. For now the description lists the valid values.
			return ("Valid values:  " + type.getValidValues());
		}

		return returnValue;
	}
}
