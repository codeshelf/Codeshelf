package com.gadgetworks.codeshelf.model.domain;

import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

import org.hibernate.annotations.Type;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.gadgetworks.codeshelf.model.dao.ITypedDao;
import com.gadgetworks.codeshelf.model.dao.PropertyDao;
import com.gadgetworks.flyweight.command.ColorEnum;
import com.google.inject.Inject;

@Entity
@Table(name = "property")
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE)
public class DomainObjectProperty extends DomainObjectABC implements IDomainObject {

	private static final Logger						LOGGER		= LoggerFactory.getLogger(DomainObjectProperty.class);

	@Inject
	public static ITypedDao<DomainObjectProperty>	DAO			= PropertyDao.getInstance();

	@Getter
	@NonNull
	@Column(name = "objectid", nullable = false)
	@Type(type = "com.gadgetworks.codeshelf.platform.persistence.DialectUUIDType")
	private UUID									objectId	= null;

	@Getter
	@Column(length = 120, nullable = false)
	String											value;

	@Getter
	@Setter
	@ManyToOne(fetch = FetchType.EAGER, optional = false)
	@JoinColumn(name = "property_default_persistentid")
	DomainObjectPropertyDefault						propertyDefault;

	// These match contents in liquidbase change .xml files
	public final static String						BAYCHANG	= "BAYCHANG";
	public final static String						RPEATPOS	= "RPEATPOS";
	public final static String						WORKSEQR	= "WORKSEQR";
	public final static String						LIGHTSEC	= "LIGHTSEC";
	public final static String						LIGHTCLR	= "LIGHTCLR";
	public final static String						CROSSBCH	= "CROSSBCH";
	public final static String						AUTOSHRT	= "AUTOSHRT";

	public DomainObjectProperty() {
	}

	public DomainObjectProperty(IDomainObject object, DomainObjectPropertyDefault propertyDefault) {
		this.setDomainId(propertyDefault.getName());
		this.propertyDefault = propertyDefault;
		this.objectId = object.getPersistentId();
	}

	public DomainObjectProperty(IDomainObject object, DomainObjectPropertyDefault type, String value) {
		this(object, type);
		this.value = value;
	}

	public DomainObjectProperty setValue(String stringValue) {
		this.value = stringValue;
		return this;
	}

	public DomainObjectProperty setValue(int intValue) {
		this.value = Integer.toString(intValue);
		return this;
	}

	public DomainObjectProperty setValue(double doubleValue) {
		this.value = Double.toString(doubleValue);
		return this;
	}

	public DomainObjectProperty setValue(boolean boolValue) {
		this.value = Boolean.toString(boolValue);
		return this;
	}

	public int getIntValue() {
		if (this.value == null) {
			return Integer.parseInt(getDefaultValue());
		}
		return Integer.parseInt(this.value);
	}

	public double getDoubleValue() {
		if (this.value == null) {
			return Double.parseDouble(getDefaultValue());
		}
		return Double.parseDouble(this.value);
	}

	public boolean getBooleanValue() {
		if (this.value == null) {
			return Boolean.parseBoolean(getDefaultValue());
		}
		return Boolean.parseBoolean(this.value);
	}
	
	public ColorEnum getColorValue() {
		if (this.value == null) {
			return ColorEnum.valueOf(getDefaultValue());

		}
		return ColorEnum.valueOf(this.value);
	}

	// convenience function to get the property name via default/type object
	public String getName() {
		if (propertyDefault == null) {
			return null;
		}
		return propertyDefault.getName();
	}

	// convenience function
	public String getDescription() {
		if (propertyDefault == null) {
			return null;
		}
		return propertyDefault.getDescription();
	}

	// convenience function
	public String getObjectType() {
		if (propertyDefault == null) {
			return null;
		}
		return propertyDefault.getObjectType();
	}

	// convenience function to get the default value via default/type object
	public String getDefaultValue() {
		if (propertyDefault == null) {
			return null;
		}
		return propertyDefault.getDefaultValue();
	}

