package uk.ac.york.cs.emu.eol.lives.mutations.executor;

import org.apache.log4j.Logger;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.Level;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
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
import org.eclipse.epsilon.common.util.StringProperties;
import org.eclipse.epsilon.emc.emf.EmfMetaModel;
import org.eclipse.epsilon.emc.emf.EmfModel;
import org.eclipse.epsilon.eol.EolModule;
import org.eclipse.epsilon.eol.exceptions.models.EolModelLoadingException;
import org.eclipse.epsilon.eol.execute.control.DefaultExecutionController;
import org.eclipse.epsilon.eol.metamodel.EolPackage;
import org.eclipse.epsilon.eol.models.IModel;
import org.eclipse.epsilon.eol.models.IRelativePathResolver;

import uk.ac.york.cs.emu.eol.lives.mutations.executor.candidates.EOLCandidate;
import uk.ac.york.cs.emu.eol.lives.mutations.executor.configurations.Configuration;

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
    public static final short ORIGINAL_EXECUTION = 1;
    public static final short MUTATION_EXECUTION = 2;

    // parameters used by the launcher
    private String eol_name = null;
    private short type;
    private int max_exe;
    private String imported_by = null;
    private String[] importing = null;
    private String[] mm_paths = null;
    private EmfMetaModel metamodels[] = null;
    private File not_killed_dir = null;
    private File outputs_dir = null;
    private List<String> mutant_names = null;
    private short execution_mode;
    private String eol_original_code = null;

    public EolLauncher(Map<String, Object> config, short mode) throws Exception {
	execution_mode = mode;
	eol_original_code = (String) config.get(Configuration.EOL_CODE);
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

	File report = new File(MUTATIONS_DIR + eol_name + File.separatorChar + eol_name + "_lives.txt");

	BufferedReader read = new BufferedReader(new FileReader(report));
	String line;
	mutant_names = new ArrayList<String>();
	while ((line = read.readLine()) != null) {
	    mutant_names.add(line);
	}
	read.close();

	not_killed_dir = new File(MUTATIONS_DIR + eol_name + File.separatorChar + NOTKILLED_DIR);
	not_killed_dir.mkdirs();
	outputs_dir = new File(not_killed_dir.getPath() + File.separatorChar + "live_outputs");
	outputs_dir.mkdirs();
    }

    public void run() {
	try {
	    EolPackage.eINSTANCE.eClass();
	    Resource.Factory.Registry.INSTANCE.getExtensionToFactoryMap().put("*", new XMIResourceFactoryImpl());
	    switch (execution_mode) {
	    case ORIGINAL_EXECUTION:
		originalExecution();
		break;
	    case MUTATION_EXECUTION:
		originalExecution();
		mutationExecution();
		break;
	    }
	} catch (Exception e) {
	    e.printStackTrace();
	}
    }

    private void mutationExecution() throws Exception {

	Logger.getRootLogger().setLevel(Level.OFF);

	List<OperatorEntry> operators_stats = new ArrayList<OperatorEntry>();

	EolPackage.eINSTANCE.eClass();
	Resource.Factory.Registry.INSTANCE.getExtensionToFactoryMap().put("*", new XMIResourceFactoryImpl());

	// locate mutations folder
	File mutants = new File(not_killed_dir.getPath());

	// register and load metamodels
	registerAndLoadMetamodels();

	Map<Short, List<File>> input_tests = new HashMap<Short, List<File>>();
	initalizeInputsMap(input_tests);

	File execution_dir = new File(EXECUTION_DIR);
	execution_dir.mkdirs();
	execution_dir.deleteOnExit();

	// go through all mutations
	File[] mutants_code = mutants.listFiles();

	Arrays.sort(mutants_code);
	if (mutant_names.size() == 0)
	    return;
	int index = 0;
	String entry = mutant_names.get(index);
	while (entry != null) {
	    String status = null, name = null;

	    if (entry.contains("<>")) {
		status = entry.substring(0, entry.indexOf("<>"));
		name = entry.substring(entry.indexOf("<>") + 2, entry.length() - 4);
	    } else
		name = entry.substring(0, entry.length() - 4);
	    OperatorEntry current_operator = getOperatorEntryByMutant(operators_stats, name);
	    if (current_operator == null) {
		current_operator = new OperatorEntry(name);
		operators_stats.add(current_operator);
	    }
	    current_operator.incrementProcessedMutants();

	    File mutant = getCorrespondingMutantFileByName(mutants_code, name);

	    if (status != null) {
		if (status.startsWith("L"))
		    getOperatorEntryByMutant(operators_stats, name).incrementKilledMutants();
		else
		    getOperatorEntryByMutant(operators_stats, name).addNotKilledToAList(name);
		index++;
		if (index >= mutant_names.size())
		    break;
		entry = mutant_names.get(index);

		// delete corresponding mutation file because its either killed or equivalent
		if (mutant != null && mutant.exists())
		    mutant.delete();
		continue;
	    }

	    // check if mutant has same content of original code
	    boolean equal = FileUtils.contentEquals(new File(eol_original_code), mutant);

	    if (equal) // equivalent
	    {
		mutant.delete();
		mutant_names.remove(index);
		entry = "E<>" + entry;
		mutant_names.add(index, entry);
		++index;
		if (index >= mutant_names.size())
		    break;
		entry = mutant_names.get(index);
		continue;
	    }

	    if (mutant == null)
		throw new NullPointerException("Unable to find mutation correspoding to: " + name);

	    if (mutant.getName().endsWith(".eol")) {

		boolean valid_mutant = true;

		File exe_temp_dir = new File(execution_dir.getPath() + File.separatorChar + mutant.getName() + TEMP_SUFFIX);
		exe_temp_dir.mkdir();
		exe_temp_dir.deleteOnExit();

		while (true) {
		    File mutant_code = new File(exe_temp_dir.getPath() + File.separatorChar + eol_name + ".eol");
		    Files.copy(mutant.toPath(), mutant_code.toPath(), StandardCopyOption.REPLACE_EXISTING);

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
			EolExecutor exe = new EolExecutor(base, eol_name, type, metamodels, mutant.getName(), exe_temp_dir.getPath(), num, input_tests.get(num));
			Future<String> f = executorService.submit(exe);
			Thread.sleep(max_exe / SLEEPING_FACTOR);
			futures.put(f, exe);
		    }

		    boolean killed = false;

		    Set<Future<String>> fs = futures.keySet();
		    for (Future<String> future : fs) {
			try {
			    String res = future.get(max_exe, TimeUnit.MILLISECONDS);
			    if (res.startsWith("K") || res.startsWith("E")) {
				killed = true;
				break;
			    }
			} catch (Exception | Error e) {
			    if (e instanceof TimeoutException) {
				killed = true;
				break;
			    } else {
				executorService.shutdownNow();
				cleanup(execution_dir);
				throw e;
			    }
			}
		    }

		    // terminate all executions
		    for (Future<String> future : fs) {
			futures.get(future).getExecutionController().dispose();
		    }

		    executorService.shutdownNow();

		    int counter = EXECUTOR_SERVICE_SLEEPING_REPEAT;
		    while (!executorService.isTerminated()) {
			if (counter == 0) {
			    cleanup(execution_dir);
			    throw new RuntimeException("The execution service of mutant [" + mutant.getName() + "] was not terminated.");
			}
			counter--;
			Thread.sleep(SLEEPING_FACTOR / 2);
		    }

		    if (!killed) {
			getOperatorEntryByMutant(operators_stats, mutant.getName()).addNotKilledToAList(mutant.getName());
		    } else {
			mutant_names.remove(index);
			entry = "L<>" + entry;
			mutant_names.add(index, entry);
			getOperatorEntryByMutant(operators_stats, mutant.getName()).incrementKilledMutants();
			// delete the file
			mutant.delete();
		    }

		    futures = null;

		    // exit execution loop
		    break;
		}

		if (!valid_mutant) {
		    current_operator.incrementInvalidMutants();
		}
	    } // end of executing one mutation
	    index++;
	    if (index >= mutant_names.size())
		break;
	    entry = mutant_names.get(index);
	} // end of executing all mutations

	// clear execution temporary files and folders
	cleanup(execution_dir);

	// finally print the execution report
	print_summary(operators_stats);
    }

    private File getCorrespondingMutantFileByName(File[] mutants_code, String name) {
	for (int i = 0; i < mutants_code.length; i++) {
	    String m = mutants_code[i].getName().substring(0, mutants_code[i].getName().length() - 8);
	    if (name.equals(m)) {
		return mutants_code[i];
	    }
	}
	return null;
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

    private void print_summary(List<OperatorEntry> list) throws IOException {
	if (list == null)
	    return;
	Iterator<OperatorEntry> it = list.iterator();

	// FileWriter file = new FileWriter(outputs_dir.getPath() + File.separatorChar + eol_name + ".csv");

	int sum_processed = 0, sum_trivial = 0, sum_killed = 0, sum_not_killed = 0, sum_invalid = 0;

	String not_killed_list = "";

	while (it.hasNext()) {

	    OperatorEntry operator = it.next();
	    sum_processed += operator.getTotalProcessedMutants();

	    sum_trivial += operator.getTotalTrivialMutants();

	    sum_killed += operator.getTotalKilledMutants();

	    sum_not_killed += operator.getAllNotKilled().size();
	    for (String nk : operator.getAllNotKilled())
		not_killed_list += "\n\t\t\\-->" + nk;

	    sum_invalid += operator.getTotalInvalidMutants();
	}

	try (FileWriter log = new FileWriter(outputs_dir.getPath() + File.separatorChar + eol_name + ".txt")) {

	    log.write("\t\\--> Processed mutants-> " + sum_processed + "\n");
	    log.write("\t\\--> Invalid mutants-> " + sum_invalid + "\n");
	    log.write("\t\\--> Killed mutants-> " + (sum_trivial + sum_killed) + "\n");
	    log.write("\t\\--> Not killed mutants-> " + sum_not_killed);
	    log.write(not_killed_list + "\n");
	    log.close();
	}
	try (FileWriter report = new FileWriter(MUTATIONS_DIR + eol_name + File.separatorChar + eol_name + "_lives.txt")) {
	    for (String s : mutant_names)
		report.write(s + "\n");
	    report.close();
	}
    }

    private void originalExecution() throws Exception {

	String mainModule = null;
	if (imported_by != null)
	    mainModule = imported_by;
	else
	    mainModule = eol_original_code;

	if (mainModule == null)
	    throw new Exception("Unable to find the main EOL file for execution.");

	// output folder
	File output_dir = new File(EXPECTED_MODELS_DIR + eol_name);
	output_dir.mkdirs();

	EolModule base = new EolModule();

	base.parse(new File(mainModule).getAbsoluteFile());
	if (base.getParseProblems().size() > 0)
	    throw new Exception("Unable to parse file: " + mainModule + "\n" + base.getParseProblems().toString());

	registerAndLoadMetamodels();

	Random rand = new Random(System.nanoTime());

	// load test inputs that conform to loaded metamodels
	List<File> inputs = null;
	for (short num : getInputFilesNumbers()) {

	    inputs = getInputFilesOfNumber(num);

	    // clone base EolModule for multiple use
	    EolModule eol = EolCloneFactory.clone(base);
	    eol.getContext().getExecutorFactory().setExecutionController(new DefaultExecutionController());

	    // Three types of outputs
	    // 1) Models create
	    // 2) Models update
	    // 3) Console output

	    int n = rand.nextInt(Integer.MAX_VALUE);

	    if (type == EOLCandidate.CONSOLE_TYPE) {
		// read all input models
		for (File f : inputs) {
		    String aliase = f.getName().substring(0, f.getName().indexOf("_"));
		    IModel m = newEmfModel("M_" + aliase + "_" + n, aliase, f.getPath(), getMetamodelUri(aliase), true, false);
		    eol.getContext().getModelRepository().addModel(m);
		}
		// console output -> there is only one output
		String m_path = output_dir.getPath() + File.separatorChar + eol_name + "_" + num + ".text";
		eol.getContext().setOutputStream(new PrintStream(new FileOutputStream(m_path)));
	    } else if (type == EOLCandidate.MODEL_CREATE_TYPE) {
		String loaded_input_metamodes = "";
		// read all input models
		for (File f : inputs) {
		    String aliase = f.getName().substring(0, f.getName().indexOf("_"));
		    loaded_input_metamodes += "," + aliase;
		    File new_f = new File(output_dir.getPath() + File.separatorChar + aliase + "_" + num + ".xmi");
		    Files.copy(f.toPath(), new_f.toPath(), StandardCopyOption.REPLACE_EXISTING);
		    IModel m = newEmfModel("M_" + aliase + "_" + n, aliase, new_f.getPath(), getMetamodelUri(aliase), true, false);
		    eol.getContext().getModelRepository().addModel(m);
		}
		// add all output models that their aliases were not loaded as input models
		for (EmfMetaModel mm : getOutputMetamodelUris(loaded_input_metamodes.split(","))) {
		    File new_f = new File(output_dir.getPath() + File.separatorChar + mm.getName() + "_" + num + ".xmi");
		    IModel m = newEmfModel("M_" + mm.getName() + "_" + n, mm.getName(), new_f.getPath(), mm.getMetamodelUri(), false, true);
		    eol.getContext().getModelRepository().addModel(m);
		}
	    } else if (type == EOLCandidate.MODEL_UPDATE_TYPE) {
		// copy all input models to output models
		for (File f : inputs) {
		    File new_f = new File(output_dir.getPath() + File.separatorChar + f.getName());
		    Files.copy(f.toPath(), new_f.toPath(), StandardCopyOption.REPLACE_EXISTING);
		    String aliase = f.getName().substring(0, f.getName().indexOf("_"));
		    IModel m = newEmfModel("M_" + aliase + "_" + n, aliase, new_f.getPath(), getMetamodelUri(aliase), false, true);
		    eol.getContext().getModelRepository().addModel(m);
		}
	    }

	    eol.execute();

	    if (type == EOLCandidate.CONSOLE_TYPE)
		eol.getContext().getOutputStream().close();

	    eol.getContext().getModelRepository().dispose();
	    eol.clearCache();
	    eol = null;
	}
    }

    private String getMetamodelUri(String a) throws Exception {
	for (EmfMetaModel mm : metamodels) {
	    if (a.equals(mm.getName()))
		return mm.getMetamodelUri();
	}
	throw new Exception("Unable to find the nsURI of Ecore model:" + a);
    }

    private List<EmfMetaModel> getOutputMetamodelUris(String[] loaded_input_mms) throws Exception {

	List<EmfMetaModel> returned = new ArrayList<EmfMetaModel>();
	boolean found;
	for (EmfMetaModel mm : metamodels) {
	    found = false;
	    for (String s : loaded_input_mms) {
		if (s.equals(mm.getName())) {
		    found = true;
		    break;
		}
	    }
	    if (!found)
		returned.add(mm);
	}
	return returned;
    }

    private IModel newEmfModel(String name, String aliases, String m, String mm, boolean read, boolean store) throws EolModelLoadingException, URISyntaxException {
	IModel emfModel = new EmfModel();
	StringProperties properties = new StringProperties();
	properties.put(EmfModel.PROPERTY_NAME, name);
	properties.put(EmfModel.PROPERTY_ALIASES, aliases);
	properties.put(EmfModel.PROPERTY_METAMODEL_URI, mm);
	// properties.put(EmfModel.PROPERTY_FILE_BASED_METAMODEL_URI, new URI(mm).toString());
	properties.put(EmfModel.PROPERTY_MODEL_URI, new URI(m).toString());
	properties.put(EmfModel.PROPERTY_READONLOAD, read + "");
	properties.put(EmfModel.PROPERTY_STOREONDISPOSAL, store + "");
	emfModel.load(properties, (IRelativePathResolver) null);
	return emfModel;
    }

    private List<Short> getInputFilesNumbers() {
	List<Short> numbers = new ArrayList<Short>();
	File dir = new File(IN_MODELS_DIR + eol_name);
	short num;
	if (dir.isDirectory()) {
	    for (File f : dir.listFiles()) {
		if (!f.getName().endsWith(".xmi"))
		    continue;
		// numbers of files reside at the end of file name before extension
		// example: MMName_NUM.xmi
		// index number 1
		// get the number with out the extension
		String ss[] = f.getName().split("_");
		num = Short.parseShort(ss[1].replaceAll(".xmi", ""));
		if (!numbers.contains(num))
		    numbers.add(num);
	    }
	}
	return numbers;
    }

    private List<File> getInputFilesOfNumber(short num) {
	List<File> returned = new ArrayList<File>();
	File dir = new File(IN_MODELS_DIR + eol_name);
	short num_search;
	if (dir.isDirectory()) {
	    for (File f : dir.listFiles()) {
		if (f.getName().endsWith(".xmi")) {
		    // numbers of files reside at the end of file name before extension
		    // example: MMName_NUM.xmi
		    // index number 1
		    // get the number with out the extension
		    String ss[] = f.getName().split("_");
		    num_search = Short.parseShort(ss[1].replaceAll(".xmi", ""));
		    if (num_search == num)
			returned.add(f);
		}
	    }
	}
	return returned;
    }
}