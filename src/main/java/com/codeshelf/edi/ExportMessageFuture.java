package com.codeshelf.edi;

import groovy.transform.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

import org.joda.time.DateTime;

import com.codeshelf.model.domain.ExportReceipt;
import com.google.common.util.concurrent.AbstractFuture;
import com.google.common.util.concurrent.ListenableFuture;

@EqualsAndHashCode(excludes={"contents"})
public class ExportMessageFuture extends AbstractFuture<ExportReceipt> implements ListenableFuture<ExportReceipt> {

	public static class OrderOnCartFinishedExportMessage extends ExportMessageFuture {

		public OrderOnCartFinishedExportMessage(String inOrderId, String inCheGuid, String exportStr) {
			super(inOrderId, inCheGuid, exportStr);
		}

	}

	public static class OrderOnCartAddedExportMessage extends ExportMessageFuture {

		public OrderOnCartAddedExportMessage(String inOrderId, String inCheGuid, String exportStr) {
			super(inOrderId, inCheGuid, exportStr);
		}
		
	}

	
	@Getter
	private String	orderId;
	
	@Getter
	private String	cheGuid;
	
	@Getter
	private String	contents;
	
	@Getter
	private DateTime	dateTime;
	
	@Getter @Setter
	private UUID		persistentId;
	
	
	public ExportMessageFuture(String orderId, String cheGuid, String contents) {
		super();
		this.dateTime = new DateTime();
		this.orderId = orderId;
		this.cheGuid = cheGuid;
		this.contents = contents;
	}

	public void setReceipt(ExportReceipt receipt) {
		set(receipt);
	}
	
	@Override
	public String toString() {
		return String.format("%s: orderId: %s, che: %s, time: %s", this.getClass().getSimpleName(), orderId, cheGuid, dateTime.toString());
	}
}
