package com.codeshelf.edi;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.PrintWriter;
import java.util.HashMap;

public class PFSWebOrderConverter {
	
	public static void main(String[] args) {
		if (args.length!=3) {
			System.out.println("Missing arguments.  Usage: PFSWebOrderConverter <inventory-file> <input-order-file> <output-order-file>");
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
		// process order input file
		String orderInputFile = args[1];
		String orderOutputFile = args[2];
		try {
			System.out.println("Reading orders from "+orderInputFile);
			BufferedReader br = new BufferedReader(new FileReader(orderInputFile));
			
			System.out.println("Writing orders to "+orderOutputFile);
			PrintWriter writer = new PrintWriter(orderOutputFile, "UTF-8");	
			
			// write header
			writer.println("orderId,orderDetailId,itemId,description,quantity,uom,locationId,preAssignedContainerId,preferredSequence");

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
					String itemDescription = descriptionMap.get(itemId);
					if (itemDescription==null) {
						itemDescription="";
					}
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
