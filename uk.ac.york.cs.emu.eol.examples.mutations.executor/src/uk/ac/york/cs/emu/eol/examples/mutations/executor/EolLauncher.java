package uk.ac.york.cs.emu.eol.examples.mutations.executor;

import org.apache.log4j.Logger;
import org.apache.log4j.Level;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;
import org.eclipse.epsilon.emc.emf.EmfMetaModel;
import org.eclipse.epsilon.eol.EolModule;
import org.eclipse.epsilon.eol.metamodel.EOLElement;
import org.eclipse.epsilon.eol.metamodel.EolPackage;
import org.eclipse.epsilon.eol.visitor.printer.impl.EolPrinter;
import uk.ac.york.cs.emu.eol.examples.mutations.executor.configurations.Configuration;

public class EolLauncher {

    // Important directories
    public static final String IN_MODELS_DIR = "inModels" + File.separatorChar;
    public static final String EXPECTED_MODELS_DIR = "expectedModels" + File.separatorChar;
    public static final String MUTATIONS_DIR = "mutations" + File.separatorChar;
    public static final String TEMP_SUFFIX = ".tmp";
    public static final String EXECUTION_DIR = "execution" + TEMP_SUFFIX + File.separatorChar;
    public static final String OUTPUTS_DIR = "outputs" + File.separatorChar;
    public static final String NOTKILLED_DIR = "not_killed" + File.separatorChar;
    private static final short SLEEPING_FACTOR = 100;
    private static final short EXECUTOR_SERVICE_SLEEPING_REPEAT = 50;

    // parameters used by the launcher
    private String eol_name = null;
    private short type;
    private int max_exe;
    private String imported_by = null;
    private String[] importing = null;
    private String[] mm_paths = null;
    private EmfMetaModel metamodels[] = null;
    private PrintStream logger = null;
    private File outputs_dir = null;
    private File not_killed_dir = null;

    public EolLauncher(Map<String, Object> config) throws Exception {
	eol_name = (String) config.get(Configuration.EOL_NAME);
	type = (short) config.get(Configuration.PROGRAM_TYPE);
	max_exe = (int) config.get(Configuration.MAX_EXE);

	if (config.get(Configuration.IMPORTED_BY) != null)
	    imported_by = (String) config.get(Configuration.IMPORTED_BY);

	if (config.get(Configuration.IMPORTING) != null)
	    importing = ((String) config.get(Configuration.IMPORTING)).split(",");

	if (config.get(Configuration.MM_METAMODELS) != null)
	    metamodels = (EmfMetaModel[]) config.get(Configuration.MM_METAMODELS);

	if (config.get(Configuration.MM_PATHS) != null)
	    mm_paths = ((String) config.get(Configuration.MM_PATHS)).split(",");

	outputs_dir = new File(OUTPUTS_DIR + eol_name);
	outputs_dir.mkdirs();
	not_killed_dir = new File(outputs_dir.getPath() + File.separatorChar + NOTKILLED_DIR);
	not_killed_dir.mkdirs();

	logger = new PrintStream(new File(outputs_dir.getPath() + File.separatorChar + eol_name + ".log"));
    }

    public void run() {
	long mins = System.currentTimeMillis();
	try {
	    logger.println("Executing mutations: " + eol_name);
	    mutationExecution();
	} catch (Exception e) {
	    logger.println("\tException: " + e.getMessage());
	} finally {
	    int time = (int) (((System.currentTimeMillis() - mins) / 1000) / 60);
	    logger.println(String.format("End Execution....(%d mins)", time));
	    logger.close();
	}
    }

