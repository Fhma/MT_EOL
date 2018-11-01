package uk.ac.york.cs.emu.eol.examples.executor;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.PrintStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EcorePackage;
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
import org.eclipse.epsilon.eol.models.IModel;
import org.eclipse.epsilon.eol.models.IRelativePathResolver;
import com.google.common.io.Files;
import uk.ac.york.cs.emu.eol.examples.executor.candidates.EOLCandidate;
import uk.ac.york.cs.emu.eol.examples.executor.configurations.Configuration;

public class EolLauncher implements Runnable {

    // important directories
    public static final String IN_MODELS_DIR = "inModels" + File.separatorChar;
    public static final String EXPECTED_MODELS_DIR = "expectedModels" + File.separatorChar;
    public static final String REPORT_DIR = "report" + File.separatorChar;
    public static final String TEMP_SUFFIX = ".tmp";

    // parameters used by launcher
    private String eol_name = null;
    private String eol_code = null;
    private short type;
    private String[] imported_by = null;
    private String[] importing = null;

    private String[] mm_paths = null;
    private EmfMetaModel metamodels[] = null;

    private PrintStream logger = null;

    public EolLauncher(Map<String, Object> config) throws Exception {
	this.eol_name = (String) config.get(Configuration.EOL_NAME);
	this.eol_code = (String) config.get(Configuration.EOL_CODE);
	this.type = (short) config.get(Configuration.PROGRAM_TYPE);

	if (config.get(Configuration.IMPORTED_BY) != null) {
	    imported_by = ((String) config.get(Configuration.IMPORTED_BY)).split(",");
	}

	if (config.get(Configuration.IMPORTING) != null) {
	    importing = ((String) config.get(Configuration.IMPORTING)).split(",");
	}

	if (config.get(Configuration.MM_METAMODELS) != null) {
	    metamodels = (EmfMetaModel[]) config.get(Configuration.MM_METAMODELS);
	}

	if (config.get(Configuration.MM_PATHS) != null) {
	    mm_paths = (String[]) config.get(Configuration.MM_PATHS);
	}
	File report = new File(REPORT_DIR);
	report.mkdirs();
	logger = new PrintStream(new File(report.getPath() + File.separatorChar + this.eol_name + ".log"));
    }

    @Override
    public void run() {
	long mins = System.currentTimeMillis();
	try {
	    logger.println("Original Transformation Execution: " + eol_name);
	    originalExecution();
	} catch (Exception e) {
	    e.printStackTrace(logger);
	} finally {
	    int time = (int) ((System.currentTimeMillis() - mins) / 1000) / 60;
	    logger.println(String.format("End Execution....(%d mins)", time));
	    logger.close();
	}
    }

    private void originalExecution() throws Exception {

	File temp = new File("execution" + TEMP_SUFFIX);
	temp.mkdirs();
	temp.deleteOnExit();

	String module_path = null;

	// copy all files to execution.tmp
	if (imported_by != null) {
	    Files.copy(new File(EOLCandidate.LOCATION + imported_by[0]), new File(temp.getPath() + File.separatorChar + imported_by[0]));
	    module_path = imported_by[0];
	} else
	    module_path = eol_code;

	if (importing != null) {
	    for (String s : importing) {
		Files.copy(new File(EOLCandidate.LOCATION + s), new File(temp.getPath() + File.separatorChar + s));
	    }
	}
	Files.copy(new File(EOLCandidate.LOCATION + eol_code), new File(temp.getPath() + File.separatorChar + eol_code));

	File module_file = new File(temp.getPath() + File.separatorChar + module_path);

	if (importing != null) {
	    // read entire file and add at the importings at beginning
	    BufferedReader br = new BufferedReader(new FileReader(module_file));
	    ArrayList<String> lines = new ArrayList<String>();
	    String line;
	    while ((line = br.readLine()) != null) {
		if (line.length() > 0) {
		    lines.add(line);
		}
	    }
	    br.close();
	    // add imports
	    for (String s : importing) {
		lines.add(0, "import '" + s + "';");
	    }
	    // write to same file
	    FileWriter fw = new FileWriter(module_file);
	    for (String s : lines) {
		fw.write(s + System.lineSeparator());
	    }
	    fw.close();
	}

	// output folder
	File output_dir = new File(EXPECTED_MODELS_DIR + eol_name);
	output_dir.mkdirs();

	EolModule base = new EolModule();

	base.parse(module_file.getAbsoluteFile());
	if (base.getParseProblems().size() > 0)
	    throw new Exception("Unable to parse file: " + module_path + "\n" + base.getParseProblems().toString());

	registerAndLoadMetamodels();

	Random rand = new Random(System.nanoTime());

	// load test inputs that conform to loaded metamodels
	List<File> input_files = null;
	for (short num : getInputFilesNumbers()) {

	    logger.println("\tExecution against inputs [" + num + "]");
	    input_files = getInputFilesOfNumber(num);

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
		for (File f : input_files) {
		    String aliase = f.getName().substring(0, f.getName().indexOf("_"));
		    IModel m = newEmfModel("M_" + aliase + "_" + n, aliase, f.getPath(), getMetamodelUri(aliase), true, false);
		    eol.getContext().getModelRepository().addModel(m);
		}
		// console output -> there is only one output
		String m_path = output_dir.getPath() + File.separatorChar + eol_name + "_" + num + ".text";
		eol.getContext().setOutputStream(new PrintStream(new FileOutputStream(m_path)));
	    } else if (type == EOLCandidate.MODEL_CREATE_MULTI_TYPE) {
		String loaded_input_metamodes = "";
		// read all input models
		for (File f : input_files) {
		    String aliase = f.getName().substring(0, f.getName().indexOf("_"));
		    loaded_input_metamodes += "," + aliase;
		    File new_f = new File(output_dir.getPath() + File.separatorChar + aliase + "_" + num + ".xmi");
		    Files.copy(f, new_f);
		    IModel m = newEmfModel("M_" + aliase + "_" + n, aliase, new_f.getPath(), getMetamodelUri(aliase), true, false);
		    eol.getContext().getModelRepository().addModel(m);
		}
		// add all output models that their aliases were not loaded as input models
		for (EmfMetaModel mm : getOutputMetamodelUris(loaded_input_metamodes.split(","))) {
		    File new_f = new File(output_dir.getPath() + File.separatorChar + mm.getName() + "_" + num + ".xmi");
		    IModel m = newEmfModel("M_" + mm.getName() + "_" + n, mm.getName(), new_f.getPath(), mm.getMetamodelUri(), false, true);
		    eol.getContext().getModelRepository().addModel(m);
		}
	    } else if (type == EOLCandidate.MODEL_CREATE_TYPE || type == EOLCandidate.MODEL_UPDATE_TYPE) {
		// copy all input models to output models
		for (File f : input_files) {
		    File new_f = new File(output_dir.getPath() + File.separatorChar + f.getName());
		    if (type == EOLCandidate.MODEL_UPDATE_TYPE)
			Files.copy(f, new_f);
		    String aliase = f.getName().substring(0, f.getName().indexOf("_"));
		    IModel m = newEmfModel("M_" + aliase + "_" + n, aliase, new_f.getPath(), getMetamodelUri(aliase), false, true);
		    eol.getContext().getModelRepository().addModel(m);
		}
	    }

	    eol.execute();

	    if (type == EOLCandidate.CONSOLE_TYPE)
		eol.getContext().getOutputStream().close();

	    eol.getContext().getModelRepository().dispose();
	    // eol.getContext().dispose();
	    eol.clearCache();
	    eol = null;
	}
	// cleanup
	cleanup(temp);

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
