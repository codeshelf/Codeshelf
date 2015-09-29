/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2014, Codeshelf All rights reserved
  *******************************************************************************/
package com.codeshelf.edi;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.sql.Timestamp;

import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.model.domain.DropboxGateway;
import com.codeshelf.model.domain.Facility;
// import com.codeshelf.model.domain.LedController;
import com.codeshelf.model.domain.OrderHeader;
import com.codeshelf.model.domain.Point;
import com.codeshelf.testframework.ServerTest;
import com.codeshelf.validation.BatchResult;

/**
 * @author jon ranstrom
 *
 */
public class DropboxRealTest extends ServerTest {
	private static final Logger			LOGGER				= LoggerFactory.getLogger(DropboxRealTest.class);

	private ICsvOrderImporter			mCsvOrderImporter;
	private ICsvInventoryImporter		mCsvInventoryImporter;
	private ICsvLocationAliasImporter	mCsvLocationAliasImporter;
	private AislesFileCsvImporter		mAislesFileCsvImporter;
	private ICsvCrossBatchImporter		mCsvCrossBatchImporter;
	private ICsvOrderLocationImporter	mCsvOrderLocationImporter;

	// This obtained by jon Oct. 5 2014.  Pretty easy. Just run server normally. Dropbox link, etc.  Then later, pull this from the provider_credentials field in the database.
	@SuppressWarnings("unused")
	private final static String			TEST_CREDENTIALS3	= "rMS4NWXXc90AAAAAAAAAbXlihAPhBC7TafUSn3Tla4H3U43UauXCuWsFA7U3K-1U";

	// To continue the dropbox investigation
	// 1) Make sure you have your local dropbox working
	// 2) Change LOCAL_DROPBOX_DIR
	// 3) Uncomment @Test on the test functions below. For TeamCity, we only keep the stub testDBX0() as @test.
	// 3b) Does it work? Or do you need to obtain your own credentials for your dropbox account? See above.
	// 4) See DropboxGateway.handleImport(). Very complicated.
	// Any change there requires someone to run these tests again.

	private final static String			LOCAL_DROPBOX_DIR	= "/Users/jonranstrom/Dropbox/Apps/Codeshelf-Interface";

	public DropboxRealTest() {
		super();
	}

	public void doBefore() {
		super.doBefore();

		mCsvOrderImporter = createOrderImporter();

		mCsvInventoryImporter = createInventoryImporter();

		mCsvLocationAliasImporter = createLocationAliasImporter();

		mAislesFileCsvImporter = createAisleFileImporter();

		mCsvCrossBatchImporter = createCrossBatchImporter();

		mCsvOrderLocationImporter = createOrderLocationImporter();
	}

	private Facility setUpStartingFacility(String inOrganizationName) {
		// The organization will get "O-" prepended to the name. Facility F-
		// Caller must use a different organization name each time this is used

		String fName = "F-" + inOrganizationName;
		Facility facility = Facility.createFacility(fName, "TEST", Point.getZeroPoint());

		/*
		LedController controller1 = network.findOrCreateLedController(inOrganizationName, new NetGuid("0x00000011"));
		LedController controller2 = network.findOrCreateLedController(inOrganizationName, new NetGuid("0x00000012"));
		LedController controller3 = network.findOrCreateLedController(inOrganizationName, new NetGuid("0x00000013"));
		*/
		
		facility.createDefaultEDIServices();
		
		return facility;
	}

	/**
	 * Brain-dead wait. No guarantee that dbx and file system are synched
	 */
	private void waitForDbxSynch() {
		try {
			Thread.sleep(1500);
		} catch (InterruptedException e) {
			LOGGER.error("", e);
		}
	}

	@Test
	public final void testDBX0() throws IOException {
		// Just to keep Team City happy
		Integer a = 1;
		Assert.assertEquals(a, (Integer) 1);
	}

