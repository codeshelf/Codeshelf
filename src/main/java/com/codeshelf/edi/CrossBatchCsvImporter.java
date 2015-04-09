/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: CsvImporter.java,v 1.30 2013/07/22 04:30:36 jeffw Exp $
 *******************************************************************************/
package com.codeshelf.edi;

import java.io.Reader;
import java.sql.Timestamp;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.persistence.PersistenceException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.event.EventProducer;
import com.codeshelf.event.EventSeverity;
import com.codeshelf.event.EventTag;
import com.codeshelf.model.OrderStatusEnum;
import com.codeshelf.model.OrderTypeEnum;
import com.codeshelf.model.dao.DaoException;
import com.codeshelf.model.domain.Container;
import com.codeshelf.model.domain.ContainerKind;
import com.codeshelf.model.domain.ContainerUse;
import com.codeshelf.model.domain.Facility;
import com.codeshelf.model.domain.ItemMaster;
import com.codeshelf.model.domain.OrderDetail;
import com.codeshelf.model.domain.OrderGroup;
import com.codeshelf.model.domain.OrderHeader;
import com.codeshelf.model.domain.UomMaster;
import com.codeshelf.service.WorkService;
import com.codeshelf.service.WorkService.Work;
import com.codeshelf.validation.BatchResult;
import com.codeshelf.validation.DefaultErrors;
import com.codeshelf.validation.ErrorCode;
import com.codeshelf.validation.InputValidationException;
import com.google.inject.Inject;
import com.google.inject.Singleton;

/**
 * @author jeffw
 *
 */
@Singleton
public class CrossBatchCsvImporter extends CsvImporter<CrossBatchCsvBean> implements ICsvCrossBatchImporter {

	private static final Logger		LOGGER	= LoggerFactory.getLogger(CrossBatchCsvImporter.class);

	private WorkService	mWorkService;

	@Inject
	public CrossBatchCsvImporter(final EventProducer inProducer,
		final WorkService inWorkService) {

		super(inProducer);
		mWorkService = inWorkService;
	}

	// --------------------------------------------------------------------------
	/* (non-Javadoc)
	 * @see com.codeshelf.edi.ICsvImporter#importInventoryFromCsvStream(java.io.InputStreamReader, com.codeshelf.model.domain.Facility)
	 */
	public final int importCrossBatchesFromCsvStream(Reader inCsvStreamReader, Facility inFacility, Timestamp inProcessTime) {

		LOGGER.debug("Begin cross batch import.");
		List<CrossBatchCsvBean> crossBatchBeanList = toCsvBean(inCsvStreamReader, CrossBatchCsvBean.class);

		int importedRecords = 0;
		Set<String> importedContainerIds = new HashSet<String>();
		// Iterate over the put batch import beans.
		for (CrossBatchCsvBean crossBatchBean : crossBatchBeanList) {
			try {
				Container container = crossBatchCsvBeanImport(crossBatchBean, inFacility, inProcessTime);
				importedContainerIds.add(container.getContainerId());
				BatchResult<Work> workResults = mWorkService.determineWorkForContainer(inFacility, container);
				//				produceRecordSuccessEvent(crossBatchBean);
				importedContainerIds.add(container.getContainerId());
				importedRecords++;
				if (!workResults.isSuccessful()) {
					produceRecordViolationEvent(EventSeverity.WARN, workResults.getViolations(), crossBatchBean);
				}
			} catch (InputValidationException e) {
				produceRecordViolationEvent(EventSeverity.WARN, e, crossBatchBean);
				LOGGER.warn("Unable to process record: " + crossBatchBean, e);
			} catch (Exception e) {
				produceRecordViolationEvent(EventSeverity.ERROR, e, crossBatchBean);
				LOGGER.error("Unable to process record: " + crossBatchBean, e);
			}
		}

		archiveCheckCrossBatches(inFacility, inProcessTime, importedContainerIds);

		LOGGER.debug("End cross batch import.");
		return importedRecords;
	}

