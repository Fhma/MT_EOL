package uk.ac.york.cs.emu.eol.examples.execution;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Stage2 {

	@SuppressWarnings("unchecked")
	public static void main(String[] args) throws Exception {
		List<String> configurations = new ArrayList<String>();
		BufferedReader read = null;
		String line;
		try {
			read = new BufferedReader(new InputStreamReader(Stage2.class.getResource("configurations" + File.separatorChar + "modules.configurations").openStream()));
			while ((line = read.readLine()) != null) {
				if (!line.startsWith("#") && line.length() > 0) {
					configurations.add(line);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			return;
		}

		Class<?> clazz;
		Method method;
		String _package = Stage2.class.getPackage().getName() + ".configurations";
		Map<String, Object> config = null;

		for (String eol_module : configurations) {
			clazz = Class.forName(_package + "." + eol_module);
			method = clazz.getMethod("properties");
			config = (Map<String, Object>) method.invoke(clazz);
			System.out.println("Original Transformation Execution: " + eol_module);
			EolLauncher runner = new EolLauncher(config, EolLauncher.ORIGINAL_EXECUTION);
			runner.run();
		}
	}
}
