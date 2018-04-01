package uk.ac.york.cs.emu.eol.examples.mutations.generator;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Stage1 {

	@SuppressWarnings("unchecked")
	public static void main(String[] args) throws Exception {
		List<String> configurations = new ArrayList<String>();
		BufferedReader read = null;
		String line;
		try {
			read = new BufferedReader(new InputStreamReader(Stage1.class.getResource("configurations/modules.configurations").openStream()));
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
		String _package = Stage1.class.getPackage().getName() + ".configurations";
		Map<String, String> config = null;

		ExecutorService executor = null;
		for (String config_file : configurations) {
			System.out.println("Operators Execution: " + config_file);
			clazz = Class.forName(_package + "." + config_file);
			method = clazz.getMethod("properties");
			config = (Map<String, String>) method.invoke(clazz);
			OperatorExecutor runner = new OperatorExecutor(config);
			// executor = Executors.newSingleThreadExecutor();
			// executor.execute(runner);
			runner.run();
		}
		// if (executor != null)
		// executor.shutdown();
		System.out.println("------------------------------------------------");
	}
}
