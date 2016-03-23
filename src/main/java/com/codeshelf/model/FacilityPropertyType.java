package com.codeshelf.model;

import lombok.Getter;

public enum FacilityPropertyType {
	AUTOSHRT(
			"true",
			"On CHE setup, if there is no product known by the system, system will immediately short and not present those work instructions. Some sites may like this off, making the worker locate and update the item location immediately.",
			"true, false."),
	BADGEAUTH(
			"false",
			"If true, only registered Workers will be able to log into CHEs. If false, creates default accounts for new badges.",
			"true, false."),
	BAYCHANG(
			"BayChange",
			"Determine under what conditions the CHE should get a bay change housekeeping work instruction.",
			"None, Baychange, BaychangeExceptAcrossAisle, PathSegmentChange."),
	RPEATPOS(
			"ContainerOnly",
			"Determine under what conditions the CHE should get a repeat position housekeeping work instruction.",
			"None, ContainerOnly, ContainerAndCount."),
	CNTRTYPE(
			"Order",
			"The content of the CHE containers. This affects the message displayed during CHE setup.",
			"Order, Container."),
	CROSSBCH("false", "Are cross-batch orders expected in the system? If off, hides some unneeded UI elements.", "true, false."),
	EACHMULT(
			"false",
			"Shall multiple inventory locations for EACH SKUs be allowed in the facility. (Will decrement or change eventually in favor of work area controls.)",
			"true, false."),
	INVTMENU("false", "If true the UI will have a menu for viewing and managing inventory.", "true, false."),
	LIGHTCLR(
			"RED",
			"When a user lights a location or item, what color shall it light",
			"Red, Green, Blue, Cyan, Orange, Magenta, White."),
	LIGHTSEC("7", "When a user lights a location or item, how many seconds shall it remain lit.", "number between 2 and 30."),
	LOCAPICK("false", "Shall inventory and pick locations come from preferredLocation field on order detail.", "true, false."),
	ORDERSUB(
			"Disabled",
			"Specification of the sub-string to capture from scanned order/container. A value of 11-19 means to take characters 11 through 19 as the sub-string. This parameter takes effect after the site controller restarts.",
			"Disabled, <start>-<end>."),
	PICKINFO("SKU", "The secondary information shown on the CHE during a pick, below the location.", "SKU, Description, Both."),
	PICKMULT(
			"false",
			"If true, get simultaneous pick instructions to several position controllers for a single SKU/location.",
			"true, false."),
	PRODUCTION(
			"false",
			"If true, Codeshelf monitoring aggressively alerts on failures. If false, monitoring does not alert.",
			"true, false."),
	PROTOCOL("0", "Site Controller radio protocol version.", "0, 1"),
	SCANPICK(
			"Disabled",
			"Enabling will require the picker to scan the picked item before placing in a container. Specify if the picker should scan the item SKU or UPC.",
			"Disabled, SKU, UPC."),
	TIMEZONE(
			"US/Pacific",
			"Facility local timezone. Must be a valid Java TimeZone ID. Some US timezones for quick refference: US/Eastern, US/Central, US/Mountain, US/Pacific.",
			" valid timezone name."),
	WORKSEQR(
			"BayDistance",
			"When a CHE is set up and work instructions created, what sort algorithm should be used.",
			"BayDistance, WorkSequence."),
	INDICATOR("Disabled", "Handling of bay or aisle indicator lights if modeled", "Disabled, All_jobs.");

	@Getter
	private String	key;

	@Getter
	private String	defaultValue;

	private String	description;

	@Getter
	private String	validValues;

	private FacilityPropertyType(String defaultValue, String description, String validValues) {
		this.defaultValue = defaultValue;
		this.description = description;
		this.validValues = validValues;
	}

	public String getDescription() {
		return description + " Valid values: " + validValues;
	}
}