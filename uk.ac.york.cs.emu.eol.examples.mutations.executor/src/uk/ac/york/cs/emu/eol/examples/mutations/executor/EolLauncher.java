package uk.ac.york.cs.emu.eol.examples.mutations.executor;

import org.apache.log4j.Logger;
import org.apache.log4j.Level;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EcorePackage;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;
import org.eclipse.epsilon.emc.emf.EmfMetaModel;
import org.eclipse.epsilon.eol.EolModule;
import org.eclipse.epsilon.eol.metamodel.EOLElement;
import org.eclipse.epsilon.eol.metamodel.EolPackage;
import org.eclipse.epsilon.eol.visitor.printer.impl.EolPrinter;
import org.eclipse.epsilon.eunit.dt.cmp.emf.v3.EMFModelComparator;

import uk.ac.york.cs.emu.eol.examples.mutations.executor.configurations.Configuration;

public class EolLauncher {

    // Important directories
    public static final String IN_MODELS_DIR = "inModels" + File.separatorChar;
    public static final String EXPECTED_MODELS_DIR = "expectedModels" + File.separatorChar;
    public static final String EXECUTION_DIR = "execution" + File.separatorChar;
    public static final String MUTATIONS_DIR = "mutations" + File.separatorChar;
    public static final String TEMP_SUFFIX = ".tmp";

    // parameters used by the launcher
    private String eol_name = null;
    private short type;
    private int max_exe;
    private String imported_by = null;
    private String[] importing = null;
    private String[] mm_paths = null;
    private EmfMetaModel metamodels[] = null;

    private FileWriter logger = null;

    public EolLauncher(Map<String, Object> config) {
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
    }

    public void run() {
	try {
	    mutationExecutionMode();
	} catch (Exception e) {
	    e.printStackTrace();
	}
    }

