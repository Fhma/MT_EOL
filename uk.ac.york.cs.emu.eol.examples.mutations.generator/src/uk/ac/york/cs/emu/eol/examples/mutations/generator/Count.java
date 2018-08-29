package uk.ac.york.cs.emu.eol.examples.mutations.generator;

import java.io.File;

import org.eclipse.epsilon.emu.EmuModule;

public class Count {

	public static void main(String[] args) throws Exception {

		File folder = new File("operators/all_operators");

		short count_impl = 0;
		short count_operators = 0;

		for (File f : folder.listFiles()) {
			if (f != null && f.getName().endsWith(".emu")) {
				EmuModule emu = new EmuModule();
				emu.parse(f.getAbsoluteFile());
				if (emu.getParseProblems().size() > 0) throw new Exception(emu.getParseProblems().toString());
				if (emu.getPatterns().size() > 0) count_operators++;
				count_impl = (short) (count_impl + emu.getPatterns().size());
			}
		}
		System.out.println("Number of mutation operators: " + count_operators);
		System.out.println("Number of implementations: " + count_impl);
	}
}
