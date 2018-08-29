package uk.ac.york.cs.emu.eol.examples.mutations.executor;

import java.io.File;
import java.io.PrintStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import org.apache.commons.io.FileUtils;
import org.eclipse.epsilon.common.util.StringProperties;
import org.eclipse.epsilon.emc.emf.EmfMetaModel;
import org.eclipse.epsilon.emc.emf.EmfModel;
import org.eclipse.epsilon.eol.EolModule;
import org.eclipse.epsilon.eol.exceptions.models.EolModelLoadingException;
import org.eclipse.epsilon.eol.execute.control.ExecutionController;
import org.eclipse.epsilon.eol.models.IModel;
import org.eclipse.epsilon.eol.models.IRelativePathResolver;
import org.eclipse.epsilon.eunit.dt.cmp.emf.v3.EMFModelComparator;
import uk.ac.york.cs.emu.eol.examples.mutations.executor.candidates.EOLCandidate;

public class EolExecutor implements Callable<String> {

    public static final String IN_MODELS_DIR = "inModels" + File.separatorChar;
    public static final String EXPECTED_MODELS_DIR = "expectedModels" + File.separatorChar;

    private EolModule eol = null;
    private ExecutionController controller = null;

    private short type;
    private String eol_name = null;
    private EmfMetaModel mms[] = null;
    private String mutant_model = null;
    private String execution_dir = null;
    private short input_num;
    private String signature = null;

    public EolExecutor(File src, String eol_name, short type, EmfMetaModel mms[], String mutant_model, String dir, EMFModelComparator comp, short num) throws Exception {
	eolExecutor(eol_name, type, mms, mutant_model, dir, comp, num);

	eol = new EolModule();
	eol.parse(src);
	// execution controller
	controller = new MutationExecutionController();
	eol.getContext().getExecutorFactory().setExecutionController(controller);
    }

    public EolExecutor(EolModule base, String eol_name, short type, EmfMetaModel mms[], String mutant_model, String dir, EMFModelComparator comp, short num) throws Exception {
	eolExecutor(eol_name, type, mms, mutant_model, dir, comp, num);

	eol = EolCloneFactory.clone(base);
	controller = new MutationExecutionController();
	eol.getContext().getExecutorFactory().setExecutionController(controller);
    }

    private void eolExecutor(String eol_name, short type, EmfMetaModel mms[], String mutant_model, String dir, EMFModelComparator comp, short num) throws Exception {
	this.type = type;
	this.eol_name = eol_name;
	this.mms = mms;
	input_num = num;
	this.mutant_model = mutant_model;
	this.execution_dir = dir;
	signature = String.format("[Mutant = %s, Test Input = %d]", mutant_model, input_num);
	// comparator = comp;
    }

