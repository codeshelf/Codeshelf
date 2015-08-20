package com.codeshelf.perf.importer_stress;

/**
 * WARNING: running this test will load new aisle/locations/inventory/orders data into your database
 * 
 * @author Ilya
 * We found that when DB/Hibernate errors are generated during importers, the transactions were often not rolled back fully.
 * This would cause errors and hanging in all subsequent calls to those importers.  
 * 
 * This test tests our fixes to the issue in the following way:
 * 1a) Induce DB errors by running 2 identical import calls simultaneously (repeat for each file type)
 * 1b) Ensure that none of the above calls hanged. Though, error codes are expected.
 * 2a) Run a single instance of each import process.
 * 2b) Ensure that all importers finished without hangings or errors.
 *
 */
public class StresserTest extends StresserABS{
	public static void main(String[] args) throws Exception{
		//Initial run of importers. Expected to return errors, but not expected to hang.
		testImporter(ImporterType.Aisle, 2, false, genError("First", ImporterType.Aisle));
		testImporter(ImporterType.Location, 2, false, genError("First", ImporterType.Location));
		testImporter(ImporterType.Inventory, 2, false, genError("First", ImporterType.Inventory));
		testImporter(ImporterType.Orders, 2, false, genError("First", ImporterType.Orders));
		//Second run of importers. Will fail if the first run doesn't released DB resources properly
		testImporter(ImporterType.Aisle, 1, true, genError("Second", ImporterType.Aisle));
		testImporter(ImporterType.Location, 1, true, genError("Second", ImporterType.Location));
		testImporter(ImporterType.Inventory, 1, true, genError("Second", ImporterType.Inventory));
		testImporter(ImporterType.Orders, 1, true, genError("Second", ImporterType.Orders));
		
		System.out.println("Test completed");
	}
	
	private static String genError(String numRun, ImporterType type){
		return String.format("%s run of %s importer", numRun, type);
	}
	 
	private static void testImporter(ImporterType type, int numThreads, boolean watchForBadReturnCodes, String testDescription) throws Exception{
		long start = System.currentTimeMillis();
		int errorStatus = run(type, numThreads);
		long end = System.currentTimeMillis();
		long durationSec = (end - start) / 1000;
		if (durationSec > 15) {
			throw new Exception(testDescription + " took too long");
		}
		if (watchForBadReturnCodes && errorStatus != 0){
			throw new Exception(testDescription + " returned bad status " + errorStatus);
		}
	}
}