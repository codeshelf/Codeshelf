package com.codeshelf.edi;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class PFSWebOrderBatchConverter {
	
	public static void main(String[] args) {
		// check command-line arguments
		if (args.length!=4) {
			System.out.println("Missing arguments.  Usage: PFSWebOrderConverter <inventory-file> <sequence-mapping-file> <input-order-file> <output-order-file>");
			return;
		}
		
		// process item description file and store content in map
		String inventoryFile = args[0];
		HashMap<String,String> descriptionMap = new HashMap<String,String>();
		try {
			System.out.println("Reading inventory file from "+inventoryFile);
			BufferedReader br = new BufferedReader(new FileReader(inventoryFile));
			String line=null;
			while ((line = br.readLine()) != null) {
				String[] columns = line.split(",",2);
				if (columns.length==2) {
					String desc = columns[1];
					//String desc = columns[1].replace(","," ");
					descriptionMap.put(columns[0], desc);
					//System.out.println("Adding "+columns[0]+"="+desc);
				}
				else {
					// skipping malformed line
				}
			}
			br.close();
			System.out.println(descriptionMap.size()+" item descriptions found");
		}
		catch (Exception e) {
			System.out.println("Failed to parse inventory file: "+e);
			return;
		}
		
		// process location to sequence mapping file and store content in map
		String sequenceFile = args[1];
		HashMap<String,String> sequenceMap = new HashMap<String,String>();
		try {
			System.out.println("Reading location file from "+sequenceFile);
			BufferedReader br = new BufferedReader(new FileReader(sequenceFile));
			String line=null;
			while ((line = br.readLine()) != null) {
				String[] columns = line.split(",",4);
				if (columns.length==4) {
					String sequenceNumber = columns[3];
					//String desc = columns[1].replace(","," ");
					sequenceMap.put(columns[0], sequenceNumber);
					//System.out.println("Adding "+columns[0]+"="+desc);
				}
				else {
					// skipping malformed line
				}
			}
			br.close();
			System.out.println(descriptionMap.size()+" locations found");
		}
		catch (Exception e) {
			System.out.println("Failed to parse locations file: "+e);
			return;
		}
		// process order input file
		String orderInputDirectory = args[2];
		String orderOutputDirectory = args[3];
		
		System.out.println("Reading orders files from "+orderInputDirectory);
		
		List<String> fileNames = new ArrayList<String>();

		File[] files = new File(orderInputDirectory).listFiles();

		for (File file : files) {
		    if (file.isFile()) {
		    	fileNames.add(file.getName());
		    }
		}
		System.out.println(fileNames.size()+" files found...");
		
		for (String fileName : fileNames) {
			try {
				String orderInputFile = orderInputDirectory+File.separator+fileName;
				String orderOutputFile = orderOutputDirectory+File.separator+fileName+".csv";
				BufferedReader br = new BufferedReader(new FileReader(orderInputFile));
				
				System.out.println("Writing orders to "+orderOutputFile);
				PrintWriter writer = new PrintWriter(orderOutputFile, "UTF-8");	
				
				// write header
				writer.println("orderId,orderDetailId,itemId,description,quantity,uom,locationId,preAssignedContainerId,workSequence");
	
				// read input order file line by line and write to output file in Codeshelf format
				String line=null;
				int numItems = 0;
				while ((line = br.readLine()) != null) {
					String[] columns = line.split("\\^",9);
					if (columns.length==9) {
						String containerId = columns[1];
						String locationId = columns[3];
						String quantity = columns[4];
						String itemId = columns[5];
						String orderDetailId = columns[6];
						String pickSequence = columns[7];
						// look up item descriptions
						String itemDescription = descriptionMap.get(itemId);
						if (itemDescription==null) {
							itemDescription="";
						}
						// look up sequence number if undefined
						if (pickSequence==null || pickSequence.length()==0 || pickSequence.equals("00000") || pickSequence.equals("99999")) {
							pickSequence = sequenceMap.get(locationId);
							if (pickSequence==null) {
								pickSequence = "0";
							}
						}
						// write record to codeshelf order file
						writer.println(containerId+","+orderDetailId+","+itemId+",\""+itemDescription+"\","+quantity+",each,"+locationId+","+containerId+","+pickSequence);
						System.out.println("Adding item: "+quantity+"x "+itemDescription+"("+itemId+") from "+locationId+" to "+containerId);
						numItems++;
					}
					else {
						System.out.println("Malformed order line: "+line);					
					}
				}
				br.close();
				writer.close();
				System.out.println(numItems+" order items converted");
			}
			catch (Exception e) {
				System.out.println("Failed to parse inventory file: "+e);
				return;
			}
		}
	}

}
