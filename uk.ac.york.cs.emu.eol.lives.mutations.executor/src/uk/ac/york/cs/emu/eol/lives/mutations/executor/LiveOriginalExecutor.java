package uk.ac.york.cs.emu.eol.lives.mutations.executor;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class LiveOriginalExecutor {

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
	String _package = LiveOriginalExecutor.class.getPackage().getName() + ".configurations";
	Map<String, Object> config = null;

	for (String config_file : configurations) {
	    clazz = Class.forName(_package + "." + config_file);
	    method = clazz.getMethod("properties");
	    config = (Map<String, Object>) method.invoke(clazz);
	    EolLauncher runner = new EolLauncher(config, EolLauncher.ORIGINAL_EXECUTION);
	    runner.run();
	}
    }
}
