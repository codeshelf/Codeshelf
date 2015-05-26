package com.codeshelf.api.pickscript;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import lombok.Getter;

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
		ScriptStep prevStep = null;
		ScriptStep firstStep = null;
		while (!lines.isEmpty()) {
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
		}
		return firstStep;
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
		ScriptStep step = new ScriptStep(stepParts, stepHeader.substring(STEP.length()).trim());
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
	
	public static class ScriptStep {
		@Getter
		private final UUID id = UUID.randomUUID();
		@Getter
		private ScriptStep nextStep = null;
		@Getter
		private ArrayList<StepPart> parts;
		@Getter
		private String comment;
		@Getter
		private ArrayList<String> requiredFiles = Lists.newArrayList();
		private Timestamp created = new Timestamp(System.currentTimeMillis());
		
		public ScriptStep(ArrayList<StepPart> parts, String comment) {
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
					if (command.equalsIgnoreCase("importAisles") || command.equalsIgnoreCase("importLocations") || command.equalsIgnoreCase("importInventory") || command.equalsIgnoreCase("importOrders")) {
						requiredFiles.add(parts[1]);
					}
				}
			}
		}
	}
	
	public static class ScriptApiResponse{
		public UUID nextStepId;
		public List<String> requiredFiles;
		public String report;
		
		public ScriptApiResponse(UUID nextStepId, List<String> requiredFiles, String report) {
			this.nextStepId = nextStepId;
			this.requiredFiles = requiredFiles;
			this.report = report;
		}
	}
}
