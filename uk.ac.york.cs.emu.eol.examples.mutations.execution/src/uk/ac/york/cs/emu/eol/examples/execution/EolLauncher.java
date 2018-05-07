package uk.ac.york.cs.emu.eol.examples.execution;

import org.apache.log4j.Logger;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.Level;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.eclipse.emf.compare.Comparison;
import org.eclipse.emf.compare.EMFCompare;
import org.eclipse.emf.compare.scope.DefaultComparisonScope;
import org.eclipse.emf.compare.scope.IComparisonScope;
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
import org.eclipse.epsilon.eol.exceptions.EolRuntimeException;
import org.eclipse.epsilon.eol.exceptions.models.EolModelLoadingException;
import org.eclipse.epsilon.eol.metamodel.EOLElement;
import org.eclipse.epsilon.eol.metamodel.EolPackage;
import org.eclipse.epsilon.eol.models.IModel;
import org.eclipse.epsilon.eol.models.IRelativePathResolver;
import org.eclipse.epsilon.eol.visitor.printer.impl.EolPrinter;

import com.google.common.io.Files;

import uk.ac.york.cs.emu.eol.examples.execution.candidates.EOLCandidate;
import uk.ac.york.cs.emu.eol.examples.execution.configurations.Configuration;

public class EolLauncher implements Runnable {

	// Important directories
	public static final String IN_MODELS_DIR = "inModels" + File.separatorChar;
	public static final String EXPECTED_MODELS_DIR = "expectedModels" + File.separatorChar;
	public static final String MUTATIONS_DIR = "mutations" + File.separatorChar;

	// parameters used by the launcher
	private String eol_name = null;
	private short type;
	private int max_exe;
	private String imported_by = null;
	private String[] importing = null;
	private String[] mm_paths = null;
	private EmfMetaModel metamodels[] = null;

	FileWriter logger = null;

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

	@Override
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
		File log_file = new File(mutations_execution.getAbsolutePath() + File.separatorChar + eol_name + ".log");
		logger = new FileWriter(log_file);
		File temp_dir = new File("temp");
		temp_dir.mkdirs();
		temp_dir.deleteOnExit();

		EMFCompare comparator = EMFCompare.builder().build();

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