	// @Test
	public final void testDBX1() throws IOException {
		// This calls dropboxGateway.getUpdatesFromHost() directly

		String facilityDirPath = "";
		try {

			LOGGER.info("START creation of facility DBX01");
			Facility facility = setUpStartingFacility("DBX01");
			LOGGER.info("facility DBX01 made");

			String csvString2 = "orderGroupId,shipmentId,customerId,preAssignedContainerId,orderId,itemId,description,quantity,uom,orderDate,dueDate,workSequence"
					+ "\r\n1,USF314,COSTCO,12345,12345,1123,12/16 oz Bowl Lids -PLA Compostable,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0"
					+ "\r\n1,USF314,COSTCO,12345,12345,1493,PARK RANGER Doll,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0"
					+ "\r\n1,USF314,COSTCO,12345,12345,1522,SJJ BPP,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0" + "\r\n";

			/*
			 * This all works. Just established the reference results for real dropbox test
			byte[] csvArray2 = csvString2.getBytes();

			ByteArrayInputStream stream2 = new ByteArrayInputStream(csvArray2);
			InputStreamReader reader2 = new InputStreamReader(stream2);

			Timestamp ediProcessTime2 = new Timestamp(System.currentTimeMillis());
			ICsvOrderImporter importer2 = new OutboundOrderCsvImporter(OrderGroup.DAO,
				OrderHeader.DAO,
				OrderDetail.DAO,
				Container.DAO,
				ContainerUse.DAO,
				ItemMaster.DAO,
				UomMaster.DAO);

			LOGGER.info("start simulated orders read");
			importer2.importOrdersFromCsvStream(reader2, facility, ediProcessTime2);
			LOGGER.info("finish simulated orders read");

			// We should have one order with 3 details. Only 2 of which are fulfillable.
			OrderHeader order = facility.getOrderHeader("12345");
			Assert.assertNotNull(order);
			Integer detailCount = order.getOrderDetails().size();
			Assert.assertEquals((Integer) 3, detailCount);
			*/

			DropboxGateway dropboxGateway = facility.getDropboxGateway();

			String ordersDirPath = dropboxGateway.getFacilityImportSubDirPath("orders"); // IMPORT_ORDERS_PATH
			String orderFileFragment = "/testorder.csv";
			String processedFragment = "/processed";
			String fullOrdersDirPath = LOCAL_DROPBOX_DIR + ordersDirPath;
			facilityDirPath = LOCAL_DROPBOX_DIR + dropboxGateway.getFacilityPath();

			// No file present yet
			LOGGER.info("calling dbx getUpdatesFromHost");
			dropboxGateway.getUpdatesFromHost(mCsvOrderImporter,
				mCsvOrderLocationImporter,
				mCsvInventoryImporter,
				mCsvLocationAliasImporter,
				mCsvCrossBatchImporter,
				mAislesFileCsvImporter);
			LOGGER.info("finish dbx getUpdatesFromHost");

			try {
				String csvFilePath = fullOrdersDirPath + orderFileFragment;
				LOGGER.debug("full file path: " + csvFilePath);
				FileUtils.writeStringToFile(new File(csvFilePath), csvString2);
				LOGGER.debug("wrote the file ");

			} catch (IOException e) {
				LOGGER.error("failed to create test orders file", e);
			}

			waitForDbxSynch();

			LOGGER.info("second  call to getUpdatesFromHost, after new orders file written");
			dropboxGateway.getUpdatesFromHost(mCsvOrderImporter,
				mCsvOrderLocationImporter,
				mCsvInventoryImporter,
				mCsvLocationAliasImporter,
				mCsvCrossBatchImporter,
				mAislesFileCsvImporter);
			LOGGER.info("finish second call to getUpdatesFromHost");

			// We should have one order with 3 details.
			OrderHeader order = OrderHeader.staticGetDao().findByDomainId(facility, "12345");
			Assert.assertNotNull(order);
			Integer detailCount = order.getOrderDetails().size();
			Assert.assertEquals((Integer) 3, detailCount);

			waitForDbxSynch();

			// Check that the processed file is there as we expect
			String processedCsvFilePath = fullOrdersDirPath + processedFragment + orderFileFragment;
			LOGGER.debug("delete: " + processedCsvFilePath);
			if (!FileUtils.deleteQuietly(new File(processedCsvFilePath))) {
				Assert.fail("file was not there to delete");
			}

		} finally {
			waitForDbxSynch();

			// Delete the entire dbx03 directory
			if (FileUtils.deleteQuietly(new File(facilityDirPath)))
				LOGGER.debug("deleted entire facility dbx01 folder ");
			else
				LOGGER.error("failed to delete facility dbx01folder ");

		}

	}

