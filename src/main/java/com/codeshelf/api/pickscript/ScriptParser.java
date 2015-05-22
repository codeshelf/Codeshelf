package com.codeshelf.api.pickscript;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.UUID;

import lombok.Getter;

import com.codeshelf.api.pickscript.ScriptStepParser.StepPart;
import com.google.common.collect.Lists;

public class ScriptParser {
	private static final String STEP = "STEP";
	private static final long EXPITATION_MIN = 20;

	private static final HashMap<UUID, ScriptStep> scriptStepsGlobal = new HashMap<>();
	
	public static ScriptApiResponse parseScript(String script) throws Exception{
		ArrayList<String> lines = new ArrayList<String>(Arrays.asList(script.split("\n")));
		ScriptStep prevStep = null;
		UUID firstId = null;
		while (!lines.isEmpty()) {
			ScriptStep scriptStep = getNextScriptStep(lines);
			synchronized (scriptStepsGlobal) {
				scriptStepsGlobal.put(scriptStep.id, scriptStep);
			}
			if (prevStep == null) {
				firstId = scriptStep.id;
			} else {
				prevStep.nextId = scriptStep.id;
			}
			prevStep = scriptStep;
		}
		return new ScriptApiResponse(firstId, "Script imported");
	}
	
	private static ScriptStep getNextScriptStep(ArrayList<String> scriptLines) throws Exception{
		String stepHeader = scriptLines.remove(0);
		if (!stepHeader.toUpperCase().startsWith(STEP)) {
			throw new Exception("getNextScriptStep() called with script not starting with SEGMENT - internal logic error");
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
		ScriptStep step = new ScriptStep(stepParts, stepHeader.substring(STEP.length()));
		return step;
	}
		
	public static ScriptStep getScriptStep(UUID uuid) throws Exception {
		synchronized (scriptStepsGlobal) {
			return scriptStepsGlobal.get(uuid);
		}
	}
	
	public static void cleanup(){
		long expirationMs = EXPITATION_MIN * 60 * 1000;
		long now = System.currentTimeMillis();
		synchronized (scriptStepsGlobal) {
			Iterator<ScriptStep> iterator = scriptStepsGlobal.values().iterator(); 
			while (iterator.hasNext()) {
				ScriptStep scriptStep = iterator.next();
				if (now - scriptStep.created.getTime() > expirationMs){
			        iterator.remove();
			    }
			}			
		}
	}
	
	public static class ScriptStep {
		private final UUID id = UUID.randomUUID();
		@Getter
		private UUID nextId = null;
		@Getter
		private ArrayList<StepPart> parts;
		@Getter
		private String comment;
		private Timestamp created = new Timestamp(System.currentTimeMillis());
		
		public ScriptStep(ArrayList<StepPart> parts, String comment) {
			this.parts = parts;
			this.comment = comment;
		}
	}
	
	public static class ScriptApiResponse{
		public UUID nextStepId, scriptId;
		public String report;
		
		public ScriptApiResponse(UUID nextStepId, String report) {
			this.nextStepId = nextStepId;
			this.report = report;
		}
	}
}