    private void mutationExecution() throws Exception {

	Logger.getRootLogger().setLevel(Level.OFF);

	List<OperatorEntry> operators_stats = new ArrayList<OperatorEntry>();

	EolPackage.eINSTANCE.eClass();
	Resource.Factory.Registry.INSTANCE.getExtensionToFactoryMap().put("*", new XMIResourceFactoryImpl());

	// locate mutations folder
	File mutants = new File(MUTATIONS_DIR + eol_name);

	// register and load metamodels
	registerAndLoadMetamodels();

	Map<Short, List<File>> input_tests = new HashMap<Short, List<File>>();
	initalizeInputsMap(input_tests);

	File execution_dir = new File(EXECUTION_DIR);
	execution_dir.mkdirs();
	execution_dir.deleteOnExit();

	// go through all mutations
	File[] mutant_models = mutants.listFiles();

	Arrays.sort(mutant_models);

	for (File mutant_model : mutant_models) {
	    if (mutant_model.getName().endsWith(".xmi")) {

		logger.println("\tExecuting mutant: " + mutant_model.getName());

		OperatorEntry current_operator = getOperatorEntryByMutant(operators_stats, mutant_model.getName());
		if (current_operator == null) {
		    current_operator = new OperatorEntry(mutant_model);
		    operators_stats.add(current_operator);
		}
		current_operator.incrementProcessedMutants();

		boolean valid_mutant = true;

		File exe_temp_dir = new File(execution_dir.getPath() + File.separatorChar + mutant_model.getName() + TEMP_SUFFIX);
		exe_temp_dir.mkdir();
		exe_temp_dir.deleteOnExit();

		while (true) {

		    // generate EOL code of this EOL mutant model
		    File mutant_code = getCodeOfModel(mutant_model, exe_temp_dir.getPath() + File.separatorChar + eol_name + ".eol");
		    if (mutant_code == null) {
			valid_mutant = false;
			break;
		    }

		    // copy over dependency modules to temporary execution folder
		    if (importing != null) {
			for (String dep : importing) {
			    File src = new File(dep);
			    File dest = new File(exe_temp_dir.getPath() + File.separatorChar + src.getName());
			    Files.copy(src.toPath(), dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
			    src = dest = null;
			}
		    }

		    File mainModule = null;

		    if (imported_by != null) {
			File src = new File((String) imported_by);
			File dest = new File(exe_temp_dir.getPath() + File.separatorChar + src.getName());
			Files.copy(src.toPath(), dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
			mainModule = dest;
		    } else
			mainModule = mutant_code;

		    final EolModule base = new EolModule();
		    base.parse(mainModule.getAbsoluteFile());
		    if (base.getParseProblems().size() > 0) {
			valid_mutant = false;
			break;
		    }

		    ExecutorService executorService = Executors.newSingleThreadExecutor();
		    HashMap<Future<String>, EolExecutor> futures = new HashMap<Future<String>, EolExecutor>();

		    for (short num : input_tests.keySet()) {
			EolExecutor exe = new EolExecutor(base, eol_name, type, metamodels, mutant_model.getName(), exe_temp_dir.getPath(), num, input_tests.get(num));
			Future<String> f = executorService.submit(exe);
			Thread.sleep(max_exe / SLEEPING_FACTOR);
			futures.put(f, exe);
		    }

		    int total_killed = 0;

		    Set<Future<String>> fs = futures.keySet();
		    logger.printf("\t");
		    for (Future<String> future : fs) {
			try {
			    String res = future.get(max_exe, TimeUnit.MILLISECONDS);
			    if (res.startsWith("K") || res.startsWith("E")) // killed
				total_killed++;
			    logger.printf("[%s], ", res);
			} catch (Exception | Error e) {
			    if (e instanceof TimeoutException)
				logger.printf("[Exceed time allowance]");
			    else {
				executorService.shutdownNow();
				cleanup(execution_dir);
				throw e;
			    }
			    total_killed++;
			    // terminate execution of this future
			    futures.get(future).getExecutionController().dispose();
			}
		    }

		    executorService.shutdownNow();

		    int counter = EXECUTOR_SERVICE_SLEEPING_REPEAT;
		    while (!executorService.isTerminated()) {
			if (counter == 0) {
			    cleanup(execution_dir);
			    throw new RuntimeException("The execution service of mutant [" + mutant_model.getName() + "] was not terminated.");
			}
			counter--;
			Thread.sleep(SLEEPING_FACTOR / 2);
		    }

		    if (total_killed == input_tests.size()) {
			// killed by all test cases-> trivial
			logger.println(String.format("\t\tMutant [%s] is trivial", mutant_model.getName()));
			getOperatorEntryByMutant(operators_stats, mutant_model.getName()).incrementTrivialMutants();
		    } else if (total_killed == 0) {
			// the mutant wasn't killed by any test input
			// copy mutant to live mutants folder
			Files.copy(mutant_code.toPath(), new File(not_killed_dir.getPath() + File.separatorChar + mutant_model.getName() + ".eol").toPath(), StandardCopyOption.REPLACE_EXISTING);
			getOperatorEntryByMutant(operators_stats, mutant_model.getName()).addNotKilledToAList(mutant_model.getName());
			logger.println(String.format("\t\tMutant [%s] is NOT killed", mutant_model.getName()));
		    } else {
			logger.println(String.format("\t\tMutant [%s] is killed", mutant_model.getName()));
			getOperatorEntryByMutant(operators_stats, mutant_model.getName()).incrementKilledMutants();
		    }

		    futures = null;

		    // exit execution loop
		    break;
		}

		if (!valid_mutant) {
		    logger.println(String.format("\t\tMutant [%s] is NOT valid", mutant_model.getName()));
		    current_operator.incrementInvalidMutants();
		}

		logger.print("\n");
		logger.flush();
	    } // end of executing one mutation

	} // end of executing all mutations

	// clear execution temporary files and folders
	cleanup(execution_dir);

	// finally print the execution report
	print_summary(operators_stats);
	logger.flush();
    }

    private void initalizeInputsMap(Map<Short, List<File>> input_tests) {
	File dir = new File(IN_MODELS_DIR + eol_name);
	if (dir.isDirectory()) {
	    for (File f : dir.listFiles()) {
		if (f.getName().endsWith(".xmi")) {
		    short n = Short.parseShort(getSegment(f.getName(), "_", 1).replaceAll(".xmi", ""));
		    if (input_tests.get(n) == null) {
			input_tests.put(n, new ArrayList<File>());
		    }
		    input_tests.get(n).add(f);
		}
	    }
	}
    }

    private void cleanup(File dir) {
	if (dir != null && dir.isDirectory() && dir.getName().endsWith(TEMP_SUFFIX)) {
	    for (File f : dir.listFiles()) {
		if (f.isDirectory()) {
		    cleanup(f);
		}
		f.delete();
	    }
	}
    }

    private Resource getResource(ResourceSet rs, File mutant) {
	return rs.getResource(org.eclipse.emf.common.util.URI.createFileURI(mutant.getPath()), true);
    }

    private OperatorEntry getOperatorEntryByMutant(List<OperatorEntry> list, String mutant) {
	String oid = mutant.substring(0, mutant.lastIndexOf("_"));
	for (int j = 0; j < list.size(); j++)
	    if (list.get(j).getOID().equals(oid))
		return list.get(j);
	return null;
    }

    private void registerAndLoadMetamodels() throws Exception {
	ResourceSet rs = new ResourceSetImpl();
	Resource r = null;
	org.eclipse.emf.common.util.URI uri = null;
	if (mm_paths != null) {
	    List<EObject> ps = new ArrayList<EObject>();
	    for (int i = 0; i < mm_paths.length; i++) {
		uri = org.eclipse.emf.common.util.URI.createURI(new File(mm_paths[i]).getAbsolutePath());
		r = rs.createResource(uri);
		r.load(null);
		getAllEPackages(r.getContents().get(0), ps);
	    }
	    if (ps.size() == 0)
		throw new Exception("Unable to find EPackage in resource " + r.getURI());
	    for (Object o : ps) {
		if (!(o instanceof EPackage))
		    throw new Exception("Invalid EPackage object" + o);
		EPackage p = (EPackage) o;
		EPackage.Registry.INSTANCE.put(p.getNsURI(), p);
	    }
	}
    }

    private void getAllEPackages(EObject o, List<EObject> ps) {
	boolean contain = false;
	if (o instanceof EPackage) {
	    for (EObject eo : o.eContents()) {
		if (eo instanceof EPackage) {
		    contain = true;
		    getAllEPackages(eo, ps);
		}
	    }
	    if (!contain && !ps.contains(o))
		ps.add(o);
	}
    }

    private String getSegment(String s, String delimiter, int index) {
	String[] segments = s.split(delimiter);
	return segments[index];
    }

    private File getCodeOfModel(File model, String target) throws IOException {

	// parse the EOL mutant model into EOL code
	ResourceSet rs = new ResourceSetImpl();
	Resource mutant_resource = null;
	File mutant_code = null;
	try {
	    mutant_resource = getResource(rs, model);
	    mutant_resource.load(Collections.EMPTY_MAP);
	    EObject root = mutant_resource.getContents().get(0);
	    EolPrinter printer = new EolPrinter();
	    String code = printer.print((EOLElement) root);

	    // save the code into a the given target path
	    mutant_code = new File(target);
	    FileWriter fw = new FileWriter(mutant_code);
	    fw.write(code);
	    fw.close();
	} catch (Exception e) {
	    return null;
	}
	return mutant_code;
    }

    private void print_summary(List<OperatorEntry> list) throws IOException {
	if (list == null)
	    return;
	Iterator<OperatorEntry> it = list.iterator();

	FileWriter file = new FileWriter(outputs_dir.getPath() + File.separatorChar + eol_name + ".csv");

	file.write("Mutation Operator,");
	file.write("Gen.,");
	file.write("Trivial,");
	file.write("Killed,");
	file.write("Not Killed,");
	file.write("Invalid\n");

	int sum_processed = 0, sum_trivial = 0, sum_killed = 0, sum_not_killed = 0, sum_invalid = 0;

	String not_killed_list = "";

	while (it.hasNext()) {

	    OperatorEntry operator = it.next();
	    file.write(operator.getOID() + ",");
	    file.write(operator.getTotalProcessedMutants() + ",");
	    sum_processed += operator.getTotalProcessedMutants();

	    file.write(operator.getTotalTrivialMutants() + ",");
	    sum_trivial += operator.getTotalTrivialMutants();

	    file.write(operator.getTotalKilledMutants() + ",");
	    sum_killed += operator.getTotalKilledMutants();

	    file.write(operator.getAllNotKilled().size() + ",");
	    sum_not_killed += operator.getAllNotKilled().size();
	    for (String nk : operator.getAllNotKilled())
		not_killed_list += nk + "\n";

	    file.write(operator.getTotalInvalidMutants() + "\n");
	    sum_invalid += operator.getTotalInvalidMutants();
	}

	file.write("Total Killed,,");
	file.write(sum_trivial + ",");
	file.write(sum_killed + "\n");

	file.write("Total,");
	file.write(sum_processed + ",");
	file.write(sum_trivial + sum_killed + ",,");
	file.write(sum_not_killed + ",");
	file.write(sum_invalid + "\n");
	file.close();

	try (FileWriter log = new FileWriter(outputs_dir.getPath() + File.separatorChar + eol_name + "_lives.txt")) {
	    log.write(not_killed_list);
	    log.close();
	}
    }
}