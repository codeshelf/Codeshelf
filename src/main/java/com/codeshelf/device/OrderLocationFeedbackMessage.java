package com.codeshelf.device;

import java.util.List;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import com.codeshelf.model.OrderStatusEnum;
import com.codeshelf.model.domain.OrderHeader;
import com.codeshelf.model.domain.OrderLocation;
import com.codeshelf.ws.protocol.message.MessageABC;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class OrderLocationFeedbackMessage extends MessageABC{
	@Accessors(prefix = "m")
	@Getter @Setter @Expose
	private List<PosControllerInstr>				mInstructions;

	@Accessors(prefix = "m")
	@Getter @Setter @Expose
	private String				mLocationName;

	@Accessors(prefix = "m")
	@Getter @Setter @Expose
	private String				mOrderId; // not initially used. Just seems like site controller could use it.
	
	@Accessors(prefix = "m")
	@Getter @Setter @Expose
	private Boolean				mOrderFullyComplete;
	
	@Accessors(prefix = "m")
	@Getter @Setter @Expose
	private Boolean				mOrderCompleteThisArea; // not initially used. 

	
	public OrderLocationFeedbackMessage(OrderLocation ol) {
		OrderHeader oh = ol.getParent();
		setOrderId(oh.getDomainId());
		setLocationName(ol.getDomainId());
		setOrderFullyComplete(OrderStatusEnum.COMPLETE.equals(oh.getStatus()));
		setOrderCompleteThisArea(getOrderFullyComplete()); // fix later
	}
}