		// go through all mutations
		for (File mutant_model : mutants.listFiles()) {
			if (mutant_model.getName().endsWith(".xmi")) {

				OperatorEntry current_operator = getOperatorEntryByMutant(operators_stats, mutant_model.getName());
				if (current_operator == null) {
					current_operator = new OperatorEntry(mutant_model);
					operators_stats.add(current_operator);
				}
				current_operator.incrementProcessed();

				boolean compilable = true;
				boolean exit = false;

				while (!exit) {

					String code_content = getCodeOfModel(mutant_model);
					if (code_content == null) {
						compilable = false;
						break;
					}

					// save the code into a temporary file
					String mutant_code_path = temp_dir.getAbsolutePath() + File.separatorChar + eol_name + ".eol";
					File mutant_code = new File(mutant_code_path);
					FileWriter fw = new FileWriter(mutant_code);
					fw.write(code_content);
					fw.close();

					// copy over imported modules to temporary execution folder
					if (importing != null) {
						for (String src : importing) {
							String dest = temp_dir.getAbsolutePath() + File.separatorChar + src.substring(src.lastIndexOf(File.separatorChar), src.length());
							Files.copy(new File(src), new File(dest));
						}
					}

					// set main epsilon module
					File mainModule = null;
					if (imported_by != null)
						mainModule = new File(imported_by);
					else
						mainModule = mutant_code;

					EolModule eol = new EolModule();
					eol.parse(mainModule.getAbsoluteFile());

					if (eol.getParseProblems().size() > 0) {
						logger.write("Unable to parse file EOL code of: " + mutant_model.getPath() + "\n" + eol.getParseProblems().toString() + "\n");
						compilable = false;
						break;
					}

					MutationExecutionController exe_controller = new MutationExecutionController();
					eol.getContext().getExecutorFactory().setExecutionController(exe_controller);

					int total_killed = 0;

					ResourceSet rs = new ResourceSetImpl();

					// load test inputs
					List<File> input_files = null;
					for (short num : getInputFilesNumbers()) {
						rs.getResources().clear();
						input_files = getInputFilesOfNumber(num);
						boolean killed = false;

						// three types of outputs
						// 1) Models create
						// 2) Models update
						// 3) Console output

						String model_path = null;
						IModel _model = null;
						String aliase = null;
						List<String> exe_outputs = new ArrayList<String>();

						if (type == EOLCandidate.MODEL_CREATE_TYPE || type == EOLCandidate.CONSOLE_TYPE) {

							String loaded_input_metamodes = "";
							// read all input models
							for (File f : input_files) {
								// aliase of a model is located at the beginning of an input file
								aliase = f.getName().substring(0, f.getName().indexOf("_"));
								loaded_input_metamodes += "," + aliase;
								_model = newEmfModel("M_IN_" + f.getName(), aliase, f.getAbsolutePath(), getMetamodelUri(aliase), true, false);
								eol.getContext().getModelRepository().addModel(_model);
							}
							if (type == EOLCandidate.MODEL_CREATE_TYPE) {
								// add all output models that their aliases were not loaded as input models
								for (EmfMetaModel mm : getOutputMetamodelUris(loaded_input_metamodes.split(","))) {
									model_path = temp_dir.getAbsolutePath() + File.separatorChar + mm.getName() + "_output.xmi";
									_model = newEmfModel("M_OUT_+" + mm.getName(), mm.getName(), new File(model_path).getPath(), mm.getMetamodelUri(), false, true);
									exe_outputs.add(model_path);
									eol.getContext().getModelRepository().addModel(_model);
								}
							} else {
								// console output -> there is only one output
								String _n = input_files.get(0).getName();
								model_path = temp_dir.getAbsolutePath() + File.separatorChar + _n.substring(0, _n.lastIndexOf(".xmi")) + "_output.text";
								exe_outputs.add(model_path);
								eol.getContext().setOutputStream(new PrintStream(new FileOutputStream(model_path)));
							}
						}
						if (type == EOLCandidate.MODEL_UPDATE_TYPE) {
							// copy all input models to output models
							for (File f : input_files) {
								model_path = temp_dir.getAbsolutePath() + File.separatorChar + f.getName() + "_output.xmi";
								Files.copy(f, new File(model_path));
								aliase = f.getName().substring(0, f.getName().indexOf("_"));
								_model = newEmfModel("M_" + f.getName(), aliase, model_path, getMetamodelUri(aliase), true, true);
								exe_outputs.add(model_path);
								eol.getContext().getModelRepository().addModel(_model);
							}
						}

						ExecutorService executorService = Executors.newSingleThreadExecutor();
						EolExecutor exe = new EolExecutor(eol, " of model: " + mutant_model.getPath());

						// Submit and execute the mutant the task for execution
						Future<Boolean> future = executorService.submit(exe);
						Boolean res = Boolean.FALSE;
						try {
							res = future.get(max_exe, TimeUnit.MILLISECONDS);
						} catch (TimeoutException e) {
							// terminate current execution
							logger.write(String.format("Maximum exection time is reached (%d ms): %s\n", max_exe, exe.getName()));
							future.cancel(true);
							exe_controller.terminate();
							res = Boolean.FALSE;
						}

						executorService.shutdown();

						if (res.equals(Boolean.FALSE)) {
							killed = true;
						} else {
							eol.getContext().getModelRepository().dispose();
							if (type == EOLCandidate.CONSOLE_TYPE)
								eol.getContext().getOutputStream().close();
						}

						eol.getContext().getModelRepository().getModels().clear();

						if (!killed) {
							// load expected output models and compare with actual out models
							// actual modules are stored in exe_outputs
							// go through all actual outputs
							for (String a : exe_outputs) {
								// a = actual output
								// e = expected output
								String e = null;
								// two types of output (.xmi, and .text)
								if (a.endsWith(".xmi")) {
									e = EXPECTED_MODELS_DIR + File.separatorChar + eol_name + File.separatorChar + a.substring(a.lastIndexOf(File.separatorChar), a.length());
									// load actual and expected resources
									Resource expected = getResource(rs, new File(e));
									expected.load(Collections.EMPTY_MAP);
									Resource actual = getResource(rs, new File(a));
									actual.load(Collections.EMPTY_MAP);

									// compare the two resources
									IComparisonScope scope = new DefaultComparisonScope(expected, actual, null);
									Comparison result = comparator.compare(scope);

									if (result != null)
										killed = result.getDifferences().size() >= 1;
								}
								if (a.endsWith(".text")) {
									e = EXPECTED_MODELS_DIR + File.separatorChar + eol_name + File.separatorChar + a.substring(a.lastIndexOf(File.separatorChar), a.length());
									File expected = new File(e);
									File actual = new File(a);
									boolean equal = FileUtils.contentEquals(expected, actual);
									if (!equal)
										killed = true;
								}

								if (killed)
									break; // go to next file input set
							}
							rs.getResources().clear();
						} // end comparing the outputs of actual and expected resources
						if (killed)
							total_killed++;
					} // end going through all test inputs

					if (total_killed == 0) {
						// the mutant wasn't killed by any test input
						getOperatorEntryByMutant(operators_stats, mutant_model.getName()).addNotKilled(mutant_model.getName());
					} else {
						getOperatorEntryByMutant(operators_stats, mutant_model.getName()).incrementKilledMutants();
					}

					// terminate execution loop
					exit = true;
				}

				if (!compilable)
					current_operator.incrementInvalid();

				// clear execution temporary files
				if (temp_dir != null && temp_dir.isDirectory()) {
					for (File f : temp_dir.listFiles())
						f.delete();
				}
			} // end of executing one mutation
		} // end of executing all mutations

