package com.gadgetworks.codeshelf.edi;

import static org.mockito.Mockito.mock;

import java.io.IOException;
import java.io.StringReader;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.beanutils.PropertyUtils;
import org.junit.Assert;
import org.junit.Test;

import au.com.bytecode.opencsv.CSVReader;

import com.gadgetworks.codeshelf.model.WorkInstructionStatusEnum;
import com.gadgetworks.codeshelf.model.WorkInstructionTypeEnum;
import com.gadgetworks.codeshelf.model.dao.ITypedDao;
import com.gadgetworks.codeshelf.model.domain.Aisle;
import com.gadgetworks.codeshelf.model.domain.Container;
import com.gadgetworks.codeshelf.model.domain.DomainTestABC;
import com.gadgetworks.codeshelf.model.domain.Facility;
import com.gadgetworks.codeshelf.model.domain.IDomainObject;
import com.gadgetworks.codeshelf.model.domain.ItemMaster;
import com.gadgetworks.codeshelf.model.domain.LocationAlias;
import com.gadgetworks.codeshelf.model.domain.OrderDetail;
import com.gadgetworks.codeshelf.model.domain.OrderGroup;
import com.gadgetworks.codeshelf.model.domain.OrderHeader;
import com.gadgetworks.codeshelf.model.domain.Point;
import com.gadgetworks.codeshelf.model.domain.SubLocationABC;
import com.gadgetworks.codeshelf.model.domain.UomMaster;
import com.gadgetworks.codeshelf.model.domain.WorkInstruction;
import com.google.common.collect.ImmutableList;

public class WorkInstructionCSVExporterTest extends DomainTestABC {

	private Facility facility;
	private WorkInstructionCSVExporter exporter;
	
	private static final String     TIME_FORMAT			= "yyyy-MM-dd'T'HH:mm:ss'Z'";
	
	private String[] expectedHeaders = new String[]{
			"domainId",
			"type",
			"status",
			"orderGroupId",
			"orderId",
			"containerId",
			"itemId",
			"locationId",
			"pickerId",
			"planQuantity",
			"actualQuantity",
			"assigned",
			"started",
			"completed"	
	};

	String[] dateFields = new String[]{
			"assigned",
			"started",
			"completed"
	};
	
	private DateFormat timestampFormat = new SimpleDateFormat(TIME_FORMAT);
	
	public void doBefore() {
		facility = createFacility(this.getClass().toString() + System.currentTimeMillis());
		exporter  = new WorkInstructionCSVExporter();
	}
	
	@Test
	public void generatesCSV() throws IOException {
		WorkInstruction testWi = generateValidFullWorkInstruction();
		WorkInstruction testWi2 = generateValidFullWorkInstruction();
		List<WorkInstruction> wiList = ImmutableList.of(testWi, testWi2);
		List<String[]> table = toTable(wiList);

		Assert.assertEquals(1 + wiList.size(), table.size());
		//header row
		assertHeaderRow(table.get(0));
		//data row
		String[] dataRow = table.get(1);
		Assert.assertEquals(expectedHeaders.length, dataRow.length);
		for (int i = 0; i < dataRow.length; i++) {
			String dataField = dataRow[i];
			Assert.assertNotNull("Data Field Value was null at position: " + i, dataField);
			Assert.assertNotEquals("Data Field Value was empty at position: " + i, "", dataField.trim());
			
		}

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
		
		WorkInstruction testWi = generateValidFullWorkInstruction();
		WorkInstruction testWi2 = generateValidFullWorkInstruction();
		List<WorkInstruction> wiList = ImmutableList.of(testWi, testWi2);
		List<String[]> table = toTable(wiList);
		Iterator<WorkInstruction> workInstructions = wiList.iterator();
		Iterator<String[]> dataRows = table.iterator();
		dataRows.next(); //skip header
		for (String[] dataRow : ImmutableList.copyOf(dataRows)) {
			assertEachDateField(workInstructions.next(), dataRow);
		}
	}

	@Test
	public void usesLocationIdWhenNoAlias() throws Exception {
		String expectedValue = "TESTDOMAINID";
		List<LocationAlias> emptyAliases = Collections.<LocationAlias>emptyList();
		SubLocationABC<?> noAliasLocation = mockSubLocation("ID_FROM_LOCATION"); 
		noAliasLocation.setAliases(emptyAliases);
		
		WorkInstruction testWi = generateValidFullWorkInstruction();
		testWi.setLocation(noAliasLocation);
		testWi.setLocationId(expectedValue);
			
		List<WorkInstruction> wiList = ImmutableList.of(testWi);
		List<String[]> table = toTable(wiList);
		String[] dataRow = table.get(1);
		assertField(dataRow, "locationId", expectedValue);
	}