	@Override
	public String getDefaultDomainIdPrefix() {
		return "PROP";
	}

	@Override
	public Facility getFacility() {
		throw new RuntimeException("Not Supported");
	}

	@SuppressWarnings("unchecked")
	@Override
	public ITypedDao<DomainObjectProperty> getDao() {
		return DomainObjectProperty.DAO;
	}

	/**
	 * TODO: should be moved to meta data/default table instead of being hard coded here
	 * Converts things like "baychange" to "BayChange".
	 * Return null if there is no likely match.
	 */
	public String validInputValues() {
		// Find out which one we are
		String myName = this.getName();
		if (myName.equals(BAYCHANG))
			return "None, BayChange, BayChangeExceptAcrossAisle, PathSegmentChange";
		else if (myName.equals(RPEATPOS))
			return "None, ContainerOnly, ContainerAndCount";
		else if (myName.equals(WORKSEQR))
			return "BayDistance";
		else if (myName.equals(LIGHTSEC))
			return "number between 2 and 30";
		else if (myName.equals(LIGHTCLR))
			return "Red, Green, Blue, Cyan, Orange, Magenta, White";
		else if (myName.equals(CROSSBCH))
			return "true, false";
		else if (myName.equals(AUTOSHRT))
			return "true, false";
		else {
			LOGGER.error("new DomainObjectProperty: " + myName + " has no validInputValues implementation");
		}
		return null;
	}

	/**
	 * Converts things like "baychange" to "BayChange".
	 * Return null if there is no likely match.
	 */
	public String toCanonicalForm(String inValue) {
		if (inValue == null)
			return null;
		String trimmedValue = inValue.trim();
		if (trimmedValue.isEmpty())
			return null;
		// Find out which one we are
		String myName = this.getName();
		if (myName.equals(BAYCHANG))
			return validate_baychang(trimmedValue);
		else if (myName.equals(RPEATPOS))
			return validate_rpeatpos(trimmedValue);
		else if (myName.equals(WORKSEQR))
			return validate_workseqr(trimmedValue);
		else if (myName.equals(LIGHTSEC))
			return validate_integer_in(trimmedValue, 2, 30); // mininum 2, max 30
		else if (myName.equals(LIGHTCLR))
			return validate_color_not_black(trimmedValue);
		else if (myName.equals(CROSSBCH))
			return validate_boolean(trimmedValue);
		else if (myName.equals(AUTOSHRT))
			return validate_boolean(trimmedValue);
		else {
			LOGGER.error("new DomainObjectProperty: " + myName + " has no toCanonicalForm implementation");
		}

		return null;
	}

	private String validate_baychang(String inValue) {
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

	private String validate_rpeatpos(String inValue) {
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

	private String validate_workseqr(String inValue) {
		// valid values
		// We had a "bay distance top tier last" for Accu, but then they decided not to use it. We thought it was a bad idea.
		// Still, there will certainly be other work sequences in the future.
		final String bayDistanceStr = "BayDistance";
		String returnStr = inValue;
		if (returnStr.equalsIgnoreCase(bayDistanceStr))
			return bayDistanceStr;
		else
			return null;
	}

	private String validate_integer_in(String inValue, Integer minValue, Integer maxValue) {
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

	private String validate_color_not_black(String inValue) {
		ColorEnum theColor = ColorEnum.valueOf(inValue);
		// Let the color Enum do its validation
		switch (theColor) {
			case INVALID:
			case BLACK:
				return null;
			default: {
				return theColor.getName();
			}
		}
	}

	private String validate_boolean(String inValue) {
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
	public String validateNewStringValue(String inValue, String inCanonicalForm) {
		String returnValue = null;
		if (inValue == null)
			return ("null value");
		if (inValue.isEmpty())
			return ("Empty string");
		if (inCanonicalForm == null) { // no likely match found. For now the description lists the valid values.
			return ("Valid values:  " + this.validInputValues());
		}

		return returnValue;
	}

}
