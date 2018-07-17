package uk.ac.york.cs.emu.eol.examples.mutations.executor;

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
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;
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
import org.eclipse.epsilon.eol.exceptions.models.EolModelLoadingException;
import org.eclipse.epsilon.eol.metamodel.EOLElement;
import org.eclipse.epsilon.eol.metamodel.EolPackage;
import org.eclipse.epsilon.eol.models.IModel;
import org.eclipse.epsilon.eol.models.IRelativePathResolver;
import org.eclipse.epsilon.eol.visitor.printer.impl.EolPrinter;

import uk.ac.york.cs.emu.eol.examples.mutations.executor.candidates.EOLCandidate;
import uk.ac.york.cs.emu.eol.examples.mutations.executor.configurations.Configuration;

public class EolLauncher implements Runnable {

	// Important directories
	public static final String IN_MODELS_DIR = "inModels" + File.separatorChar;
	public static final String EXPECTED_MODELS_DIR = "expectedModels" + File.separatorChar;
	public static final String BASE = ".";
	public static final String MUTATIONS_DIR = BASE + File.separatorChar + "mutations" + File.separatorChar;

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
		File log_file = new File(mutations_execution.getPath() + File.separatorChar + eol_name + ".log");
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

		Random rand = new Random(System.nanoTime());