	// --------------------------------------------------------------------------
	/**
	 * @param inFacility
	 * @param inProcessTime
	 */
	private void archiveCheckCrossBatches(final Facility inFacility,
		final Timestamp inProcessTime,
		final Set<String> inImportedContainerIds) {
		LOGGER.debug("Archive unreferenced put batch data");

		// Inactivate the WONDERWALL order detail that don't match the import timestamp.
		// FIXME: this method should not loop through all orders. replace with database query.
		List<OrderHeader> orders = OrderHeader.staticGetDao().findByParent(inFacility);
		//List<OrderHeader> orders = inFacility.getOrderHeaders();
		for (OrderHeader order : orders) {
			if (order.getOrderType().equals(OrderTypeEnum.CROSS)) {
				Boolean shouldArchiveOrder = true;
				// Make this more robust if the beans are not quite consistent. We do not want to totally fail out of EDI just because we cannot fully investigate
				// (I only got this by deleting containerUses table, so order.getContainerId() threw EntityNotFound)
				// By catching here and below, we accomplish two things. 1) Orders that can be archived are. 2) The batch file is processed and is not left as .FAILED
				try {
					if (!inImportedContainerIds.contains(order.getContainerId())) {
						shouldArchiveOrder = false;
					} else {
						for (OrderDetail orderDetail : order.getOrderDetails()) {
							if (orderDetail.getUpdated().equals(inProcessTime)) {
								shouldArchiveOrder = false;
							} else {
								LOGGER.debug("Archive old wonderwall order detail: " + orderDetail.getDomainId());
								orderDetail.setActive(false);
								OrderDetail.staticGetDao().store(orderDetail);
							}
						}
					}
				} catch (PersistenceException e) {
					LOGGER.error("Caught exception investigating an order in archiveCheckCrossBatches", e);
				}

				if (shouldArchiveOrder) {
					try {

						order.setActive(false);
						OrderHeader.staticGetDao().store(order);

						ContainerUse containerUse = order.getContainerUse();
						if (containerUse != null) {
							containerUse.setActive(false);
							ContainerUse.staticGetDao().store(containerUse);
						}
					} catch (PersistenceException e) {
						LOGGER.error("Caught exception archiving order or containerUse in archiveCheckCrossBatches", e);
					}

				}
			}
		}

	}

	// --------------------------------------------------------------------------
	/**
	 * @param inCsvBean
	 * @param inFacility
	 * @param inEdiProcessTime
	 */
	private Container crossBatchCsvBeanImport(final CrossBatchCsvBean inCsvBean,
		final Facility inFacility,
		final Timestamp inEdiProcessTime) throws InputValidationException {

		DefaultErrors errors = new DefaultErrors(inCsvBean.getClass());
		try {
			String errorMsg = inCsvBean.validateBean();
			if (errorMsg != null) {
				throw new InputValidationException(inCsvBean, errorMsg);
			}

			LOGGER.info(inCsvBean.toString());
			try {
				int quantity = Integer.valueOf(inCsvBean.getQuantity());
				// Only create the CROSS detail if the quantity is > 0.
				if (quantity <= 0) {
					errors.minViolation("quantity", quantity, 0);
				}
			} catch (NumberFormatException e) {
				errors.bindViolation("quantity", inCsvBean.getQuantity(), Integer.class);
			}

			ItemMaster itemMaster = inFacility.getItemMaster(inCsvBean.getItemId());
			if (itemMaster == null) {
				errors.rejectValue("itemId", inCsvBean.getItemId(), ErrorCode.FIELD_REFERENCE_NOT_FOUND);
			}

			if (!errors.hasErrors()) {
				OrderGroup group = updateOptionalOrderGroup(inCsvBean, inFacility, inEdiProcessTime);
				OrderHeader order = updateOrderHeader(inCsvBean, inFacility, inEdiProcessTime, group);
				UomMaster uomMaster = updateUomMaster(inCsvBean, inFacility);
				@SuppressWarnings("unused")
				OrderDetail detail = updateOrderDetail(inCsvBean, inFacility, inEdiProcessTime, order, itemMaster, uomMaster);
				Container container = updateContainer(inCsvBean, inFacility, inEdiProcessTime, order);
				//OrderHeader.staticGetDao().commitTransaction();
				return container;
			}
		} catch (Exception e) {
			errors.reject(ErrorCode.GENERAL, e.toString());
		} finally {
			//OrderHeader.staticGetDao().endTransaction();
		}
		throw new InputValidationException(errors);
	}