	// @Test
	public final void testDBX2() {
		// See DEV-454 and 455 for the purpose of this test.
		// Although as specified, it was to move file in during (slow) processing, this almost mimics it much more easily.
		// - Start processing
		// - Which starts the orders importer
		// - Which reads the files and creates the orders bean list
		// - And process the beans (sometimes slowly, if the DAO is slow
		// - Finish. Anonymous class puts another file back with the same name before the file is moved.
		// - As normal, move the file to processed.
		// DEV-454 says that if the file has the same name, but is different, then don't move it. AND make sure it will be processed.

		String facilityDirPath = "";
		try {

			LOGGER.info("START creation of facility DBX02");
			Facility facility = setUpStartingFacility("DBX02");
			Assert.assertNotNull(facility);
			LOGGER.info("facility DBX02 made");

			DropboxGateway dropboxGateway = facility.getDropboxGateway();

			final String csvString1 = "orderGroupId,shipmentId,customerId,preAssignedContainerId,orderId,itemId,description,quantity,uom,orderDate,dueDate,workSequence"
					+ "\r\n1,USF314,COSTCO,12345,12345,1123,12/16 oz Bowl Lids -PLA Compostable,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0"
					+ "\r\n1,USF314,COSTCO,12345,12345,1493,PARK RANGER Doll,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0"
					+ "\r\n1,USF314,COSTCO,12345,12345,1522,SJJ BPP,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0" + "\r\n";

			String ordersDirPath = dropboxGateway.getFacilityImportSubDirPath("orders"); // IMPORT_ORDERS_PATH
			String orderFileFragment = "/testorder.csv";
			// String processedFragment = "/processed";
			String fullOrdersDirPath = LOCAL_DROPBOX_DIR + ordersDirPath;
			facilityDirPath = LOCAL_DROPBOX_DIR + dropboxGateway.getFacilityPath();

			final String csvFilePath = fullOrdersDirPath + orderFileFragment;
			try {
				LOGGER.debug("full file path: " + csvFilePath);
				FileUtils.writeStringToFile(new File(csvFilePath), csvString1);
				LOGGER.debug("wrote the file ");
			} catch (IOException e) {
				LOGGER.error("failed to create test orders file", e);
			}
			long timeMillisOriginalFileWrite = System.currentTimeMillis();

			waitForDbxSynch();

			// Provide a wrapper for CsvOrderImporter
			ICsvOrderImporter testImporter = new ICsvOrderImporter() {
				@Override
				public int toInteger(final String inString) {
					return 0;
				}
				@Override
				public BatchResult<Object> importOrdersFromCsvStream(Reader inCsvStreamReader,
					Facility inFacility,
					Timestamp inProcessTime) throws IOException {

					BatchResult<Object> result = mCsvOrderImporter.importOrdersFromCsvStream(inCsvStreamReader, inFacility, inProcessTime);
					LOGGER.info("Anonymous Order Importer just finished");
					//file manipulation here
					String csvString2 = csvString1
							+ "1,USF314,COSTCO,12345,12345,1622,Raggety Ann Doll,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0"
							+ "\r\n";
					try {
						LOGGER.debug("full file path: " + csvFilePath);
						FileUtils.writeStringToFile(new File(csvFilePath), csvString2);
						LOGGER.debug("overwrote the file with fourth detail for 12345 ");
					} catch (IOException e) {
						LOGGER.error("failed to overwrite file", e);
					}
					return result;
				}
				
				@Override
				public void persistDataReceipt(Facility facility, String username, String filename, long receivedTime, BatchResult<?> result) {
					//do nothing
				}
				
				@Override
				public void setTruncatedGtins(boolean value) {
					// stub. Don't need to implement					
				}
				
				
			};

			LOGGER.info("START dbx getUpdatesFromHost for DBX02");
			// testImporter instead of mCsvOrderImporter
			dropboxGateway.getUpdatesFromHost(testImporter,
				mCsvOrderLocationImporter,
				mCsvInventoryImporter,
				mCsvLocationAliasImporter,
				mCsvCrossBatchImporter,
				mAislesFileCsvImporter);
			LOGGER.info("FINISH dbx getUpdatesFromHost for DBX02. Did the file move to processed?");

			// We should have one order with 3 details. (The 4th detail is now in the file, but was not as it processed into beans.)
			OrderHeader order = OrderHeader.staticGetDao().findByDomainId(facility, "12345");
			Assert.assertNotNull(order);
			Integer detailCount = order.getOrderDetails().size();
			Assert.assertEquals((Integer) 3, detailCount);

			if (FileUtils.isFileNewer(new File(csvFilePath), timeMillisOriginalFileWrite))
				LOGGER.info("as expected, file system sees the overwritten file as newer");
			else
				LOGGER.error("file system does not see the overwritten file as newer"); // never see this

			waitForDbxSynch();

			LOGGER.info("START dbx getUpdatesFromHost for DBX02 after new file came");
			// use the normal mCsvOrderImporter as we don't want to inject a new file again
			dropboxGateway.getUpdatesFromHost(mCsvOrderImporter,
				mCsvOrderLocationImporter,
				mCsvInventoryImporter,
				mCsvLocationAliasImporter,
				mCsvCrossBatchImporter,
				mAislesFileCsvImporter);
			LOGGER.info("FINISH getUpdatesFromHost. Moved .processing to processed folder, then rename. File of that name exists so should get a different file name");

			// The 4th detail should have been read now)
			order = OrderHeader.staticGetDao().findByDomainId(facility, "12345");

			Assert.assertNotNull(order);
			detailCount = order.getOrderDetails().size();
			Assert.assertEquals((Integer) 4, detailCount);

			// We have two files in processed now. One is testorder.csv. The other is something like testorder.09-26-56.csv that we cannot easily check for
			// just let the finally clean up

		} finally {
			waitForDbxSynch();

			// Delete the entire dbx03 directory
			if (FileUtils.deleteQuietly(new File(facilityDirPath)))
				LOGGER.debug("deleted entire facility dbx02 folder ");
			else
				LOGGER.error("failed to delete facility dbx02 folder ");

		}

	}

