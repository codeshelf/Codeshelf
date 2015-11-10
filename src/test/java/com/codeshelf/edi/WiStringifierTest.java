package com.codeshelf.edi;

import static org.mockito.Mockito.mock;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang.math.RandomUtils;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.generators.FacilityGenerator;
import com.codeshelf.generators.WorkInstructionGenerator;
import com.codeshelf.model.dao.ITypedDao;
import com.codeshelf.model.domain.Facility;
import com.codeshelf.model.domain.WorkInstruction;
import com.codeshelf.model.domain.Worker;
import com.codeshelf.testframework.MockDaoTest;

public class WiStringifierTest extends MockDaoTest {
	private static final Logger			LOGGER		= LoggerFactory.getLogger(WiStringifierTest.class);
	
	private FacilityGenerator			facilityGenerator;
	private WorkInstructionGenerator	wiGenerator	= new WorkInstructionGenerator();

	@Override
	public void doBefore() {
		super.doBefore();
		facilityGenerator = new FacilityGenerator();
	}

	@SuppressWarnings("unchecked")
	@Test
	public void workInstructionMessageContent() throws IOException, InterruptedException {
		beginTransaction();
		WiBeanStringifier subject = new WiBeanStringifier();

		Facility facility = facilityGenerator.generateValid();

		LOGGER.info("1: Make the work instruction");
		WorkInstruction wi = generateValidWorkInstruction(facility, nextUniquePastTimestamp());
		String orderId = wi.getOrderId();
		String facilityId = wi.getFacility().getDomainId();
		LOGGER.info("2: Extract as we would send it");
		// format calls WorkInstructionCSVExporter.exportWorkInstructions() with the list of one wi
		// The result is a header line, and the work instruction line.
		
		@SuppressWarnings("rawtypes")
		ITypedDao dao = mock(ITypedDao.class);
		useCustomDao(Worker.class, dao);
		
		String messageBody = subject.stringifyWorkInstruction(wi);

		// v18 and early v19  came like this
		// "facilityId","workInstructionId","type","status","orderGroupId","orderId","containerId","itemId","uom","lotId","locationId","pickerId","planQuantity","actualQuantity","cheId","assigned","started","completed","version-1.0"
		// "F1","143863495449100","ACTUAL","NEW","OG1","OH1","CONTID","ITEMID","UOMID","","F1.A1",,"5","0","CHE1","129044476-12-12T16:01:47Z","2015-08-03T13:49:09Z","2015-08-03T13:49:14Z",""
		LOGGER.info(messageBody);

		// let's verify that the first field of the header is facilityId
		// Our export seems to surround by quotes even when not needed.
		LOGGER.info("3: Parse the first line of message. Check the 0th and 5th column names");
		BufferedReader br = new BufferedReader(new StringReader(messageBody));
		String line1 = br.readLine();

		List<String> titleList = new ArrayList<String>(Arrays.asList(line1.split(",")));
		String title = titleList.get(0);
		Assert.assertEquals("\"facilityId\"", title);
		title = titleList.get(5);
		Assert.assertEquals("\"orderId\"", title);

		
		LOGGER.info("3: Parse the second line of message. Check the 0th and 5th field values");
		String line2 = br.readLine();
		List<String> fieldList = new ArrayList<String>(Arrays.asList(line2.split(",")));
		String field = fieldList.get(0);
		Assert.assertEquals("\""+facilityId + "\"", field);
		field = fieldList.get(5);
		Assert.assertEquals("\""+orderId + "\"", field);

		commitTransaction();
	}

	private WorkInstruction generateValidWorkInstruction(Facility facility, Timestamp timestamp) {
		WorkInstruction wi = wiGenerator.generateValid(facility);
		wi.setAssigned(timestamp);
		return wi;
	}

	private Timestamp nextUniquePastTimestamp() {
		return new Timestamp(System.currentTimeMillis() - Math.abs(RandomUtils.nextLong()));
	}

}