	// --------------------------------------------------------------------------
	/**
	 * @param inCsvBean
	 * @param inFacility
	 * @param inEdiProcessTime
	 * @return
	 */
	private OrderGroup updateOptionalOrderGroup(final CrossBatchCsvBean inCsvBean,
		final Facility inFacility,
		final Timestamp inEdiProcessTime) {
		OrderGroup result = null;

		result = inFacility.getOrderGroup(inCsvBean.getOrderGroupId());
		if ((result == null) && (inCsvBean.getOrderGroupId() != null) && (inCsvBean.getOrderGroupId().length() > 0)) {
			result = new OrderGroup();
			result.setOrderGroupId(inCsvBean.getOrderGroupId());
			result.setDescription(OrderGroup.DEFAULT_ORDER_GROUP_DESC_PREFIX + inCsvBean.getOrderGroupId());
			inFacility.addOrderGroup(result);
		}

		if (result != null) {
			result.setStatus(OrderStatusEnum.RELEASED);
			try {
				result.setActive(true);
				result.setUpdated(inEdiProcessTime);
				OrderGroup.staticGetDao().store(result);
			} catch (DaoException e) {
				LOGGER.error("", e);
			}
		}

		return result;
	}

	// --------------------------------------------------------------------------
	/**
	 * @param inCsvBean
	 * @param inFacility
	 * @param inOrderGroup
	 * @return
	 */
	private OrderHeader updateOrderHeader(final CrossBatchCsvBean inCsvBean,
		final Facility inFacility,
		final Timestamp inEdiProcessTime,
		final OrderGroup inOrderGroup) {
		OrderHeader result = null;

		String domainId = OrderHeader.computeCrossOrderId(inCsvBean.getContainerId(), inEdiProcessTime);
		result = OrderHeader.staticGetDao().findByDomainId(inFacility, domainId);
		// result = inFacility.getOrderHeader(OrderHeader.computeCrossOrderId(inCsvBean.getContainerId(), inEdiProcessTime));

		if (result == null) {
			result = new OrderHeader();
			result.setDomainId(OrderHeader.computeCrossOrderId(inCsvBean.getContainerId(), inEdiProcessTime));
			result.setStatus(OrderStatusEnum.RELEASED);
			result.setParent(inFacility);
		}

		try {
			result.setOrderType(OrderTypeEnum.CROSS);

			if (inOrderGroup != null) {
				inOrderGroup.addOrderHeader(result);
				result.setOrderGroup(inOrderGroup);
			}

			// It's not clear to me that this is correct.
			// Do we want the client to be able to setup put batches for a particular date?
			// Right now, OrderGroupDetail is the thing that groups put batch orders.
			result.setOrderDate(inEdiProcessTime);
			result.setDueDate(inEdiProcessTime);

			result.setActive(true);
			result.setUpdated(inEdiProcessTime);
			OrderHeader.staticGetDao().store(result);
		} catch (DaoException e) {
			LOGGER.error("", e);
		}

		return result;
	}

