package com.codeshelf.device;

import java.lang.reflect.Method;
import java.util.HashMap;

import org.apache.commons.lang.exception.ExceptionUtils;

import com.codeshelf.sim.worker.PickSimulator;
import com.codeshelf.ws.protocol.message.PickScriptMessage;

public class PickScriptRunner {
	private final static String TEMPLATE_DEF_PICKER = "defPicker <pickerName> <cheGuid>";
	private final static String TEMPLATE_PICKER_EXEC = "pickerExec <pickerName> <pickerCommand> [arguments]";
	
	private HashMap<String, PickSimulator> pickers = new HashMap<>();
	private StringBuilder report = new StringBuilder();
	private CsDeviceManager deviceManager;
	
	
	public void runScript(CsDeviceManager deviceManager, PickScriptMessage message) {
		this.deviceManager = deviceManager;
		String script = message.getScript();
		if (script == null) {
			message.setResponseMessage("Empty script");
			deviceManager.clientEndpoint.sendMessage(message);
			return;
		}
		try {
			String[] lines = script.split("\n");
			for (String line : lines) {
				processLine(line);
			}
		} catch (Exception e) {
			report.append(e.getClass().getName()).append("\n");
			String error = e.getMessage();
			if (error == null || error.isEmpty()) {
				error = ExceptionUtils.getStackTrace(e);
			}
			report.append(error);
			message.setResponseMessage(report.toString());
			deviceManager.clientEndpoint.sendMessage(message);
			return;
		}
		report.append("Script Finished\n");
		message.setResponseMessage(report.toString());
		deviceManager.clientEndpoint.sendMessage(message);
	}
	
	private void processLine(String line) throws Exception {
		report.append(line).append("\n");
		if (line == null || line.isEmpty()) {
			return;
		}
		String parts[] = line.split(" ");
		for (int i = 0; i < parts.length; i++) {
			parts[i] = parts[i].trim();
		}
		String command = parts[0];
		if (command.equalsIgnoreCase("pickerExec")) {
			processPickerExecCommand(parts);
		} else if (command.equalsIgnoreCase("defPicker")) {
			processDefinePickerCommand(parts);
		} else {
			throw new Exception("Invalid command. " + command + " Expected [pickerExec, defPicker]");
		}
	}
	
	/**
	 * Expects to see command
	 * pickerExec <pickerName> <pickerCommand> [arguments]
	 * @throws Exception 
	 */
	private void processPickerExecCommand(String parts[]) throws Exception {
		if (parts.length < 3) {
			throwIncorrectNumberOfArgumentsException(TEMPLATE_PICKER_EXEC);
		}
		String pickerName = parts[1];
		PickSimulator picker = pickers.get(pickerName);
		if (picker == null) {
			throw new Exception(String.format("Undefined picker '%s'. Execute '%s' first", pickerName, TEMPLATE_DEF_PICKER));
		}
		String pickerCommand = parts[2];
		if (parts.length == 3){
			Method method = PickSimulator.class.getDeclaredMethod(pickerCommand);
			method.invoke(picker);
		} else if (parts.length == 4){
			Method method = PickSimulator.class.getDeclaredMethod(pickerCommand, String.class);
			method.invoke(picker, parts[3]);
		} else if (parts.length == 5){
			Method method = PickSimulator.class.getDeclaredMethod(pickerCommand, String.class, String.class);
			method.invoke(picker, parts[3], parts[4]);
		} else {
			throw new Exception("Ability to call PickSimulator methods wih more than 2 arguments not yet implemented.");
		}
	}
	
	/**
	 * Expects to see command
	 * defPicker <pickerName> <cheGuid>
	 * @throws Exception 
	 */
	private void processDefinePickerCommand(String parts[]) throws Exception {
		if (parts.length != 3){
			throwIncorrectNumberOfArgumentsException(TEMPLATE_DEF_PICKER);
		}
		String pickerName = parts[1];
		String cheGuid = parts[2];
		PickSimulator picker = new PickSimulator(deviceManager, cheGuid);
		pickers.put(pickerName, picker);
	}
	
	private void throwIncorrectNumberOfArgumentsException(String expected) throws Exception{
		throw new Exception("Incorrect number of arguments. Expected '" + expected + "'");
	}
}
