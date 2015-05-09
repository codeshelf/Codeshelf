package com.codeshelf.api;

import java.io.InputStream;
import java.io.PrintStream;
import java.net.ConnectException;

import org.apache.commons.net.telnet.TelnetClient;

public class GroovyTelnetRunner {
	private static final boolean PRINT_RAW_OUTPUT = false;
	private static final String prompt = "groovy:000> ";
	private TelnetClient telnet = new TelnetClient();
	private InputStream in;
	private PrintStream out;
	private StringBuilder report = new StringBuilder();
		
	public String executeScript(String host, int port, String script) throws Exception {
		try {
			telnet.connect(host, port);
			in = telnet.getInputStream();
			out = new PrintStream(telnet.getOutputStream());
			
			readUntil(prompt);			
			String commnds[] = script.split("\n");
			for (String command : commnds) {
				sendCommand(command);
			}
		} catch (ConnectException e) {
			throw new ConnectException("Telnet conenction to " + host + ":" + port + " refused");
		} finally {
			disconnect();
		}
		return report.toString();
	}

	public String readUntil(String pattern) {
		try {
			char lastChar = pattern.charAt(pattern.length() - 1);
			StringBuffer sb = new StringBuffer();
			while (true) {
				char ch = (char) in.read();
				if (PRINT_RAW_OUTPUT) {
					System.out.print(ch);
				}
				sb.append(ch);
				if (ch == lastChar) {
					String plainText = removeCodes(sb.toString());
					if (plainText.endsWith(pattern)) {
						if (!PRINT_RAW_OUTPUT) {
							System.out.print(plainText);
						}
						report.append(plainText);
						return plainText;
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	public String sendCommand(String command) {
		try {
			out.println(command);
			out.flush();
			readUntil(prompt);
			return readUntil(prompt);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	public void disconnect() {
		try {
			telnet.disconnect();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public String removeCodes(String str) {
		return str.replaceAll("\\e\\[[0-9;]*m", "");
	}
}