	// --------------------------------------------------------------------------
	/**
	 * @param inCsvBean
	 * @param inFacility
	 * @param inEdiProcessTime
	 * @param inOrder
	 * @param inUomMaster
	 * @param inItemMaster
	 * @return
	 */
	private OrderDetail updateOrderDetail(final CrossBatchCsvBean inCsvBean,
		final Facility inFacility,
		final Timestamp inEdiProcessTime,
		final OrderHeader inOrder,
		final ItemMaster inItemMaster,
		final UomMaster inUomMaster) {
		OrderDetail result = null;

		result = inOrder.getOrderDetail(inCsvBean.getItemId());
		if (result == null) {
			result = new OrderDetail();
			result.setDomainId(inCsvBean.getItemId());
			result.setStatus(OrderStatusEnum.RELEASED);

			inOrder.addOrderDetail(result);
		}

		result.setItemMaster(inItemMaster);
		result.setDescription(inItemMaster.getDescription());
		// In CrossBatch orders the nominal, min and max quantities are always equal on the cross order side.
		result.setQuantities(Integer.valueOf(inCsvBean.getQuantity()));

		result.setUomMaster(inUomMaster);
		result.setUpdated(inEdiProcessTime);
		if (result.getQuantity() == 0) {
			result.setActive(false);
		} else {
			result.setActive(true);
		}

		try {
			OrderDetail.staticGetDao().store(result);
		} catch (DaoException e) {
			LOGGER.error("", e);
		}
		return result;
	}

	// --------------------------------------------------------------------------
	/**
	 * @param inCsvBean
	 * @param inFacility
	 * @param inEdiProcessTime
	 * @param inOrder
	 * @return
	 */
	private Container updateContainer(final CrossBatchCsvBean inCsvBean,
		final Facility inFacility,
		final Timestamp inEdiProcessTime,
		final OrderHeader inOrder) {
		Container result = null;

		if ((inCsvBean.getContainerId() != null) && (inCsvBean.getContainerId().length() > 0)) {
			result = Container.staticGetDao().findByDomainId(inFacility, inCsvBean.getContainerId()); 
			if (result == null) {
				result = new Container();
				result.setContainerId(inCsvBean.getContainerId());
				result.setParent(inFacility);
				result.setKind(inFacility.getContainerKind(ContainerKind.DEFAULT_CONTAINER_KIND));
			}

			result.setUpdated(inEdiProcessTime);
			result.setActive(true);
			try {
				Container.staticGetDao().store(result);
			} catch (DaoException e) {
				LOGGER.error("", e);
			}

			// Now create the container use for this. ContainerUse has Container, OrderHead as potential parent.  (Che also, but not set here.)
			ContainerUse use = result.getContainerUse(inOrder);
			if (use == null) {
				use = new ContainerUse();
				use.setDomainId(inOrder.getOrderId());
				inOrder.addHeadersContainerUse(use);
				result.addContainerUse(use);
			} else {
				OrderHeader prevOrder = use.getOrderHeader();
				// No worries for container, as no way containerUse can change to different container owner.
				if (prevOrder == null)
					inOrder.addHeadersContainerUse(use);
				else if (!prevOrder.equals(inOrder)) {
					prevOrder.removeHeadersContainerUse(use);
					inOrder.addHeadersContainerUse(use);
				}
			}

			Timestamp timestamp = new Timestamp(System.currentTimeMillis());
			use.setUsedOn(timestamp);
			use.setActive(true);
			use.setUpdated(inEdiProcessTime);

			try {
				ContainerUse.staticGetDao().store(use);
				// order-containerUse is one-to-one, so add above set a persistable field on the orderHeader
				OrderHeader.staticGetDao().store(inOrder);
			} catch (DaoException e) {
				LOGGER.error("", e);
			}

		}

		return result;
	}

	// --------------------------------------------------------------------------
	/**
	 * @param inUomId
	 * @param inFacility
	 * @return
	 */
	private UomMaster updateUomMaster(final CrossBatchCsvBean inCsvBean, final Facility inFacility) {
		UomMaster result = null;

		String uomId = inCsvBean.getUom();
		result = inFacility.getUomMaster(uomId);

		if (result == null) {
			result = new UomMaster();
			result.setUomMasterId(uomId);
			inFacility.addUomMaster(result);

			try {
				UomMaster.staticGetDao().store(result);
			} catch (DaoException e) {
				LOGGER.error("", e);
			}
		}

		return result;
	}

	@Override
	protected Set<EventTag> getEventTagsForImporter() {
		return EnumSet.of(EventTag.IMPORT, EventTag.ORDER_CROSSBATCH);
	}
}
