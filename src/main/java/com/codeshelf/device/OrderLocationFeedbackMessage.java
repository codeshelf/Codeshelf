package com.codeshelf.device;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
	@SerializedName(value = "hasAnyOrderAtAll")
	private Boolean				mHasAnyOrderAtAll;

	@Accessors(prefix = "m")
	@Getter
	@Setter
	@Expose
	@SerializedName(value = "jobCountRemaining")
	private int				mJobCountRemaining;

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

	@Accessors(prefix = "m")
	@Getter
	@Setter
	@Expose
	@SerializedName(value = "lastMsgOfGroup")
	private Boolean				mLastMsgOfGroup;

	public OrderLocationFeedbackMessage() {}

	public OrderLocationFeedbackMessage(OrderLocation ol, boolean lastMsgOfGroup) {
		// Use this constructor for one poscon per slot, one orderLocation per slot.
		Location loc = ol.getLocation();
		setLocationDependentFields(loc);
		setHasAnyOrderAtAll(true);
		OrderHeader oh = ol.getParent();
		setOrderFullyComplete(OrderStatusEnum.COMPLETE.equals(oh.getStatus()));
		setOrderCompleteThisArea(getOrderFullyComplete()); // fix later
		setLastMsgOfGroup(lastMsgOfGroup);
	}


	public OrderLocationFeedbackMessage(Location loc, boolean lastMsgOfGroup) {
		// Use this constructor to intentionally tell site controller there is nothing for this slot or other location.
		setLocationDependentFields(loc);
		setHasAnyOrderAtAll(false);
		setOrderFullyComplete(false);
		setOrderCompleteThisArea(false);
		setLastMsgOfGroup(lastMsgOfGroup);
	}

	public OrderLocationFeedbackMessage(Location loc, int remainingCount, boolean lastMsgOfGroup) {
		// Use this constructor to intentionally tell site controller there is nothing for this slot.
		setLocationDependentFields(loc);
		setHasAnyOrderAtAll(true);
		setOrderFullyComplete(false);
		setOrderCompleteThisArea(false);
		setJobCountRemaining(remainingCount);
		setLastMsgOfGroup(lastMsgOfGroup);
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
		String locName = loc.getLocationNameForMap();
		setLocationName(locName);
	}

	@Override
	public String getDeviceIdentifier() {
		return getControllerId();
	}

}
