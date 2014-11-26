package com.gadgetworks.codeshelf.edi;

import static org.mockito.Mockito.mock;

import java.io.IOException;
import java.io.StringReader;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import org.apache.commons.beanutils.PropertyUtils;
import org.junit.Assert;
import org.junit.Test;

import au.com.bytecode.opencsv.CSVReader;

import com.gadgetworks.codeshelf.generators.WorkInstructionGenerator;
import com.gadgetworks.codeshelf.model.dao.ITypedDao;
import com.gadgetworks.codeshelf.model.domain.DomainTestABC;
import com.gadgetworks.codeshelf.model.domain.Facility;
import com.gadgetworks.codeshelf.model.domain.IDomainObject;
import com.gadgetworks.codeshelf.model.domain.OrderGroup;
import com.gadgetworks.codeshelf.model.domain.OrderHeader;
import com.gadgetworks.codeshelf.model.domain.Point;
import com.gadgetworks.codeshelf.model.domain.SubLocationABC;
import com.gadgetworks.codeshelf.model.domain.WorkInstruction;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

public class WorkInstructionCSVExporterTest extends DomainTestABC {

	private UUID facilityId;
	
	private WorkInstructionCSVExporter exporter;
	private WorkInstructionGenerator wiGenerator = new WorkInstructionGenerator();
	
	private static final String     TIME_FORMAT			= "yyyy-MM-dd'T'HH:mm:ss'Z'";
	
	private String[] expectedHeaders = new String[]{
			"facilityId",
			"workInstructionId",
			"type",
			"status",
			"orderGroupId",
			"orderId",
			"containerId",
			"itemId",
			"uom",
			"lotId",
			"locationId",
			"pickerId",
			"planQuantity",
			"actualQuantity",
			"cheId",
			"assigned",
			"started",
			"completed",
			"version-1.0"
	};

	String[] dateFields = new String[]{
			"assigned",
			"started",
			"completed"
	};
	
	private DateFormat timestampFormat = new SimpleDateFormat(TIME_FORMAT);
	
	public void doBefore() {
		this.getPersistenceService().beginTenantTransaction();
		Facility facility = createDefaultFacility(this.getClass().toString() + System.currentTimeMillis());
		exporter  = new WorkInstructionCSVExporter();
		
		this.facilityId = facility.getPersistentId();
		this.getPersistenceService().endTenantTransaction();
	}
	
	@Test
	public void generatesCSV() throws IOException {
		this.getPersistenceService().beginTenantTransaction();
		Facility facility = Facility.DAO.findByPersistentId(this.facilityId);

		WorkInstruction testWi = generateValidFullWorkInstruction(facility);
		WorkInstruction testWi2 = generateValidFullWorkInstruction(facility);
		List<WorkInstruction> wiList = ImmutableList.of(testWi, testWi2);
		List<String[]> table = toTable(wiList);

		Assert.assertEquals(1 + wiList.size(), table.size());
		//header row
		assertHeaderRow(table.get(0));
		//data row
		String[] dataRow = table.get(1);
		Assert.assertEquals(expectedHeaders.length, dataRow.length);
		List<String> headers = Lists.newArrayList(expectedHeaders);
		for (int i = 0; i < dataRow.length; i++) {
			String dataField = dataRow[i];
			System.out.println(i+ " , " + dataField);
			Assert.assertNotNull("Data Field Value was null at position: " + i, dataField);
			if (i != headers.indexOf("lotId") &&
				i != headers.indexOf("version-1.0")) {
				Assert.assertNotEquals("Data Field Value was empty at position: " + i, "", dataField.trim());
			}
		}
		
		this.getPersistenceService().endTenantTransaction();
	}

	private List<String[]> toTable(List<WorkInstruction> wiList) throws IOException {
		String output = exporter.exportWorkInstructions(wiList);
		try(CSVReader reader = new CSVReader(new StringReader(output));) {
			List<String[]> table = reader.readAll();
			return table;
		}
	}
	
