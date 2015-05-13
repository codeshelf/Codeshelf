package com.codeshelf.api.pickscript;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Timestamp;

import com.codeshelf.edi.ICsvOrderImporter;
import com.codeshelf.model.domain.Facility;
import com.codeshelf.util.CsExceptionUtils;
import com.codeshelf.ws.protocol.message.PickScriptMessage;
import com.sun.jersey.multipart.FormDataMultiPart;

public class PickScriptServerRunner {
	private final static String TEMPLATE_IMPORT_ORDERS = "importOrders <filename>";
	
	private Facility facility;
	private ICsvOrderImporter orderImporter;	
	private StringBuilder report;
	private FormDataMultiPart postBody;
	
	public PickScriptServerRunner(Facility facility, ICsvOrderImporter orderImporter, FormDataMultiPart postBody) {
		this.facility = facility;
		this.orderImporter = orderImporter;
		this.postBody = postBody;
	}
	
	public PickScriptMessage processServerScript(String script){
		report = new StringBuilder();
		report.append("SERVER\n");
		PickScriptMessage message = new PickScriptMessage();
		try {
			String[] lines = script.split("\n");
			for (String line : lines) {
				processLine(line);
			}
		} catch (Exception e) {
			report.append(CsExceptionUtils.exceptionToString(e)).append("\n");
			message.setSuccess(false);
		}
		message.setResponse(report.toString());
		return message;
	}
	
	private void processLine(String line) throws Exception {
		report.append("Run: ").append(line).append("\n");
		if (line == null || line.isEmpty()) {
			return;
		}
		//Clean up multiple spaces between words 
		line = line.trim().replaceAll("\\s+", " ");
		String parts[] = line.split(" ");
		for (int i = 0; i < parts.length; i++) {
			parts[i] = parts[i].trim();
		}
		String command = parts[0];
		if (command.equalsIgnoreCase("importOrders")) {
			processImportOrdersCommand(parts);
		} else if (command.startsWith("//")) {
		} else {
			throw new Exception("Invalid command '" + command + "'. Expected [importOrders, //]");
		}
	}
	
	/**
	 * Expects to see command
	 * importOrders <filename>
	 * @throws Exception 
	 */
	private void processImportOrdersCommand(String parts[]) throws Exception {
		if (parts.length != 2){
			throwIncorrectNumberOfArgumentsException(TEMPLATE_IMPORT_ORDERS);
		}
		String filename = parts[1];
		InputStream is = PickScriptParser.getInputStream(postBody, filename);
		if (is == null) {
			throw new Exception("Unable to find file " + filename);
		}
		InputStreamReader isr = new InputStreamReader(is);
		orderImporter.importOrdersFromCsvStream(isr, facility, new Timestamp(System.currentTimeMillis()));
	}
	
	private void throwIncorrectNumberOfArgumentsException(String expected) throws Exception{
		throw new Exception("Incorrect number of arguments. Expected '" + expected + "'");
	}
}
