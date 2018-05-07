package uk.ac.york.cs.emu.eol.examples.mutations.generator;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Stage1 {

	@SuppressWarnings("unchecked")
	public static void main(String[] args) throws Exception {
		List<String> configurations = new ArrayList<String>();
		BufferedReader read = null;
		String line;
		URL config_file = null;
		try {
			config_file = Stage1.class.getResource("configurations/modules.configurations");
			read = new BufferedReader(new InputStreamReader(config_file.openStream()));
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
		Map<String, String> properties = null;

		for (String module : configurations) {
			System.out.println("Operators Execution: " + module);
			clazz = Class.forName(_package + "." + module);
			method = clazz.getMethod("properties");
			properties = (Map<String, String>) method.invoke(clazz);
			OperatorExecutor runner = new OperatorExecutor(properties);
			runner.run();
		}
	}
}
