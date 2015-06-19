/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2014, Codeshelf, Inc., All rights reserved
 *  author jon ranstrom
 *******************************************************************************/
package com.codeshelf.model;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import lombok.Getter;

import com.codeshelf.model.domain.OrderDetail;
import com.codeshelf.model.domain.OrderHeader;

/**
 * This is a structure and calculator summarize detail statuses for an arbitrary collection of orders.
 * An inner class does the counting for each order
 * WorkService needs this for order feedback for put walls
  */

public class OrderStatusSummary {
	private static final Logger	LOGGER	= LoggerFactory.getLogger(OrderStatusSummary.class);

	@Getter
	private int					completeCount;
	@Getter
	private int					remainingCount;
	@Getter
	public int					shortCount;

	public OrderStatusSummary() {
		completeCount = 0;
		remainingCount = 0;
		shortCount = 0;
	}
	
	public void addOrderToSummary(OrderHeader inOrder) {
		if (inOrder == null) {
			LOGGER.info("null order in OrderStatusSummary addOrderToSummary");
			return;
		}

		SingleOrderStatusSummary singleStatus = new SingleOrderStatusSummary(inOrder);
		incrementCounts(singleStatus);
	}

	private void incrementCounts(SingleOrderStatusSummary singleStatus) {

		completeCount += singleStatus.singleOrderCompleteCount;
		remainingCount += singleStatus.singleOrderRemainingCount;
		shortCount += singleStatus.singleOrderShortCount;
	}

	private class SingleOrderStatusSummary {

		private OrderHeader	order;
		private int			singleOrderCompleteCount;
		private int			singleOrderRemainingCount;
		public int			singleOrderShortCount;

		public SingleOrderStatusSummary(OrderHeader inOrder) {
			order = inOrder;
			computeSingleOrderCounts();
		}

		private void computeSingleOrderCounts() {
			if (order == null) {
				LOGGER.info("null order in OrderStatusSummary computeCounts");
				return;
			}
			List<OrderDetail> details = order.getOrderDetails();
			for (OrderDetail detail : details) {
				OrderStatusEnum theStatus = detail.getStatus();
				switch (theStatus) {
					case SHORT:
						shortCount++;
						break;
					case COMPLETE:
						completeCount++;
						break;
					default:
						remainingCount++;
				}
			}
		}
	}

}
