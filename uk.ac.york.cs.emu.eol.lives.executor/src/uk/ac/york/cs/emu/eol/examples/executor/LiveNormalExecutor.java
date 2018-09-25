package uk.ac.york.cs.emu.eol.examples.executor;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class LiveNormalExecutor {

    @SuppressWarnings("unchecked")
    public static void main(String[] args) throws Exception {
	List<String> configurations = new ArrayList<String>();
	BufferedReader read = null;
	String line;
	try {
	    read = new BufferedReader(new FileReader(new File("config")));
	    while ((line = read.readLine()) != null) {
		if (!line.startsWith("#") && line.length() > 0) {
		    configurations.add(line);
		}
	    }
	    read.close();
	} catch (Exception e) {
	    e.printStackTrace();
	    return;
	}

	Class<?> clazz;
	Method method;
	String _package = LiveNormalExecutor.class.getPackage().getName() + ".configurations";
	Map<String, Object> config = null;

	for (String eol_module : configurations) {
	    clazz = Class.forName(_package + "." + eol_module);
	    method = clazz.getMethod("properties");
	    config = (Map<String, Object>) method.invoke(clazz);
	    EolLauncher runner = new EolLauncher(config);
	    runner.run();
	}
    }
}