	@Test
	public void dateFieldsAreISO8601UTC() throws Exception {
		this.getPersistenceService().beginTenantTransaction();
		Facility facility = Facility.DAO.findByPersistentId(this.facilityId);

		WorkInstruction testWi = generateValidFullWorkInstruction(facility);
		WorkInstruction testWi2 = generateValidFullWorkInstruction(facility);
		List<WorkInstruction> wiList = ImmutableList.of(testWi, testWi2);
		List<String[]> table = toTable(wiList);
		Iterator<WorkInstruction> workInstructions = wiList.iterator();
		Iterator<String[]> dataRows = table.iterator();
		dataRows.next(); //skip header
		for (String[] dataRow : ImmutableList.copyOf(dataRows)) {
			assertEachDateField(workInstructions.next(), dataRow);
		}		
		this.getPersistenceService().endTenantTransaction();

	}

	
	@Test
	public void missingUomMasterReturnsEmpty() throws IOException {
		this.getPersistenceService().beginTenantTransaction();
		Facility facility = Facility.DAO.findByPersistentId(this.facilityId);

		String expectedValue = "TESTDOMAINID";
		
		
		WorkInstruction testWi = generateValidFullWorkInstruction(facility);
		testWi.getOrderDetail().setParent(new OrderHeader(facility, expectedValue));
		testWi.getOrderDetail().setUomMaster(null);
		System.out.println(testWi);	

		List<WorkInstruction> wiList = ImmutableList.of(testWi);
		List<String[]> table = toTable(wiList);
		String[] dataRow = table.get(1);
		assertField(dataRow, "uom", "");		
		this.getPersistenceService().endTenantTransaction();
		
	}

	
	@Test
	public void usesOrderDomainId() throws Exception {
		this.getPersistenceService().beginTenantTransaction();
		Facility facility = Facility.DAO.findByPersistentId(this.facilityId);

		String expectedValue = "TESTDOMAINID";
		
		
		WorkInstruction testWi = generateValidFullWorkInstruction(facility);
		testWi.getOrderDetail().setParent(new OrderHeader(facility, expectedValue));
		List<WorkInstruction> wiList = ImmutableList.of(testWi);
		List<String[]> table = toTable(wiList);
		String[] dataRow = table.get(1);
		assertField(dataRow, "orderId", expectedValue);		
		this.getPersistenceService().endTenantTransaction();

	}

	
	@Test
	public void usesOrderGroupDomainId() throws Exception {
		this.getPersistenceService().beginTenantTransaction();
		Facility facility = Facility.DAO.findByPersistentId(this.facilityId);

		String expectedValue = "TESTDOMAINID";
		
		
		WorkInstruction testWi = generateValidFullWorkInstruction(facility);
		testWi.getOrderDetail().getParent().setOrderGroup(new OrderGroup(facility, expectedValue));
		List<WorkInstruction> wiList = ImmutableList.of(testWi);
		List<String[]> table = toTable(wiList);
		String[] dataRow = table.get(1);
		assertField(dataRow, "orderGroupId", expectedValue);		
		this.getPersistenceService().endTenantTransaction();

	}
	
	@Test
	public void orderGroupIdOptional() throws Exception {
		this.getPersistenceService().beginTenantTransaction();
		Facility facility = Facility.DAO.findByPersistentId(this.facilityId);

		WorkInstruction testWi = generateValidFullWorkInstruction(facility);
		testWi.getOrderDetail().getParent().setOrderGroup(null);
		List<WorkInstruction> wiList = ImmutableList.of(testWi);
		List<String[]> table = toTable(wiList);
		String[] dataRow = table.get(1);
		assertField(dataRow, "orderGroupId", "");		
		this.getPersistenceService().endTenantTransaction();

	}

	@Test
	public void nullQuantityReturnsEmpty() throws Exception {
		this.getPersistenceService().beginTenantTransaction();
		Facility facility = Facility.DAO.findByPersistentId(this.facilityId);

		WorkInstruction testWi = generateValidFullWorkInstruction(facility);
		testWi.setPlanMaxQuantity(null);
		testWi.setPlanMinQuantity(null);
		testWi.setPlanQuantity(null);
		testWi.setActualQuantity(null);
		List<WorkInstruction> wiList = ImmutableList.of(testWi);
		List<String[]> table = toTable(wiList);
		String[] dataRow = table.get(1);
		assertField(dataRow, "actualQuantity", "");
		assertField(dataRow, "planQuantity", "");

		this.getPersistenceService().endTenantTransaction();
	}

	private WorkInstruction generateValidFullWorkInstruction(Facility facility) {
		return wiGenerator.generateValid(facility);
	}
	
	private void assertHeaderRow(String [] header) {
		Arrays.sort(header);
		String[] sortedExpectedHeaders = Arrays.copyOf(expectedHeaders, expectedHeaders.length);
		Arrays.sort(sortedExpectedHeaders );
		Assert.assertArrayEquals(sortedExpectedHeaders , header);
		
	}
	
	private void assertEachDateField(WorkInstruction wi, String[] row) throws Exception {
		for (String dateField : dateFields) {
			Timestamp wiValue = (Timestamp) PropertyUtils.getProperty(wi, dateField);
			assertField(row, dateField, timestampFormat.format(wiValue));
		}
	}

	private void assertField(String[] row, String fieldName, String expectedValue) {
		int fieldPosition = Arrays.asList(expectedHeaders).indexOf(fieldName);
		Assert.assertTrue(fieldPosition >=0);
		String fieldValue = row[fieldPosition];
		Assert.assertEquals(expectedValue,  fieldValue);

	}
	
	private SubLocationABC<Facility> mockSubLocation(String domainId) {
		
		SubLocationABC<Facility> mockLocation = new SubLocationABC<Facility>() {

			@Override
			public String getDefaultDomainIdPrefix() {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public <T extends IDomainObject> ITypedDao<T> getDao() {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public void setParent(Facility inParent) {
				// TODO Auto-generated method stub
				
			}}; 
			
		mockLocation.setDomainId(domainId);
		mockLocation.setDescription("");
		mockLocation.setAnchorPoint(mock(Point.class));
		mockLocation.setPickFaceEndPoint(mock(Point.class));
			
		return mockLocation;
	}
}
