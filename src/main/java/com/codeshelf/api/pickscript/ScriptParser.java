package com.codeshelf.api.pickscript;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.UUID;

import lombok.Getter;
import lombok.Setter;

import com.codeshelf.api.pickscript.ScriptStepParser.StepPart;
import com.google.common.collect.Lists;

public class ScriptParser {
	private static final String STEP = "STEP";
	private static final long EXPIRATION_MIN = 20;

	private static final HashMap<UUID, ScriptStep> scriptStepsGlobal = new HashMap<>();
	
	public static ScriptStep parseScript(String script) throws Exception{
		cleanup();
		ArrayList<String> lines = new ArrayList<String>(Arrays.asList(script.split("\n")));
		if (lines.isEmpty()) {
			throw new Exception("Could not read any lines from the script file");
		}
		
		lines = removeMultiLineComments(lines);
		processVaraibles(lines);
		
		ScriptStep prevStep = null;
		ScriptStep firstStep = null;
		
		ArrayList<ScriptStep> steps = Lists.newArrayList();
		while (!lines.isEmpty()) {
			//Skip comments before the first Step in the script
			if (firstStep == null && lines.get(0).startsWith("//")) {
				lines.remove(0);
				continue;
			}
			ScriptStep scriptStep = getNextScriptStep(lines);
			synchronized (scriptStepsGlobal) {
				scriptStepsGlobal.put(scriptStep.id, scriptStep);
			}
			if (prevStep == null) {
				firstStep = scriptStep;
			} else {
				prevStep.nextStep = scriptStep;
			}
			prevStep = scriptStep;
			steps.add(scriptStep);
		}
		
		//Enumerate steps
		int stepNum = 0;
		for (ScriptStep step : steps) {
			step.setStepIndex(++stepNum, steps.size());
		}
		return firstStep;
	}
	
	/**
	 * This method removes multi-line comments from the script
	 */
	private static ArrayList<String> removeMultiLineComments(ArrayList<String> scriptLines) throws Exception{
		ArrayList<String> cleanScript = Lists.newArrayList();
		boolean commentOngoing = false;
		for (String line : scriptLines) {
			if (line.startsWith("/*")) {
				commentOngoing = true;
			} else if (!line.startsWith("//") && line.endsWith("*/")) {
				if (commentOngoing) {
					commentOngoing = false;
				} else {
					throw new Exception("Encountered close-comment command without a matching open-comment: " + line);
				}
			} else if (!commentOngoing) {
				cleanScript.add(line);
			}
		}
		if(commentOngoing) {
			throw new Exception("Script contains a non-closed multi-line comment. Check your '/*' and '*/' commands");
		}
		return cleanScript;
	}
	
	private static void processVaraibles(ArrayList<String> scriptLines) throws Exception{
		ArrayList<String[]> variables = Lists.newArrayList();
		String line = null;
		//Read variables from the top of the script
		while (!scriptLines.isEmpty()){
			line = scriptLines.get(0);
			String parts[] = ScriptParser.splitLine(line);
			if (line.isEmpty() || line.startsWith("//")){
				scriptLines.remove(0);
			} else if ("var".equalsIgnoreCase(parts[0])){
				if (parts.length != 3) {
					throw new Exception("Incorrect number of arguments in '" + line + "'. Expected 'var <:name> <value>'");
				}
				String varName = parts[1];
				String varValue = parts[2];
				if (!varName.startsWith(":")) {
					throw new Exception("Incorrect variable name '" + varName + "' in '" + line + "'; must start with ':'");
				}
				String variable[] = {":\\b" + varName.substring(1) + "\\b", varValue};
				variables.add(variable);
				scriptLines.remove(0);
			} else {
				break;
			}
		}
		//Replace variables with values in the rest of the script
		int numLines = scriptLines.size();
		for (int i = 0; i < numLines; i++){
			line = scriptLines.get(i);
			for (String[] variable : variables) {
				line = line.replaceAll(variable[0], variable[1]);
			}
			scriptLines.set(i, line);
		}
		return;
	}
	
