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
		
		try (BufferedReader input = new BufferedReader(new InputStreamReader(process.getInputStream()));
			BufferedReader error = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
			
			String output = input.readLine();
			if (output == null || output.length() == 0) {
				output = error.readLine();
			}
			
			return (output == null) ? "" : output.trim();
		} finally {
			process.destroy();
		}
	}
}