    private void mutationExecutionMode() throws Exception {

	File mutations_execution = new File("mutationsExecution" + File.separatorChar + eol_name);
	mutations_execution.mkdirs();
	File log_file = new File(mutations_execution.getPath() + File.separatorChar + eol_name + ".log");
	logger = new FileWriter(log_file);

	logger.write("\nExecuting mutation of EOL transformation " + eol_name);

	Logger.getRootLogger().setLevel(Level.OFF);

	List<OperatorEntry> operators_stats = new ArrayList<OperatorEntry>();

	EolPackage.eINSTANCE.eClass();
	Resource.Factory.Registry.INSTANCE.getExtensionToFactoryMap().put("xmi", new XMIResourceFactoryImpl());

	// locate mutations folder
	File mutants = new File(MUTATIONS_DIR + eol_name);

	EcorePackage.eINSTANCE.eClass();
	Resource.Factory.Registry.INSTANCE.getExtensionToFactoryMap().put("*", new XMIResourceFactoryImpl());

	// register and load metamodels
	registerAndLoadMetamodels();

	File[] mutant_models = mutants.listFiles();
	// Arrays.sort(mutant_models);

	// go through all mutations
	for (File mutant_model : mutant_models) {
	    if (mutant_model.getName().endsWith(".xmi")) {

		logger.write("\n\tExecuting mutant: " + mutant_model.getName());

		OperatorEntry current_operator = getOperatorEntryByMutant(operators_stats, mutant_model.getName());
		if (current_operator == null) {
		    current_operator = new OperatorEntry(mutant_model);
		    operators_stats.add(current_operator);
		}
		current_operator.incrementProcessedMutants();

		boolean valid_mutant = true;

		List<Short> input_tests = getInputFilesNumbers();

		int total_killed = 0;

		ExecutorService executorService = Executors.newSingleThreadExecutor();
		HashMap<Future<String>, EolExecutor> futures = new HashMap<Future<String>, EolExecutor>();

		File temp_dir = new File(EXECUTION_DIR + mutant_model.getName() + TEMP_SUFFIX);
		temp_dir.mkdirs();
		temp_dir.deleteOnExit();
		while (true) {

		    // generate EOL code of this EOL mutant model
		    File mutant_code = getCodeOfModel(mutant_model, temp_dir.getPath() + File.separatorChar + eol_name + ".eol");
		    if (mutant_code == null) {
			valid_mutant = false;
			break;
		    }

		    // copy over dependency modules to temporary execution folder
		    File src = null, dest = null;
		    File mainModule = null;

		    if (importing != null) {
			for (String dep : importing) {
			    src = new File(dep);
			    dest = new File(temp_dir.getPath() + File.separatorChar + src.getName());
			    Files.copy(src.toPath(), dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
			    src = dest = null;
			}
		    }

		    if (imported_by != null) {
			src = new File((String) imported_by);
			dest = new File(temp_dir.getPath() + File.separatorChar + src.getName());
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
		    EMFModelComparator comp = new EMFModelComparator();
		    for (short num : input_tests) {
			EolExecutor exe = new EolExecutor(base, eol_name, type, metamodels, mutant_model.getName(), temp_dir.getPath(), comp, num);
			Future<String> f = executorService.submit(exe);
			futures.put(f, exe);
		    }

		    for (Future<String> future : futures.keySet()) {
			try {
			    String res = future.get(max_exe, TimeUnit.MILLISECONDS);
			    if (res.startsWith("KILLED") || res.startsWith("EXCEPTION") || res.startsWith("ERROR") || res.startsWith("TERMINATED"))
				total_killed++;
			    logger.write("\n\t\t\t" + res);
			} catch (Exception | Error e) {
			    if (e instanceof TimeoutException)
				logger.write(String.format("\n\t\t\tExceed time allowance: %s", futures.get(future).getSignature()));
			    else {
				logger.flush();
				// logger.write("\n\t\t\tAn exception or error was thrown while executing the mutation: " + e.getMessage());
				throw e;
			    }
			    total_killed++;
			    futures.get(future).getExecutionController().dispose();
			    int counter = 5;
			    int attempts = 2;
			    while (!future.isDone()) {
				counter--;
				if (counter == 0) {
				    attempts--;
				    if (attempts == 0)
					break;
				    counter = 5;
				    future.cancel(true);
				    futures.get(future).dispose();
				}
				Thread.sleep(50);
			    }
			} finally {
			    logger.flush();
			}
		    }

		    executorService.shutdownNow();

		    int counter = 5;
		    while (!executorService.isTerminated()) {
			if (counter == 0)
			    throw new RuntimeException("The execution serive of mutant [" + mutant_model.getName() + "] was not terminated.");
			counter--;
			Thread.sleep(100);
			executorService.shutdownNow();
		    }

		    if (valid_mutant) {
			if (total_killed == 0) {
			    // the mutant wasn't killed by any test input
			    getOperatorEntryByMutant(operators_stats, mutant_model.getName()).addNotKilledToAList(mutant_model.getName());
			    logger.write(String.format("\n\t\tMutant [%s] is NOT killed", mutant_model.getName()));
			} else if (total_killed == input_tests.size()) {
			    logger.write(String.format("\n\t\tMutant [%s] is trivial", mutant_model.getName()));
			    getOperatorEntryByMutant(operators_stats, mutant_model.getName()).incrementTrivialMutants();
			} else {
			    logger.write(String.format("\n\t\tMutant [%s] is killed", mutant_model.getName()));
			    getOperatorEntryByMutant(operators_stats, mutant_model.getName()).incrementKilledMutants();
			}
		    }
		    futures.clear();
		    futures = null;
		    // exit execution loop
		    break;
		}

		// clear execution temporary files and folders of this mutation
		cleanup(temp_dir);

		if (!valid_mutant) {
		    logger.write(String.format("\n\t\tMutant [%s] is NOT valid mutation", mutant_model.getName()));
		    current_operator.incrementInvalidMutants();
		}

		logger.write("\n\t/\\_/\\_/\\_/\\_/\\_/\\_/\\_/\\_/\\_/\\_/\\_/\\_/\\_/\\_/\\_/\\_/\\_/\\_/\\_/\\_/\\_/\\_/\\_/\\_/\\_/\\_/\\_/\\_/\\_/\\\n");
		logger.flush();
	    } // end of executing one mutation

	} // end of executing all mutations

	// finally print the execution report
	try {
	    print_summary(operators_stats, mutations_execution.getPath());
	    logger.close();
	} catch (IOException e) {
	    e.printStackTrace();
	}
    }

    private void cleanup(File dir) {
	if (dir != null && dir.isDirectory() && dir.getName().endsWith(TEMP_SUFFIX)) {
	    for (File f : dir.listFiles())
		f.delete();
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
	EcorePackage.eINSTANCE.eClass();
	Resource.Factory.Registry.INSTANCE.getExtensionToFactoryMap().put("*", new XMIResourceFactoryImpl());
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

    private List<Short> getInputFilesNumbers() {
	List<Short> numbers = new ArrayList<Short>();
	File dir = new File(IN_MODELS_DIR + eol_name);
	String s;
	short num;
	if (dir.isDirectory()) {
	    for (File f : dir.listFiles()) {
		if (f.getName().endsWith("xmi")) {
		    // numbers of files reside at the end of file name before extension
		    // example: MMName_input_NUM.xmi
		    // index number 2
		    s = f.getName();
		    // get the number with out the extension
		    num = Short.parseShort(getSegment(s, "_", 1).replaceAll(".xmi", ""));
		    if (!numbers.contains(num))
			numbers.add(num);
		}
	    }
	}
	return numbers;
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
	    logger.write(String.format("\t\tUnable to generate EOL code of model [%s]", model.getName()));
	    return null;
	}
	return mutant_code;
    }

    private void print_summary(List<OperatorEntry> list, String path) throws IOException {
	if (list == null)
	    return;

	Iterator<OperatorEntry> it = list.iterator();
	StringBuilder entry = new StringBuilder();

	entry.append("Mutation Operator,");
	entry.append("Gen.,");
	entry.append("Trivial,");
	entry.append("Killed,");
	entry.append("Not Killed,");
	entry.append("Invalid");
	entry.append("\n");

	int sum_processed = 0, sum_trivial = 0, sum_killed = 0, sum_not_killed = 0, sum_invalid = 0;

	String not_killed_list = "";

	while (it.hasNext()) {

	    OperatorEntry operator = it.next();
	    entry.append(operator.getOID());
	    entry.append(',');

	    entry.append(operator.getTotalProcessedMutants());
	    entry.append(',');
	    sum_processed += operator.getTotalProcessedMutants();

	    entry.append(operator.getTotalTrivialMutants());
	    sum_trivial += operator.getTotalTrivialMutants();
	    entry.append(',');

	    entry.append(operator.getTotalKilledMutants());
	    sum_killed += operator.getTotalKilledMutants();
	    entry.append(',');

	    entry.append(operator.getAllNotKilled().size());
	    sum_not_killed += operator.getAllNotKilled().size();
	    for (String nk : operator.getAllNotKilled())
		not_killed_list += "\n\t\t\\-->" + nk;
	    entry.append(',');

	    entry.append(operator.getTotalInvalidMutants());
	    sum_invalid += operator.getTotalInvalidMutants();
	    entry.append('\n');
	}
	entry.append("Total Killed,,");
	entry.append(sum_trivial + ",");
	entry.append(sum_killed + ",");
	entry.append('\n');

	entry.append("Total,");
	entry.append(sum_processed + ",");
	entry.append(sum_trivial + sum_killed + ",,");
	entry.append(sum_not_killed + ",");
	entry.append(sum_invalid);

	try (FileWriter log = new FileWriter(path + File.separatorChar + eol_name + ".txt")) {

	    log.write("\t\\--> Processed mutants-> " + sum_processed + "\n");
	    log.write("\t\\--> Invalid mutants-> " + sum_invalid + "\n");
	    log.write("\t\\--> Killed mutants-> " + (sum_trivial + sum_killed) + "\n");
	    log.write("\t\\--> Not killed mutants-> " + sum_not_killed);
	    log.write(not_killed_list + "\n");
	    log.flush();
	    log.close();
	}

	try (FileWriter file = new FileWriter(path + File.separatorChar + eol_name + ".csv")) {
	    file.write(entry.toString());
	    file.flush();
	    file.close();
	}
    }
}