	@Test
	public void usesFirstLocationAliasIfAvailable() throws Exception {
		String expectedValue = "ALIASID";
		SubLocationABC<?> aliasedLocation = mockSubLocation("NOT_EXPECTED");
		aliasedLocation.setAliases(ImmutableList.of(
			new LocationAlias(facility, expectedValue, aliasedLocation),
			new LocationAlias(facility, "NOTEXPECTED", aliasedLocation)));
		WorkInstruction testWi = generateValidFullWorkInstruction();
		testWi.setLocation(aliasedLocation);
		testWi.setLocationId("LOCATIONID_NOTEXPECTED");
			
		List<WorkInstruction> wiList = ImmutableList.of(testWi);
		List<String[]> table = toTable(wiList);
		String[] dataRow = table.get(1);
		assertField(dataRow, "locationId", expectedValue);
	}

	
	@Test
	public void usesOrderDomainId() throws Exception {
		String expectedValue = "TESTDOMAINID";
		
		
		WorkInstruction testWi = generateValidFullWorkInstruction();
		testWi.getOrderDetail().setParent(new OrderHeader(facility, expectedValue));
		List<WorkInstruction> wiList = ImmutableList.of(testWi);
		List<String[]> table = toTable(wiList);
		String[] dataRow = table.get(1);
		assertField(dataRow, "orderId", expectedValue);
	}

	
	@Test
	public void usesOrderGroupDomainId() throws Exception {
		String expectedValue = "TESTDOMAINID";
		
		
		WorkInstruction testWi = generateValidFullWorkInstruction();
		testWi.getOrderDetail().getParent().setOrderGroup(new OrderGroup(facility, expectedValue));
		List<WorkInstruction> wiList = ImmutableList.of(testWi);
		List<String[]> table = toTable(wiList);
		String[] dataRow = table.get(1);
		assertField(dataRow, "orderGroupId", expectedValue);
	}
	
	@Test
	public void orderGroupIdOptional() throws Exception {
		
		WorkInstruction testWi = generateValidFullWorkInstruction();
		testWi.getOrderDetail().getParent().setOrderGroup(null);
		List<WorkInstruction> wiList = ImmutableList.of(testWi);
		List<String[]> table = toTable(wiList);
		String[] dataRow = table.get(1);
		assertField(dataRow, "orderGroupId", "");
	}

	
	private WorkInstruction generateValidFullWorkInstruction() {
		WorkInstruction workInstruction = new WorkInstruction();
		OrderHeader orderHeader = new OrderHeader(facility, "OH1");
		orderHeader.setOrderGroup(new OrderGroup(facility, "OG1"));
		workInstruction.setParent(facility);
		workInstruction.setOrderDetail(new OrderDetail(orderHeader, "OD1"));
		workInstruction.setDomainId("WIDOMAINID");
		workInstruction.setContainer(new Container(facility, "C1"));
		workInstruction.setItemMaster(new ItemMaster(facility, "ITEMID", new UomMaster(facility, "UOMID")));
		workInstruction.setLocationId("LOCID");
		workInstruction.setLocation(new Aisle(facility, "A1", Point.getZeroPoint(), Point.getZeroPoint()));
		workInstruction.setPickerId("Picker");
		workInstruction.setTypeEnum(WorkInstructionTypeEnum.ACTUAL);
		workInstruction.setStatusEnum(WorkInstructionStatusEnum.COMPLETE);
		workInstruction.setPlanQuantity(2);
		workInstruction.setActualQuantity(2);
		workInstruction.setAssigned( new Timestamp(System.currentTimeMillis()-10000));
		workInstruction.setStarted(  new Timestamp(System.currentTimeMillis()-5000));
		workInstruction.setCompleted(new Timestamp(System.currentTimeMillis()-0000));
		return workInstruction;
	}
	
	private void assertHeaderRow(String [] header) {
		Arrays.sort(header);
		Arrays.sort(expectedHeaders);
		Assert.assertArrayEquals(expectedHeaders, header);
		
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
		return new SubLocationABC<Facility>(facility, domainId, mock(Point.class), mock(Point.class)) {

			/**
			 * 
			 */
			private static final long	serialVersionUID	= 1L;

			@Override
			public String getDefaultDomainIdPrefix() {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public <T extends IDomainObject> ITypedDao<T> getDao() {
				// TODO Auto-generated method stub
				return null;
			}}; 
	}
}