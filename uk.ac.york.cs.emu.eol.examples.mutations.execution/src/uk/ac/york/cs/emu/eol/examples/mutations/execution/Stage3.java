package uk.ac.york.cs.emu.eol.examples.mutations.execution;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Stage3 {

	@SuppressWarnings("unchecked")
	public static void main(String[] args) throws Exception {
		List<String> configurations = new ArrayList<String>();
		BufferedReader read = null;
		String line;
		try {
			read = new BufferedReader(new InputStreamReader(Stage3.class.getResource("configurations/modules.configurations").openStream()));
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
		String _package = Stage3.class.getPackage().getName() + ".configurations";
		Map<String, Object> config = null;

		for (String config_file : configurations) {
			clazz = Class.forName(_package + "." + config_file);
			method = clazz.getMethod("properties");
			config = (Map<String, Object>) method.invoke(clazz);
			System.out.println("Mutations Execution: " + config_file);
			EolLauncher runner = new EolLauncher(config);
			runner.run();
		}
	}
}
