package uk.ac.york.cs.emu.eol.examples.mutations.generator;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Stage1 {

    @SuppressWarnings("unchecked")
    public static void main(String[] args) throws Exception {

	if (args.length != 1)
	    throw new IllegalArgumentException("Mutation operators directory must be given as arguments. Either 'all_operators' or 'selected'");
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
	ExecutorService executorService = Executors.newSingleThreadExecutor();

	Class<?> clazz;
	Method method;
	String _package = Stage1.class.getPackage().getName() + ".configurations";
	Map<String, String> properties = null;

	for (String module : configurations) {
	    clazz = Class.forName(_package + "." + module);
	    method = clazz.getMethod("properties");
	    properties = (Map<String, String>) method.invoke(clazz);
	    OperatorExecutor runner = new OperatorExecutor(properties, args[0]);
	    // runner.run();
	    executorService.execute(runner);
	}
	executorService.shutdown();
    }
}
