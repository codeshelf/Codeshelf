package com.codeshelf.service;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyList;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.StringReader;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.commons.lang.math.RandomUtils;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.internal.stubbing.answers.DoesNothing;
import org.mockito.internal.stubbing.answers.ThrowsException;
import org.mockito.invocation.InvocationOnMock;

import com.codeshelf.edi.ICsvInventoryImporter;
import com.codeshelf.edi.IEdiExportServiceProvider;
import com.codeshelf.edi.VirtualSlottedFacilityGenerator;
import com.codeshelf.edi.WorkInstructionCSVExporter;
import com.codeshelf.flyweight.command.ColorEnum;
import com.codeshelf.generators.FacilityGenerator;
import com.codeshelf.generators.WorkInstructionGenerator;
import com.codeshelf.model.OrderStatusEnum;
import com.codeshelf.model.WiSetSummary;
import com.codeshelf.model.WorkInstructionStatusEnum;
import com.codeshelf.model.dao.DaoException;
import com.codeshelf.model.dao.ITypedDao;
import com.codeshelf.model.domain.Che;
import com.codeshelf.model.domain.CodeshelfNetwork;
import com.codeshelf.model.domain.Facility;
import com.codeshelf.model.domain.IEdiService;
import com.codeshelf.model.domain.Item;
import com.codeshelf.model.domain.Location;
import com.codeshelf.model.domain.OrderDetail;
import com.codeshelf.model.domain.OrderHeader;
import com.codeshelf.model.domain.WorkInstruction;
import com.codeshelf.testframework.ServerTest;
import com.codeshelf.util.ConverterProvider;
import com.codeshelf.validation.InputValidationException;
import com.codeshelf.ws.jetty.protocol.message.IMessageProcessor;
import com.codeshelf.ws.jetty.protocol.request.ServiceMethodRequest;
import com.codeshelf.ws.jetty.protocol.response.ResponseABC;
import com.codeshelf.ws.jetty.protocol.response.ServiceMethodResponse;
import com.codeshelf.ws.jetty.server.ServerMessageProcessor;
import com.codeshelf.ws.jetty.server.UserSession;
import com.google.common.collect.ImmutableList;

public class InventoryServiceTest extends ServerTest {

	private FacilityGenerator facilityGenerator;
	
	UUID facilityForVirtualSlottingId;
	
	@Override
	public void doBefore() {
		super.doBefore();
		this.getTenantPersistenceService().beginTransaction();
		facilityGenerator = new FacilityGenerator(getDefaultTenant());

		VirtualSlottedFacilityGenerator facilityGenerator =
					new VirtualSlottedFacilityGenerator(getDefaultTenant(),
														createAisleFileImporter(),
														createLocationAliasImporter(),
														createOrderImporter());
		
		Facility facilityForVirtualSlotting = facilityGenerator.generateFacilityForVirtualSlotting(testName.getMethodName());
		
		this.facilityForVirtualSlottingId = facilityForVirtualSlotting.getPersistentId();
		
		this.getTenantPersistenceService().commitTransaction();
	}

	@Override
	public boolean ephemeralServicesShouldStartAutomatically() {
		return false; // in this test, we start services manually after defining the work service to start
	}
	
	@Test
	public void createInventoryService() {
		//FIXME - huffa - How to do this correctly?
		LightService ls = new LightService(this.sessionManagerService);
		this.inventoryService = new InventoryService(ls);
		this.initializeEphemeralServiceManager();
		
		this.getTenantPersistenceService().beginTransaction();
		Facility facilityForVirtualSlotting = Facility.staticGetDao().findByPersistentId(this.facilityForVirtualSlottingId);

		String csvString = "itemId,locationId,description,quantity,uom,inventoryDate,cmFromLeft,gtin\r\n" 	//
				+ "1120,D402,12/16 oz Bowl Lids -PLA Compostable,6,EA,6/25/14 12:00,135,100\r\n" 			//
				+ "1121,D502,12/18 oz Bowl Lids -PLA Compostable,6,EA,6/25/14 12:00,8,101\r\n"				//
				+ "1122,D503,12/20 oz Bowl Lids -PLA Compostable,6,CS,6/25/14 12:00,55,102\r\n";			//
		Facility facility = setupInventoryData(facilityForVirtualSlotting, csvString);
		Assert.assertNotNull(facility);
		
		CodeshelfNetwork network = facility.getNetworks().get(0);
		Che che1 = network.getChe("CHE1");
		Assert.assertNotNull(che1);
		che1.setColor(ColorEnum.GREEN);
		
		// Try moving and existing item
		inventoryService.moveOrCreateInventory("101", "D403", che1.getPersistentId());
		Location locationD401 = facility.findSubLocationById("D403");
		Assert.assertNotNull(locationD401);
		Location locationD502 = facility.findSubLocationById("D502");
		Assert.assertNotNull(locationD502);
		
		Item item1121D401 = locationD401.getStoredItemFromMasterIdAndUom("1121", "EA");
		Assert.assertNotNull(item1121D401);
		
		Item item1120D502 = locationD502.getStoredItemFromMasterIdAndUom("1121", "EA");
		Assert.assertNull(item1120D502);
		
		// Try creating a new item
		inventoryService.moveOrCreateInventory("201", "D100", che1.getPersistentId());
		
		Location locationD100 = facility.findSubLocationById("D100");
		Assert.assertNotNull(locationD100);
		
		Item item201D100 = locationD100.getStoredItemFromMasterIdAndUom("201", "EA");
		Assert.assertNotNull(item201D100);
		
		this.getTenantPersistenceService().commitTransaction();
		
	}
	
	private Facility setupInventoryData(Facility facility, String csvString) {
		Timestamp ediProcessTime = new Timestamp(System.currentTimeMillis());
		ICsvInventoryImporter importer = createInventoryImporter();
		importer.importSlottedInventoryFromCsvStream(new StringReader(csvString), facility, ediProcessTime);
		return facility.getDao().findByPersistentId(facility.getPersistentId());
	}
}