	private static ScriptStep getNextScriptStep(ArrayList<String> scriptLines) throws Exception{
		String stepHeader = scriptLines.remove(0);
		if (!stepHeader.toUpperCase().startsWith(STEP)) {
			throw new Exception("Script must begin with STEP to denote the beginning of the first step. Instead encountered " + stepHeader);
		}
		ArrayList<String> stepLines = Lists.newArrayList();
		while (!scriptLines.isEmpty()) {
			String nextLine = scriptLines.get(0);
			if (nextLine.toUpperCase().startsWith(STEP)) {
				break;
			}
			stepLines.add(scriptLines.remove(0));
		}
		ArrayList<StepPart> stepParts = ScriptStepParser.parseScriptStep(stepLines);
		String comment = stepHeader.substring(STEP.length()).trim();
		ScriptStep step = new ScriptStep(stepParts, comment);
		return step;
	}
		
	public static ScriptStep getScriptStep(UUID uuid) throws Exception {
		synchronized (scriptStepsGlobal) {
			return scriptStepsGlobal.remove(uuid);
		}
	}
	
	private static void cleanup(){
		long expirationMs = EXPIRATION_MIN * 60 * 1000;
		long now = System.currentTimeMillis();
		synchronized (scriptStepsGlobal) {
			Iterator<ScriptStep> iterator = scriptStepsGlobal.values().iterator(); 
			while (iterator.hasNext()) {
				ScriptStep scriptStep = iterator.next();
				long scriptAge = now - scriptStep.created.getTime();
				if (scriptAge > expirationMs){
			        iterator.remove();
			    }
			}			
		}
	}
	
	public static String[] splitLine(String line){
		//Clean up multiple spaces between words 
		line = line.trim().replaceAll("\\s+", " ");
		String parts[] = line.split(" ");
		for (int i = 0; i < parts.length; i++) {
			parts[i] = parts[i].trim();
		}
		return parts;
	}
	
	public static class ScriptStep {
		@Getter
		private UUID id;
		@Getter
		private String comment;
		@Getter @Setter
		private String report;
		@Getter
		private ArrayList<String> requiredFiles = Lists.newArrayList();
		@Getter
		private ArrayList<String> errors;
		@Getter
		private int stepNum, totalSteps;
		
		private ScriptStep nextStep = null;
		private ArrayList<StepPart> parts;
		private Timestamp created = new Timestamp(System.currentTimeMillis());
		
		public ScriptStep(String report) {
			this.report = report;
		}

		public ScriptStep(ArrayList<StepPart> parts, String comment) {
			id = UUID.randomUUID();
			this.parts = parts;
			this.comment = comment;
			scanForRequiredFiles();
		}
		
		private void scanForRequiredFiles(){
			for (StepPart part : parts) {
				for (String line : part.getScriptLines()) {
					//Remove extra spaces
					String lineClean = line.trim().replaceAll("\\s+", " ");
					String parts[] = lineClean.split(" ");
					String command = parts[0];
					if (command.equalsIgnoreCase("importAisles") || command.equalsIgnoreCase("importLocations") || command.equalsIgnoreCase("importInventory") || command.equalsIgnoreCase("importOrders") || command.equalsIgnoreCase("addExtensionPoint")) {
						if (parts.length > 1){
							requiredFiles.add(parts[1]);
						}
					}
				}
			}
		}
		
		public void setStepIndex(int stepNum, int totalSteps){
			this.stepNum = stepNum;
			this.totalSteps = totalSteps;
		}
		
		public ScriptStep nextStep(){
			return nextStep;
		}
		
		public ArrayList<StepPart> parts(){
			return parts;
		}
		
		public void addError(String error){
			if (error == null || error.isEmpty()) {
				return;
			}
			if (errors == null) {
				errors = Lists.newArrayList();
			}
			errors.add(error);
		}
	}
}