		// finally print the execution report
		try {
			print_summary(operators_stats, mutations_execution.getAbsolutePath());
			logger.close();
		} catch (

		IOException e) {
			e.printStackTrace();
		}
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

	private Resource getResource(ResourceSet rs, File mutant) {
		return rs.getResource(org.eclipse.emf.common.util.URI.createFileURI(mutant.getAbsolutePath()), true);
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
				// numbers of files reside at the end of file name before extension
				// example: MMName_MName_input_NUM.xmi
				// index number 3
				s = f.getName();
				// get the number with out the extension
				num = Short.parseShort(s.substring(s.lastIndexOf("_") + 1, s.lastIndexOf(".xmi")));
				if (!numbers.contains(num))
					numbers.add(num);
			}
		}
		return numbers;
	}

	private List<File> getInputFilesOfNumber(short num) {
		List<File> returned = new ArrayList<File>();
		File dir = new File(IN_MODELS_DIR + eol_name);
		if (dir.isDirectory()) {
			for (File f : dir.listFiles()) {
				if (f.getName().contains(String.valueOf(num)))
					returned.add(f);
			}
		}
		return returned;
	}

	private String getCodeOfModel(File model) throws IOException {
		String returned = null;
		// covert the EOL mutant model to EOL code
		ResourceSet rs = new ResourceSetImpl();
		Resource mutant_resource = null;
		mutant_resource = getResource(rs, model);
		try {
			mutant_resource.load(Collections.EMPTY_MAP);
			EObject root = mutant_resource.getContents().get(0);
			EolPrinter printer = new EolPrinter();
			returned = printer.print((EOLElement) root);
		} catch (Exception e) {
			logger.write("Unable to print EOL model: " + model.getPath() + "\n");
			return returned;
		}
		return returned;
	}

	private void print_summary(List<OperatorEntry> list, String path) throws IOException {
		if (list == null)
			return;

		Iterator<OperatorEntry> it = list.iterator();
		StringBuilder entry = new StringBuilder();

		entry.append("Mutation Operator");
		entry.append(",");
		entry.append("Gen.");
		entry.append(",");
		entry.append("Trivial");
		entry.append(",");
		entry.append("Killed");
		entry.append(",");
		entry.append("Not Killed");
		entry.append(",");
		entry.append("Not Compiled");
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

			entry.append(operator.getKilledMutants());
			sum_killed += operator.getKilledMutants();
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

		entry.append("Total");
		entry.append(",");
		entry.append(sum_processed);
		entry.append(",");
		entry.append(sum_trivial);
		entry.append(",");
		entry.append(sum_killed);
		entry.append(",");
		entry.append(sum_not_killed);
		entry.append(",");
		entry.append(sum_invalid);

		try (FileWriter log = new FileWriter(path + File.separatorChar + eol_name + ".txt")) {

			log.write("\t\\--> Processed mutants-> " + sum_processed + "\n");
			log.write("\t\\--> Invalid mutants-> " + sum_invalid + "\n");
			log.write("\t\\--> Killed mutants-> " + sum_killed + "\n");
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

	private class EolExecutor implements Callable<Boolean> {

		private String name;
		private EolModule eol;
		private volatile Boolean execution_status = Boolean.FALSE;

		public EolExecutor(EolModule _eol, String str) {
			eol = _eol;
			name = str;
		}

		@Override
		public Boolean call() throws Exception {
			try {
				eol.execute();
				execution_status = true;
			} catch (EolRuntimeException | VirtualMachineError e) {
				execution_status = false;
			}
			return execution_status;
		}

		public String getName() {
			return name;
		}
	}
}