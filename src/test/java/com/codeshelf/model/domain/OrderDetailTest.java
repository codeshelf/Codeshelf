package com.codeshelf.model.domain;

import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Matchers;
import org.mockito.Mockito;

import com.codeshelf.model.OrderStatusEnum;
import com.codeshelf.model.WorkInstructionStatusEnum;
import com.codeshelf.model.dao.ITypedDao;
import com.codeshelf.testframework.MinimalTest;

public class OrderDetailTest extends MinimalTest {

	ITypedDao<OrderDetail> mockDao;
	private OrderDetail	subject;
	private Integer	testQuantity;
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Before
	public void setUp() {
		testQuantity = 5;
		mockDao = Mockito.<ITypedDao>mock(ITypedDao.class);
		OrderDetail.DAO = mockDao;

		subject = new OrderDetail();
		subject.setQuantities(testQuantity);

	}
	
	private void verifyStorage() {
		verify(mockDao, atLeastOnce()).store(Matchers.<OrderDetail>any(OrderDetail.class));
	}
	
	/**
	 * Given a new WI added to order detail  
	 * When reevaluateStatus
	 * Then status = INPROGRESS
	 */
	@Test
	public void newWI() {
		
		WorkInstruction wi = new WorkInstruction();
		wi.setStatus(WorkInstructionStatusEnum.NEW);
		subject.addWorkInstruction(wi);
		
		subject.reevaluateStatus();
		
		Assert.assertEquals(OrderStatusEnum.INPROGRESS, subject.getStatus());
		verifyStorage();
	}
	
	/**
	 * Given a short WI added to order detail  
	 * When reevaluateStatus
	 * Then status = SHORT
	 */
	@Test
	public void shortWI() {
		WorkInstruction wi = new WorkInstruction();
		wi.setStatus(WorkInstructionStatusEnum.SHORT);
		wi.setActualQuantity(testQuantity - 1);
		subject.addWorkInstruction(wi);
		
		subject.reevaluateStatus();
		
		Assert.assertEquals(OrderStatusEnum.SHORT, subject.getStatus());
		verifyStorage();

	}

	/**
	 * Given a complete WI added to order detail  
	 * When reevaluateStatus
	 * Then status = COMPLETE
	 */
	@Test
	public void completeWI() {
		WorkInstruction wi = new WorkInstruction();
		wi.setStatus(WorkInstructionStatusEnum.COMPLETE);
		wi.setActualQuantity(testQuantity);
		subject.addWorkInstruction(wi);
		
		subject.reevaluateStatus();
		
		Assert.assertEquals(OrderStatusEnum.COMPLETE, subject.getStatus());
		verifyStorage();

	}

	
	/**
	 * Given order detail with no work instructions
	 * And not completed
	 * When reevaluateStatus
	 * Then status = the same
	 */
	@Test
	public void noWI() {
		OrderStatusEnum priorStatus = subject.getStatus();
		subject.reevaluateStatus();
		
		Assert.assertEquals(priorStatus, subject.getStatus());
	}
	
	/**
	 * Given orderDetail that had one shorted instructions
	 * And another new instruction to finish
	 * When reevaluateStatus
	 * Then status = INPROGRESS
	 */
	@Test
	public void oneShortOneNewWI() {
		WorkInstruction shortWi = new WorkInstruction();
		shortWi.setStatus(WorkInstructionStatusEnum.SHORT);
		shortWi.setActualQuantity(testQuantity - 1);
		subject.addWorkInstruction(shortWi);
		
		WorkInstruction newWi = new WorkInstruction();
		newWi.setStatus(WorkInstructionStatusEnum.NEW);
		subject.addWorkInstruction(newWi);
		
		
		subject.reevaluateStatus();
		
		Assert.assertEquals(OrderStatusEnum.INPROGRESS, subject.getStatus());
		verifyStorage();

	}
	
	/**
	 * Given orderDetail that had one shorted instructions
	 * And another new instruction to finish
	 * When reevaluateStatus
	 * Then status = INPROGRESS
	 */
	@Test
	public void oneShortOneCompleteWI() {
		WorkInstruction shortWi = new WorkInstruction();
		shortWi.setStatus(WorkInstructionStatusEnum.SHORT);
		shortWi.setActualQuantity(testQuantity - 1);
		subject.addWorkInstruction(shortWi);
		
		WorkInstruction completeWi = new WorkInstruction();
		completeWi.setStatus(WorkInstructionStatusEnum.COMPLETE);
		completeWi.setActualQuantity(testQuantity);
		subject.addWorkInstruction(completeWi);
		
		
		subject.reevaluateStatus();
		
		Assert.assertEquals(OrderStatusEnum.COMPLETE, subject.getStatus());
		verifyStorage();

	}	

}
