package com.codeshelf.edi;

import groovy.transform.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.UUID;

import org.joda.time.DateTime;

import com.codeshelf.model.domain.Che;
import com.codeshelf.model.domain.ExportReceipt;
import com.codeshelf.model.domain.OrderHeader;
import com.google.common.util.concurrent.AbstractFuture;
import com.google.common.util.concurrent.ListenableFuture;

@EqualsAndHashCode(excludes={"contents"})
@ToString(exclude={"contents"})
public class ExportMessage extends AbstractFuture<ExportReceipt> implements ListenableFuture<ExportReceipt> {

	public static class OrderOnCartFinishedExportMessage extends ExportMessage {

		public OrderOnCartFinishedExportMessage(OrderHeader inOrder, Che inChe, String exportStr) {
			super(inOrder, inChe, exportStr);
		}

	}

	public static class OrderOnCartAddedExportMessage extends ExportMessage {

		public OrderOnCartAddedExportMessage(OrderHeader inOrder, Che inChe, String exportStr) {
			super(inOrder, inChe, exportStr);
		}
		
	}

	
	@Getter
	private OrderHeader	order;
	
	@Getter
	private Che	che;
	
	@Getter
	private String	contents;
	
	@Getter
	private DateTime	dateTime;
	
	@Getter @Setter
	private UUID		persistentId;
	
	
	public ExportMessage(OrderHeader order, Che che, String contents) {
		super();
		this.dateTime = new DateTime();
		this.order = order;
		this.che = che;
		this.contents = contents;
	}

	public void setReceipt(ExportReceipt receipt) {
		set(receipt);
	}
}
