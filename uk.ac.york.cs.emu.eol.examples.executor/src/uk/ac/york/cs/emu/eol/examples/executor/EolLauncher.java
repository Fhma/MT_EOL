package uk.ac.york.cs.emu.eol.examples.executor;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.PrintStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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
import org.eclipse.epsilon.eol.models.IModel;
import org.eclipse.epsilon.eol.models.IRelativePathResolver;
import com.google.common.io.Files;
import uk.ac.york.cs.emu.eol.examples.executor.candidates.EOLCandidate;
import uk.ac.york.cs.emu.eol.examples.executor.configurations.Configuration;

public class EolLauncher implements Runnable {

	// execution types
	public static final String MUTATION_EXECUTION = "mutation_execution";
	public static final String ORIGINAL_EXECUTION = "original_execution";

	// important directories
	public static final String IN_MODELS_DIR = "inModels" + File.separatorChar;
	public static final String EXPECTED_MODELS_DIR = "expectedModels" + File.separatorChar;
	public static final String REPORT_DIR = "execution_report" + File.separatorChar;

	// parameters used by launcher
	private String executionMode = null;
	private String eol_name = null;
	private String eol_path = null;
	private short type;
	private String[] imported_by = null;

	private String[] mm_paths = null;
	private EmfMetaModel metamodels[] = null;

	FileWriter logger = null;

	public EolLauncher(Map<String, Object> config, String execution_mode) {
		this.executionMode = execution_mode;
		this.eol_name = (String) config.get(Configuration.EOL_NAME);
		this.eol_path = (String) config.get(Configuration.EOL_CODE);
		this.type = (short) config.get(Configuration.PROGRAM_TYPE);

		if (config.get("IMPORTED_BY") != null)
			imported_by = ((String) config.get(Configuration.IMPORTED_BY)).split(",");

		if (config.get("MM_METAMODELS") != null)
			metamodels = (EmfMetaModel[]) config.get(Configuration.MM_METAMODELS);

		if (config.get("MM_PATHS") != null)
			mm_paths = ((String) config.get(Configuration.MM_PATHS)).split(",");
	}

	@Override
	public void run() {
		if (executionMode == null)
			throw new IllegalArgumentException("Unknown execution mode.");
		try {
			if (executionMode == ORIGINAL_EXECUTION)
				originalExecutionMode();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void originalExecutionMode() throws Exception {

		String mainModule = null;
		if (imported_by != null && imported_by.length == 1)
			mainModule = imported_by[0];
		else
			mainModule = eol_path;

		if (mainModule == null)
			throw new Exception("Unable to find main EOL file to execute.");

		// report dir
		File report_dir = new File(REPORT_DIR + File.separatorChar);
		report_dir.mkdirs();
		File log_file = new File(report_dir.getPath() + File.separatorChar + eol_name + ".log");
		logger = new FileWriter(log_file);

		// output folder
		File output_dir = new File(EXPECTED_MODELS_DIR + eol_name);
		output_dir.mkdirs();

		EolModule eol = null;

		// register and load metamodels
		registerAndLoadMetamodels();

		// load test inputs that conform to loaded metamodels
		List<File> input_files = null;
		for (short num : getInputFilesNumbers()) {
			input_files = getInputFilesOfNumber(num);

			eol = new EolModule();
			eol.parse(new File(mainModule).getAbsoluteFile());

			if (eol.getParseProblems().size() > 0) {
				throw new Exception("Unable to parse file: " + mainModule + "\n" + eol.getParseProblems().toString());
			}

			// Three types of outputs
			// 1) Models create
			// 2) Models update
			// 3) Console output

			String model_path = null;
			IModel _model = null;
			String aliase = null; // aliase of a model is located at the beginning of an input file

			if (type == EOLCandidate.CONSOLE_TYPE) {
				// read all input models
				for (File f : input_files) {
					aliase = f.getName().substring(0, f.getName().indexOf("_"));
					_model = newEmfModel("M_IN_" + f.getName(), aliase, f.getPath(), getMetamodelUri(aliase), true, false);
					eol.getContext().getModelRepository().addModel(_model);
				}
				// console output -> there is only one output
				model_path = output_dir.getPath() + File.separatorChar + aliase + "_" + num + ".text";
				eol.getContext().setOutputStream(new PrintStream(new FileOutputStream(model_path)));

			} else if (type == EOLCandidate.MODEL_CREATE_MULTI_TYPE) {
				String loaded_input_metamodes = "";
				// read all input models
				for (File f : input_files) {
					aliase = f.getName().substring(0, f.getName().indexOf("_"));
					loaded_input_metamodes += "," + aliase;
					model_path = output_dir.getPath() + File.separatorChar + f.getName();
					File new_f = new File(model_path);
					Files.copy(f, new_f);
					_model = newEmfModel("M_IN_" + f.getName(), aliase, new_f.getPath(), getMetamodelUri(aliase), true, false);
					eol.getContext().getModelRepository().addModel(_model);
				}
				// add all output models that their aliases were not loaded as input models
				for (EmfMetaModel mm : getOutputMetamodelUris(loaded_input_metamodes.split(","))) {
					model_path = output_dir.getPath() + File.separatorChar + mm.getName() + "_" + num + ".xmi";
					_model = newEmfModel("M_OUT_" + mm.getName() + num, mm.getName(), new File(model_path).getPath(), mm.getMetamodelUri(), false, true);
					eol.getContext().getModelRepository().addModel(_model);
				}
			} else if (type == EOLCandidate.MODEL_CREATE_TYPE) {
				// copy all input models to output models
				for (File f : input_files) {
					model_path = output_dir.getPath() + File.separatorChar + f.getName();
					Files.copy(f, new File(model_path));
					aliase = f.getName().substring(0, f.getName().indexOf("_"));
					_model = newEmfModel("M_" + f.getName(), aliase, model_path, getMetamodelUri(aliase), false, true);
					eol.getContext().getModelRepository().addModel(_model);
				}
			} else if (type == EOLCandidate.MODEL_UPDATE_TYPE) {
				// copy all input models to output models
				for (File f : input_files) {
					model_path = output_dir.getPath() + File.separatorChar + f.getName();
					Files.copy(f, new File(model_path));
					aliase = f.getName().substring(0, f.getName().indexOf("_"));
					_model = newEmfModel("M_" + f.getName(), aliase, model_path, getMetamodelUri(aliase), true, true);
					eol.getContext().getModelRepository().addModel(_model);
				}
			}

			try {
				eol.execute();
			} catch (Error e) {
				logger.write("Unable to execute file [" + mainModule + "] on input file [" + input_files.toString() + "]");
				logger.write(e.getStackTrace().toString() + "\n");
			} catch (Exception e) {
				logger.write("Unable to execute file [" + mainModule + "] on input file [" + input_files.toString() + "]");
				logger.write(e.getStackTrace().toString() + "\n");
			}
			if (type == EOLCandidate.CONSOLE_TYPE)
				eol.getContext().getOutputStream().close();
			eol.getContext().getModelRepository().dispose();
			eol.getContext().getModelRepository().getModels().clear();
			eol.getContext().dispose();
			eol = null;
		}
		logger.flush();
		logger.close();

	}

	private String getMetamodelUri(String a) throws Exception {
		for (EmfMetaModel mm : metamodels) {
			if (a.equals(mm.getName())) {
				return mm.getMetamodelUri();
			}
		}
		throw new Exception("Unable to find the URI metamodel of this model:" + a);
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