		// go through all mutations
		for (File mutant_model : mutants.listFiles()) {
			if (mutant_model.getName().endsWith(".xmi")) {
				// System.out.println("Mutant: " + mutant_model.getName());
				OperatorEntry current_operator = getOperatorEntryByMutant(operators_stats, mutant_model.getName());
				if (current_operator == null) {
					current_operator = new OperatorEntry(mutant_model);
					operators_stats.add(current_operator);
				}
				current_operator.incrementProcessed();

				boolean valid_mutant = true;

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

					int total_killed = 0;

					ResourceSet rs = new ResourceSetImpl();

					// load test inputs
					List<File> input_files = null;
					for (short num : getInputFilesNumbers()) {
						// System.err.println("current number files: " + num);
						rs.getResources().clear();
						input_files = getInputFilesOfNumber(num);
						boolean killed = false;

						// parsing EOL code here is very bad
						EolModule eol = new EolModule();
						eol.parse(mainModule.getAbsoluteFile());
						if (eol.getParseProblems().size() > 0) {
							logger.write("Unable to parse file EOL code of: " + mutant_model.getPath() + "\n\t" + eol.getParseProblems().toString() + "\n");
							valid_mutant = false;
							break;
						}

						MutationExecutionController exe_controller = new MutationExecutionController();
						eol.getContext().getExecutorFactory().setExecutionController(exe_controller);

						// three types of outputs
						// 1) Models create
						// 2) Models update
						// 3) Console output

						String model_path = null;
						IModel _model = null;
						String aliase = null;
						Map<String, ModelPair> output_models = new HashMap<String, ModelPair>();

						int n = rand.nextInt(Integer.MAX_VALUE);

						if (type == EOLCandidate.CONSOLE_TYPE) {
							// read all input models
							for (File f : input_files) {
								// aliase of a model is located at the beginning of an input file
								aliase = f.getName().substring(0, f.getName().indexOf("_"));
								_model = newEmfModel("M_IN_" + aliase + "_" + n, aliase, f.getPath(), getMetamodelUri(aliase), true, false);
								eol.getContext().getModelRepository().addModel(_model);
							}
							// console output -> there is only one output
							model_path = eol_name + "_" + num + ".text";
							output_models.put(aliase, new ModelPair());
							output_models.get(aliase).setActual(model_path);
							eol.getContext().setOutputStream(new PrintStream(new FileOutputStream(model_path)));
						} else if (type == EOLCandidate.MODEL_CREATE_TYPE) {
							String loaded_input_metamodes = "";
							// read all input models
							for (File f : input_files) {
								// aliase of a model is located at the beginning of an input file
								aliase = getSegment(f.getName(), "_", 0);
								// aliase = f.getName().substring(0, f.getName().indexOf("_"));
								loaded_input_metamodes += "," + aliase;
								// model_path = f.getPath();
								model_path = aliase + "_" + num + ".xmi";
								// File new_f = new File(model_path);
								// Files.copy(f.toPath(), new_f.toPath(), StandardCopyOption.REPLACE_EXISTING);
								// _model = newEmfModel("M_IN_" + aliase + "_" + n, aliase, new_f.getPath(), getMetamodelUri(aliase), true, false);
								_model = newEmfModel("M_IN_" + aliase + "_" + n, aliase, f.getPath(), getMetamodelUri(aliase), true, false);
								eol.getContext().getModelRepository().addModel(_model);
							}

							// add all output models that their aliases were not loaded as input models
							for (EmfMetaModel mm : getOutputMetamodelUris(loaded_input_metamodes.split(","))) {
								model_path = mm.getName() + "_" + num + "_" + n + ".xmi";
								File new_f = new File(model_path);
								_model = newEmfModel("M_OUT_" + mm.getName() + "_" + n, mm.getName(), new_f.getPath(), mm.getMetamodelUri(), false, true);
								output_models.put(mm.getName(), new ModelPair());
								output_models.get(mm.getName()).setActual(model_path);
								eol.getContext().getModelRepository().addModel(_model);
							}
						} else if (type == EOLCandidate.MODEL_UPDATE_TYPE) {
							// copy all input models to output models
							for (File f : input_files) {
								model_path = f.getName();
								Files.copy(f.toPath(), new File(model_path).toPath(), StandardCopyOption.REPLACE_EXISTING);
								aliase = getSegment(f.getName(), "_", 1);
								_model = newEmfModel("M_" + f.getName() + "_" + n, aliase, model_path, getMetamodelUri(aliase), true, true);
								output_models.put(aliase, new ModelPair());
								output_models.get(aliase).setActual(model_path);
								eol.getContext().getModelRepository().addModel(_model);
							}
						}

						ExecutorService executorService = Executors.newSingleThreadExecutor();

						EolExecutor exe = new EolExecutor(eol, mutant_model.getPath());

						// Submit and execute the mutant
						Future<Boolean> future = executorService.submit(exe);
						try {
							future.get(max_exe, TimeUnit.MILLISECONDS);
						} catch (Exception e) {

							if (e instanceof TimeoutException)
								logger.write(String.format("Maximum exection time is reached (%d ms): %s\n", max_exe, exe.getModel()));
							else
								logger.write(String.format("An exception occurred while executing mutant %s\n", exe.getModel()));
							killed = true;
						}
						future.cancel(true);
						exe_controller.terminate();
						executorService.shutdownNow();

						if (type == EOLCandidate.CONSOLE_TYPE)
							eol.getContext().getOutputStream().close();
						try {
							eol.getContext().getModelRepository().dispose();
						} catch (Exception e) {
							logger.write("An exception occurred while saving output models");
							// killed
							killed = true;
						}

						eol.getContext().getModelRepository().getModels().clear();
						eol.getContext().dispose();
						eol.clearCache();
						eol = null;

						if (!killed) {
							// load expected output models and compare with actual outmodels
							// actual modules are stored in exe_outputs
							// go through all actual outputs
							fetchExpectedModelPaths(output_models, num);
							Iterator<Entry<String, ModelPair>> it = output_models.entrySet().iterator();
							while (it.hasNext()) {
								File e = null;
								File a = null;
								Entry<String, ModelPair> entry = it.next();
								e = new File(entry.getValue().getExpected());
								a = new File(entry.getValue().getActual());
								if (type == EOLCandidate.CONSOLE_TYPE) {
									boolean equal = FileUtils.contentEquals(e, a);
									if (!equal)
										killed = true;
								} else {
									Resource lhs = getResource(rs, e);
									lhs.load(Collections.EMPTY_MAP);
									Resource rhs = getResource(rs, a);
									rhs.load(Collections.EMPTY_MAP);

									// compare the two resources
									IComparisonScope scope = new DefaultComparisonScope(lhs, rhs, null);
									Comparison result = comparator.compare(scope);

									if (result != null)
										killed = result.getDifferences().size() >= 1;
									if (killed)
										break;
								}
							}
							rs.getResources().clear();
						} // end comparing the outputs of actual and expected resources
						if (killed)
							total_killed++;
					} // end going through all test inputs

					if (valid_mutant) {
						if (total_killed == 0) {
							// the mutant hasn't killed by any test input
							getOperatorEntryByMutant(operators_stats, mutant_model.getName()).addNotKilled(mutant_model.getName());
						} else {
							getOperatorEntryByMutant(operators_stats, mutant_model.getName()).incrementKilledMutants();
						}
					}

					// terminate execution loop
					break;
				}

				if (!valid_mutant)
					current_operator.incrementInvalid();

				// clear execution temporary files
				// clear all output files ( .xmi or .text ) of this input test
				File dir = new File(".");
				if (dir.isDirectory()) {
					for (File f : dir.listFiles()) {
						if (f.isFile() && (f.getName().endsWith(".xmi") || f.getName().endsWith(".text"))) {
							f.delete();
						}
					}
				}
				clearFolderContent(temp_dir);

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

	private void fetchExpectedModelPaths(Map<String, ModelPair> out_models, short num) throws Exception {
		// MMName_NUM(.xmi | .text)
		Set<String> mms = out_models.keySet();
		File dir = new File(EXPECTED_MODELS_DIR + File.separatorChar + eol_name + File.separatorChar);
		String segmenets[];
		boolean found = false;
		short count = 0;
		File files[] = dir.listFiles();
		for (int i = 0; i < files.length && !found; i++) {
			segmenets = files[i].getName().split("_");
			for (String mm : mms) {
				if (mm.equals(segmenets[0])) {
					Short n = null;
					if (files[i].getName().endsWith(".text"))
						n = Short.parseShort(segmenets[1].replaceAll(".text", ""));
					else if (files[i].getName().endsWith(".xmi"))
						n = Short.parseShort(segmenets[1].replaceAll(".xmi", ""));
					if (n == null)
						throw new Exception("Unable to find expected model of metamodel: " + mm);
					if (n == num) {
						if (out_models.get(mm).getExpected() != null)
							throw new Exception("Found two expected model of metamodel: " + mm);
						out_models.get(mm).setExpected(files[i].getPath());
						count++;
						if (count == mms.size()) {
							found = true;
							break;
						}
					}
				}
			}
		}
		if (count != mms.size())
			throw new Exception("One or more expected model were not found.");
	}

	private void clearFolderContent(File dir) {
		if (dir != null && dir.isDirectory()) {
			for (File f : dir.listFiles())
				f.delete();
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
		return newEmfModel(name, aliases, m, mm, read, store, false);
	}

	private IModel newEmfModel(String name, String aliases, String m, String mm, boolean read, boolean store, boolean cached) throws EolModelLoadingException, URISyntaxException {
		IModel emfModel = new EmfModel();
		StringProperties properties = new StringProperties();
		properties.put(EmfModel.PROPERTY_NAME, name);
		properties.put(EmfModel.PROPERTY_ALIASES, aliases);
		properties.put(EmfModel.PROPERTY_METAMODEL_URI, mm);
		properties.put(EmfModel.PROPERTY_MODEL_URI, new URI(m).toString());
		properties.put(EmfModel.PROPERTY_READONLOAD, read + "");
		properties.put(EmfModel.PROPERTY_CACHED, cached + "");
		properties.put(EmfModel.PROPERTY_STOREONDISPOSAL, store + "");
		emfModel.load(properties, (IRelativePathResolver) null);
		return emfModel;
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
			logger.write("Unable to generate EOL code of model: " + model.getPath() + "\n");
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

		entry.append("Total,");
		entry.append(sum_processed + ",");
		entry.append(sum_trivial + ",");
		entry.append(sum_killed + ",");
		entry.append(sum_not_killed + ",");
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

		private String model_;
		private EolModule eol;

		public EolExecutor(EolModule _eol, String model) {
			eol = _eol;
			model_ = model;
		}

		@Override
		public Boolean call() throws Exception {
			eol.execute();
			return true;
		}

		public String getModel() {
			return model_;
		}
	}

	private class ModelPair {
		private String expected;
		private String actual;

		public ModelPair() {
		}

		public void setActual(String actual) {
			this.actual = actual;
		}

		public void setExpected(String expected) {
			this.expected = expected;
		}

		public String getActual() {
			return actual;
		}

		public String getExpected() {
			return expected;
		}
	}
}