	// @Test
	public final void testDBX3() {
		// Edge cases
		// Make sure leftover .processing file is processed and moved normally to /processed folder
		// Make sure leftover .FAILED file is not processed.

		String facilityDirPath = "";

		try {

			Facility facility = setUpStartingFacility("DBX03");
			Assert.assertNotNull(facility);
			LOGGER.info("facility DBX03 made");

			DropboxGateway dropboxGateway = facility.getDropboxGateway();

			final String csvString1 = "orderGroupId,shipmentId,customerId,preAssignedContainerId,orderId,itemId,description,quantity,uom,orderDate,dueDate,workSequence"
					+ "\r\n1,USF314,COSTCO,12345,12345,1123,12/16 oz Bowl Lids -PLA Compostable,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0"
					+ "\r\n1,USF314,COSTCO,12345,12345,1493,PARK RANGER Doll,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0"
					+ "\r\n1,USF314,COSTCO,12345,12345,1522,SJJ BPP,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0" + "\r\n";

			String ordersDirPath = dropboxGateway.getFacilityImportSubDirPath("orders"); // IMPORT_ORDERS_PATH
			String orderFileFragment = "/testorder.csv";
			String processingFileFragment = "/testorder.csv.processing";
			String failedFileFragment = "/testorder.csv.FAILED";
			String processedFragment = "/processed";
			String fullOrdersDirPath = LOCAL_DROPBOX_DIR + ordersDirPath;
			facilityDirPath = LOCAL_DROPBOX_DIR + dropboxGateway.getFacilityPath();

			final String csvFilePath = fullOrdersDirPath + processingFileFragment;
			try {
				LOGGER.debug("writing new file: " + csvFilePath);
				FileUtils.writeStringToFile(new File(csvFilePath), csvString1);
			} catch (IOException e) {
				LOGGER.error("failed to create test orders file", e);
			}

			waitForDbxSynch();

			LOGGER.info("START dbx getUpdatesFromHost for DBX03");
			dropboxGateway.getUpdatesFromHost(mCsvOrderImporter,
				mCsvOrderLocationImporter,
				mCsvInventoryImporter,
				mCsvLocationAliasImporter,
				mCsvCrossBatchImporter,
				mAislesFileCsvImporter);
			LOGGER.info("FINISH dbx getUpdatesFromHost for DBX03. The .processing file hould have movedto processed and renamed as normal");

			// We should have one order with 3 details.
			OrderHeader order = OrderHeader.staticGetDao().findByDomainId(facility, "12345");

			Assert.assertNotNull(order);
			Integer detailCount = order.getOrderDetails().size();
			Assert.assertEquals((Integer) 3, detailCount);

			waitForDbxSynch();

			// Add a .FAILED file
			String csvString2 = csvString1
					+ "1,USF314,COSTCO,12345,12345,1622,Raggety Ann Doll,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0" + "\r\n";
			final String csvFilePath2 = fullOrdersDirPath + failedFileFragment;

			try {
				LOGGER.debug("full file path: " + csvFilePath2);
				FileUtils.writeStringToFile(new File(csvFilePath2), csvString2);
				LOGGER.debug("the .FAILED file has a fourth detail for 12345 ");
			} catch (IOException e) {
				LOGGER.error("failed to write file", e);
			}

			LOGGER.info("START dbx getUpdatesFromHost for DBX03 after .FAILED file came");
			// use the normal mCsvOrderImporter as we don't want to inject a new file again
			dropboxGateway.getUpdatesFromHost(mCsvOrderImporter,
				mCsvOrderLocationImporter,
				mCsvInventoryImporter,
				mCsvLocationAliasImporter,
				mCsvCrossBatchImporter,
				mAislesFileCsvImporter);
			LOGGER.info("FINISH getUpdatesFromHost. Nothing should have happened");

			// Still only 3 details)
			order = OrderHeader.staticGetDao().findByDomainId(facility, "12345");

			Assert.assertNotNull(order);
			detailCount = order.getOrderDetails().size();
			Assert.assertEquals((Integer) 3, detailCount);

			// Final cleanup. We have have testorder.csv in processed, and testorder.csv.FAILED in the orders folder.. 
			// Let's first make sure our leftover files are as we expect
			String processedCsvFilePath = fullOrdersDirPath + processedFragment + orderFileFragment;
			if (!FileUtils.deleteQuietly(new File(processedCsvFilePath))) {
				Assert.fail("file was not there to delete");
			}

			String failedCsvFilePath = fullOrdersDirPath + failedFileFragment;
			LOGGER.debug("delete: " + failedCsvFilePath);
			if (!FileUtils.deleteQuietly(new File(failedCsvFilePath))) {
				Assert.fail("file was not there to delete");
			}

			// Just checking that deleteQuietly returns false if the file is not found.
			String bogusCsvFilePath = fullOrdersDirPath + failedFileFragment + "xxx.csv";
			if (FileUtils.deleteQuietly(new File(bogusCsvFilePath))) {
				Assert.fail("deleteQuietly return true for missing file. Other test results are suspect.");
			}

		} finally {
			waitForDbxSynch();

			// Delete the entire dbx03 directory
			if (FileUtils.deleteQuietly(new File(facilityDirPath)))
				LOGGER.debug("deleted entire facility dbx03 folder ");
			else
				LOGGER.error("failed to delete facility dbx03folder ");

		}
	}

}
