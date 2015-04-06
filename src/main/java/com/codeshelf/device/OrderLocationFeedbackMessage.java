package com.codeshelf.device;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import com.codeshelf.model.OrderStatusEnum;
import com.codeshelf.model.domain.LedController;
import com.codeshelf.model.domain.Location;
import com.codeshelf.model.domain.OrderHeader;
import com.codeshelf.model.domain.OrderLocation;
import com.codeshelf.ws.protocol.message.MessageABC;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class OrderLocationFeedbackMessage extends MessageABC {

	private static final Logger	LOGGER	= LoggerFactory.getLogger(OrderLocationFeedbackMessage.class);

	@Accessors(prefix = "m")
	@Getter
	@Setter
	@Expose
	@SerializedName(value = "controllerId")
	private String				mControllerId;

	@Accessors(prefix = "m")
	@Getter
	@Setter
	@Expose
	@SerializedName(value = "position")
	private Byte				mPosition;
	// as a Byte, similar to PosControllerInstr. Someday might change to an Integer

	@Accessors(prefix = "m")
	@Getter
	@Setter
	@Expose
	@SerializedName(value = "locationName")
	private String				mLocationName;

	@Accessors(prefix = "m")
	@Getter
	@Setter
	@Expose
	@SerializedName(value = "orderId")
	private String				mOrderId;																// not initially used. Just seems like site controller could use it.

	@Accessors(prefix = "m")
	@Getter
	@Setter
	@Expose
	@SerializedName(value = "hasAnyOrderAtAl")
	private Boolean				mHasAnyOrderAtAll;

	@Accessors(prefix = "m")
	@Getter
	@Setter
	@Expose
	@SerializedName(value = "orderFullyComplete")
	private Boolean				mOrderFullyComplete;

	@Accessors(prefix = "m")
	@Getter
	@Setter
	@Expose
	@SerializedName(value = "orderCompleteThisArea")
	private Boolean				mOrderCompleteThisArea;													// not initially used. 

	public OrderLocationFeedbackMessage() {}

	public OrderLocationFeedbackMessage(OrderLocation ol) {
		Location loc = ol.getLocation();
		setLocationDependentFields(loc);
		setHasAnyOrderAtAll(true);
		OrderHeader oh = ol.getParent();
		setOrderId(oh.getDomainId());
		setOrderFullyComplete(OrderStatusEnum.COMPLETE.equals(oh.getStatus()));
		setOrderCompleteThisArea(getOrderFullyComplete()); // fix later
	}

	public OrderLocationFeedbackMessage(Location loc) {
		// Use this constructor to intentionally tell site controller there is nothing for this slot.
		setLocationDependentFields(loc);
		setHasAnyOrderAtAll(false);
		setOrderId("");
		setOrderFullyComplete(false);
		setOrderCompleteThisArea(false);
	}

	private void setLocationDependentFields(Location loc) {
		// Device GUID and position
		LedController controller = loc.getEffectiveLedController();
		if (controller == null)
			LOGGER.error("OrderLocationFeedbackMessage constructor called for location without controller");
		else
			setControllerId(controller.getDeviceGuidStr());
		Integer index = loc.getPosconIndex();
		setPosition(index.byteValue());

		// Location name in a uniform way, so the map in PosManagerDeviceLogic can set and clear.
		String locName = loc.getPrimaryAliasId();
		if (locName.isEmpty())
			locName = loc.getNominalLocationIdExcludeBracket();
		setLocationName(locName);
	}

}