    @Override
    public String call() throws Exception, Error {
	boolean killed = false;
	String result = null;

	if (eol.getParseProblems().size() >= 1)
	    throw new RuntimeException("Parsing problems found: " + eol.getSourceUri());

	if (eol == null)
	    throw new IllegalArgumentException("Source file or base EolModule weren't specified.");

	List<File> input_files = getInputFilesOfNumber(input_num);

	long time = System.nanoTime();

	if (type == EOLCandidate.CONSOLE_TYPE) {
	    for (File f : input_files) {
		// aliase of a model is located at the beginning of an input file
		String aliase = f.getName().substring(0, f.getName().indexOf("_"));
		File new_f = new File(execution_dir + File.separatorChar + aliase + "_" + input_num + ".xmi");
		Files.copy(f.toPath(), new_f.toPath(), StandardCopyOption.REPLACE_EXISTING);
		IModel m = newEmfModel("IN_" + aliase + "_" + input_num + "_" + time, aliase, new_f.getPath(), getMetamodelUri(aliase), true, false, false);
		eol.getContext().getModelRepository().addModel(m);
	    }
	    File new_f = new File(execution_dir + File.separatorChar + eol_name + "_" + input_num + ".text");
	    eol.getContext().setOutputStream(new PrintStream(new_f));

	} else if (type == EOLCandidate.MODEL_CREATE_TYPE) {
	    String loaded_input_metamodes = "";
	    // read all input models
	    for (File f : input_files) {
		// aliase of a model is located at the beginning of an input file
		String aliase = f.getName().substring(0, f.getName().indexOf("_"));
		loaded_input_metamodes += "," + aliase;
		File new_f = new File(execution_dir + File.separatorChar + aliase + "_" + input_num + ".xmi");
		Files.copy(f.toPath(), new_f.toPath(), StandardCopyOption.REPLACE_EXISTING);
		IModel m = newEmfModel("IN_" + aliase + "_" + input_num + "_" + time, aliase, new_f.getPath(), getMetamodelUri(aliase), true, false, false);
		eol.getContext().getModelRepository().addModel(m);
	    }

	    // add all output models that their aliases were not loaded as input models
	    for (EmfMetaModel mm : getOutputMetamodelUris(loaded_input_metamodes.split(","))) {
		File new_f = new File(execution_dir + File.separatorChar + mm.getName() + "_" + input_num + ".xmi");
		IModel m = newEmfModel("OUT_" + mm.getName() + "_" + input_num + "_" + time, mm.getName(), new_f.getPath(), mm.getMetamodelUri(), false, true, false);
		eol.getContext().getModelRepository().addModel(m);
	    }

	} else if (type == EOLCandidate.MODEL_UPDATE_TYPE) {
	    // copy all input models to output models
	    for (File f : input_files) {
		File new_f = new File(execution_dir + File.separatorChar + f.getName());
		Files.copy(f.toPath(), new_f.toPath(), StandardCopyOption.REPLACE_EXISTING);
		String aliase = f.getName().substring(0, f.getName().indexOf("_"));
		IModel m = newEmfModel("OUT_" + f.getName() + "_" + input_num + "_" + time, aliase, new_f.getPath(), getMetamodelUri(aliase), true, true, false);
		eol.getContext().getModelRepository().addModel(m);
	    }
	} else
	    throw new RuntimeException("Unable to determine EolModule type");

	try {
	    eol.execute();
	    if (type == EOLCandidate.CONSOLE_TYPE) {
		eol.getContext().getOutputStream().flush();
		eol.getContext().getOutputStream().close();

		File actual = new File(execution_dir + File.separatorChar + eol_name + "_" + input_num + ".text");
		File expected = new File(EXPECTED_MODELS_DIR + eol_name + File.separatorChar + eol_name + "_" + input_num + ".text");
		if (!actual.exists())
		    throw new RuntimeException(String.format("Unable to locate output file [%s] for comparison", actual.getPath()));
		if (!expected.exists())
		    throw new RuntimeException(String.format("Unable to locate output file [%s] for comparison", expected.getPath()));
		boolean equal = FileUtils.contentEquals(actual, expected);
		if (!equal)
		    killed = true;

	    } else {
		EMFModelComparator comparator = new EMFModelComparator();
		for (IModel m : eol.getContext().getModelRepository().getModels()) {
		    if (m.getName().startsWith("OUT")) {
			EmfModel actual = (EmfModel) m;
			String splits[] = actual.getModelFileUri().toString().split("" + File.separatorChar);
			if (splits.length != 3)
			    throw new RuntimeException("Invalid model name: " + actual.getModelFileUri().toString());
			String aliase = splits[2];
			aliase = aliase.substring(0, aliase.indexOf("_"));
			String exp_path = EXPECTED_MODELS_DIR + eol_name + File.separatorChar + aliase + "_" + input_num + ".xmi";
			String name = mutant_model + "_" + aliase + "_" + input_num + "_" + time;
			IModel expected = newEmfModel(name, name, exp_path, getMetamodelUri(aliase), true, false, false);
			Object res = comparator.compare(actual, expected);

			if (res != null) {
			    killed = true;
			    break;
			}
		    }
		}
	    }
	} catch (Exception | Error e) {
	    killed = true;
	    result = String.format("EXCEPTION: %s", signature);
	}

	dispose();

	if (killed)
	    result = String.format("KILLED: %s", signature);
	else
	    result = String.format("NOT KILLED: %s", signature);
	return result;
    }

    public void dispose() {
	eol.getContext().getOutputStream().flush();
	eol.getContext().getOutputStream().close();
	controller.dispose();
	eol.getContext().getModelRepository().getModels().clear();
	// eol.getContext().dispose();
	eol.clearCache();
	eol = null;
    }

    public String getSignature() {
	return signature;
    }

    public ExecutionController getExecutionController() {
	return controller;
    }

    private List<File> getInputFilesOfNumber(short num) {
	List<File> returned = new ArrayList<File>();
	File dir = new File(IN_MODELS_DIR + eol_name);
	if (dir.isDirectory()) {
	    for (File f : dir.listFiles()) {
		if (f.getName().endsWith("xmi")) {
		    short n = Short.parseShort(getSegment(f.getName(), "_", 1).replaceAll(".xmi", ""));
		    if (n == num)
			returned.add(f);
		}
	    }
	}
	return returned;
    }

    private IModel newEmfModel(String name, String aliases, String m, String mm, boolean read, boolean store, boolean cached) throws EolModelLoadingException, URISyntaxException {
	IModel emfModel = new EmfModel();
	StringProperties properties = new StringProperties();
	properties.put(EmfModel.PROPERTY_NAME, name);
	properties.put(EmfModel.PROPERTY_ALIASES, aliases);
	properties.put(EmfModel.PROPERTY_METAMODEL_URI, mm);
	properties.put(EmfModel.PROPERTY_MODEL_URI, new URI(m).toString());
	properties.put(EmfModel.PROPERTY_READONLOAD, (Object) read);
	properties.put(EmfModel.PROPERTY_CACHED, (Object) cached);
	properties.put(EmfModel.PROPERTY_STOREONDISPOSAL, (Object) store);
	emfModel.load(properties, (IRelativePathResolver) null);
	return emfModel;
    }

    private String getMetamodelUri(String a) throws RuntimeException {

	for (EmfMetaModel mm : mms) {
	    if (a.equals(mm.getName())) {
		return mm.getMetamodelUri();
	    }
	}
	throw new RuntimeException("Unable to find the URI metamodel of this model:" + a);
    }

    private List<EmfMetaModel> getOutputMetamodelUris(String[] loaded_input_mms) {

	List<EmfMetaModel> returned = new ArrayList<EmfMetaModel>();
	boolean found;
	for (EmfMetaModel mm : mms) {
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

    private String getSegment(String s, String delimiter, int index) {
	String[] segments = s.split(delimiter);
	return segments[index];
    }
}