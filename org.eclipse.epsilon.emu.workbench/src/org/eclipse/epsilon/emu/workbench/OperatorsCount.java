/**
 * 
 * @author Faisal Alhwikem
 * @version 1.0
 * @since March, 2017
 */
package org.eclipse.epsilon.emu.workbench;

import java.io.File;
import org.eclipse.epsilon.emu.EmuModule;

public class OperatorsCount {
	public static void main(String[] args) {
		new OperatorsCount().run();
	}

	public void run() {

		File folder = new File("EMU_script/operatorDefinitions");

		

		int count = 0;

		try {
			for (File sourceFile : folder.listFiles()) {
				if (sourceFile.getName().endsWith(".emu")) {
					EmuModule module = new EmuModule();
					module.parse(sourceFile);
					if (module.getParseProblems().size() >= 1) {
						System.out.println("Parsing Problems: " + module.getParseProblems().toString());
						return;
					}
					count += module.getPatterns().size();
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		System.out.println("Number of patterns: " + count);
	}
}
