package com.github.woshikid.utils;

import java.io.BufferedReader;
import java.io.InputStreamReader;

/**
 * 
 * @author kid
 *
 */
public class ShellUtils {

	public static String exec(String... command) throws Exception {
		Process process = Runtime.getRuntime().exec(command);
		BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
		
		String output = reader.readLine();
		if (output == null || output.length() == 0) {
			reader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
			output = reader.readLine();
		}
		
		process.destroy();
		return (output == null) ? "" : output.trim();
	}
	
}
