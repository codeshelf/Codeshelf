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
 * This is a structure and calculator for order counts of its details. This refers to the single passed in order
 * WorkService needs this for order feedback for putwalls
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
		clearStatusSummary();
	}
	
	public void clearStatusSummary() {
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

	class SingleOrderStatusSummary {

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
