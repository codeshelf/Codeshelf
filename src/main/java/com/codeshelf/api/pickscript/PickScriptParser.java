package com.codeshelf.api.pickscript;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;

import lombok.Getter;

import com.google.common.collect.Lists;
import com.sun.jersey.multipart.FormDataBodyPart;
import com.sun.jersey.multipart.FormDataMultiPart;

public class PickScriptParser {
	private static final String SERVER = "SERVER", SITE = "SITE";

	public static ArrayList<PickScriptPart> parseMixedScript(String script) throws Exception{
		ArrayList<String> lines = new ArrayList<String>(Arrays.asList(script.split("\n")));
		verifyThatScriptStartsEitherWithServerOrSite(lines);
		ArrayList<PickScriptPart> scriptParts = Lists.newArrayList();
		while (!lines.isEmpty()) {
			scriptParts.add(getNextScriptPart(lines));
		}
		return scriptParts;
	}
	
	private static void verifyThatScriptStartsEitherWithServerOrSite(ArrayList<String> lines) throws Exception {
		int i;
		for (i = 0; i < lines.size(); i++) {
			String line = lines.get(i).trim();
			if (line.equalsIgnoreCase(SERVER) || line.equalsIgnoreCase(SITE)) {
				break;
			}
			if (!(line.isEmpty() || line.startsWith("//"))) {
				throw new Exception("First non-comment or empty line of the script must be either 'SERVER' or 'SITE'");
			}
		}
		//Remove all comments before first SERVER or SITE line
		while (i-- > 0) {
			lines.remove(0);
		}
	}
	
	private static PickScriptPart getNextScriptPart(ArrayList<String> lines) throws Exception{
		StringBuilder builder = new StringBuilder();
		boolean isServer = false, lookingForFirstLine = true;
		while (!lines.isEmpty()) {
			String line = lines.get(0).trim();
			//line = lines.remove(0);
			if (lookingForFirstLine) {
				if (!(line.equalsIgnoreCase(SERVER) || line.equalsIgnoreCase(SITE))) {
					throw new Exception("getNextScriptPart() called with script not starting with SERVER/SITE - internal logic error");
				}
				isServer = line.equalsIgnoreCase(SERVER);
				lookingForFirstLine = false;
			} else {
				if (line.equalsIgnoreCase(SERVER) || line.equalsIgnoreCase(SITE)) {
					//Reached the next script part
					break;
				}
				builder.append(line).append("\n");
			}
			lines.remove(0);
		}
		PickScriptPart part = new PickScriptPart(isServer, builder.toString()); 
		return part;
	}
	
	public static class PickScriptPart{
		@Getter
		private boolean isServer;
		@Getter
		private String script;
		
		public PickScriptPart(boolean isServer, String script) {
			this.isServer = isServer;
			this.script = script;
		}
		
		@Override
		public String toString() {
			return script;
		}
	}
	
	public static InputStream getInputStream(FormDataMultiPart body, String fieldName) throws IOException {
		FormDataBodyPart part = body.getField(fieldName);
		if (part == null) {
			return null;
		}
		InputStream is = part.getEntityAs(InputStream.class);
		return is;
	}
}
