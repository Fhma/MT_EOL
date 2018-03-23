package uk.ac.york.cs.emu.eol.examples.execution.candidates.launcher;

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

import org.eclipse.emf.compare.Comparison;
import org.eclipse.emf.compare.EMFCompare;
import org.eclipse.emf.compare.scope.DefaultComparisonScope;
import org.eclipse.emf.compare.scope.IComparisonScope;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;
import org.eclipse.epsilon.common.util.StringProperties;
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

public class EolLauncher implements Runnable {

	public static final String MUTATION_EXECUTION = "mutation_execution";
	public static final String ORIGINAL_EXECUTION = "original_execution";

	private String executionMode;
	private String c_module_name;
	private String c_path;
	private int type;
	private String inMM_name;
	private String inMM_File;
	private String outMM_name;
	private String outMM_File;

	// input and output models
	private String inModels;
	private String expectedOutModels;
	private String mutants_path;

	public EolLauncher(Map<String, Object> config, String execution_mode) {
		this.executionMode = execution_mode;
		this.c_module_name = (String) config.get("EOL_NAME");
		this.c_path = (String) config.get("EOL_CODE");
		this.inMM_name = (String) config.get("IN_MM_NAME");
		this.inMM_File = (String) config.get("IN_MM_FILE");
		this.type = (int) config.get("EOL_TYPE");
		if (type == EOLCandidate.SECONDARY_MODEL_UPDATE) {
			this.outMM_name = (String) config.get("OUT_MM_NAME");
			this.outMM_File = (String) config.get("OUT_MM_FILE");
		}
		inModels = "inModels" + File.separatorChar + c_module_name;
		expectedOutModels = "expectedModels" + File.separatorChar + c_module_name;
		mutants_path = "generatedMutations" + File.separatorChar + c_module_name;

	}

	@Override
	public void run() {
		if (executionMode == null)
			throw new IllegalArgumentException("Unknown execution mode.");
		try {
			if (executionMode == ORIGINAL_EXECUTION)
				originalExecutionMode();
			if (executionMode == MUTATION_EXECUTION)
				mutationExecutionMode();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void originalExecutionMode() throws Exception {

		EolModule module = new EolModule();
		module.parse(new File(c_path).getAbsoluteFile());
		if (module.getParseProblems().size() > 0) {
			System.err.println("Unable to parse file: " + c_path);
			System.err.println(module.getParseProblems().toString());
			return;
		}

		// load input models
		File inputs = new File(inModels);
		File outputs = new File(expectedOutModels);
		outputs.mkdirs();

		if (inputs.isDirectory() && inputs.listFiles().length >= 0) {
			for (File file : inputs.listFiles()) {
				if (file.getName().endsWith(".xmi")) {

					// three types of output
					// 1) main input model updated
					// 2) secondary input model updated
					// 3) console output

					IModel in_model = null;
					IModel out_model = null;
					String output_path = null;
					if (type == EOLCandidate.MAIN_MODEL_UPDATE) {
						output_path = outputs.getAbsolutePath() + File.separatorChar + file.getName() + "_output.xmi";
						Files.copy(file, new File(output_path));
						in_model = newEmfModel("M", output_path, inMM_File, true, true);
					} else if (type == EOLCandidate.SECONDARY_MODEL_UPDATE) {
						output_path = outputs.getAbsolutePath() + File.separatorChar + file.getName() + "_output.xmi";
						out_model = newEmfModel("M", output_path, outMM_File, true, true);
					} else {
						in_model = newEmfModel("M", file.getAbsolutePath(), inMM_File, true, false);
						output_path = outputs.getAbsolutePath() + File.separatorChar + file.getName() + "_output.text";
					}
					// add models
					module.getContext().getModelRepository().addModel(in_model);
					if (out_model != null)
						module.getContext().getModelRepository().addModel(out_model);
					if (type == EOLCandidate.CONSOLE_OUTPUT)
						module.getContext().setOutputStream(new PrintStream(new FileOutputStream(output_path)));
					module.execute();
					module.getContext().getModelRepository().dispose();
					if (type == EOLCandidate.CONSOLE_OUTPUT)
						module.getContext().getOutputStream().close();
				}
			}
		}
	}

	private void mutationExecutionMode() throws Exception {

		File mutations_execution = new File("mutationsExecution" + File.separatorChar + c_module_name);
		mutations_execution.mkdirs();

		File temp_folder = new File("temp");
		temp_folder.mkdirs();
		temp_folder.deleteOnExit();

		EMFCompare comparator = EMFCompare.builder().build();
		Logger.getRootLogger().setLevel(Level.OFF);

		List<OperatorEntry> operators_stats = new ArrayList<OperatorEntry>();

		EolPackage.eINSTANCE.eClass();
		ResourceSet rs = new ResourceSetImpl();
		Resource.Factory.Registry.INSTANCE.getExtensionToFactoryMap().put("xmi", new XMIResourceFactoryImpl());
		Resource mutant_resource = null;

		// locate mutation folder
		File mutants = new File(mutants_path + File.separatorChar);

		for (File mutant_model : mutants.listFiles()) {
			if (mutant_model.getName().endsWith(".xmi")) {

				OperatorEntry current_operator = getOperatorEntryByMutant(operators_stats, mutant_model.getName());
				if (current_operator == null) {
					current_operator = new OperatorEntry(mutant_model);
					operators_stats.add(current_operator);
				}
				current_operator.incrementProcessed();

				// covert the EOL model back to code
				rs.getResources().clear();
				mutant_resource = getResource(rs, mutant_model);
				mutant_resource.load(Collections.EMPTY_MAP);
				EObject root = mutant_resource.getContents().get(0);
				EolPrinter printer = new EolPrinter();
				printer.run((EOLElement) root);
				String mutant_code_path = temp_folder.getAbsolutePath() + File.separatorChar + c_module_name + ".eol";
				File mutant_code = new File(mutant_code_path);
				FileWriter fw = new FileWriter(mutant_code);
				fw.write(printer.getPrintedProgram());
				fw.close();

				// parse the EOL mutatn code
				EolModule eol = new EolModule();
				eol.parse(mutant_code.getAbsoluteFile());
				if (eol.getParseProblems().size() > 0) {
					System.err.println("Unable to parse file: " + c_path);
					System.err.println(eol.getParseProblems().toString());
					current_operator.incrementInvalid();
					continue;
				}

				eol.getContext().getModelRepository().getModels().clear();

				// load input models
				File inputs = new File(inModels);

				int total_killed = 0;

				if (inputs.isDirectory() && inputs.listFiles().length > 0) {
					for (File input_test : inputs.listFiles()) {
						if (input_test.getName().endsWith(".xmi")) {

							boolean killed = false;
							// three types of output
							// 1) main input model updated
							// 2) secondary input model updated
							// 3) console output

							rs.getResources().clear();
							IModel in_model = null;
							IModel out_model = null;
							String actual_out_path = null;
							if (type == EOLCandidate.MAIN_MODEL_UPDATE) {
								actual_out_path = temp_folder.getAbsolutePath() + File.separatorChar + input_test.getName() + "_output.xmi";
								Files.copy(input_test, new File(actual_out_path));
								in_model = newEmfModel("M", actual_out_path, inMM_File, true, true);
							} else if (type == EOLCandidate.SECONDARY_MODEL_UPDATE) {
								actual_out_path = temp_folder.getAbsolutePath() + File.separatorChar + input_test.getName() + "_output.xmi";
								out_model = newEmfModel("M", actual_out_path, outMM_File, true, true);
							} else {
								in_model = newEmfModel("M", input_test.getAbsolutePath(), inMM_File, true, false);
								actual_out_path = temp_folder.getAbsolutePath() + File.separatorChar + input_test.getName() + "_output";
							}

							// add models
							eol.getContext().getModelRepository().addModel(in_model);
							if (out_model != null)
								eol.getContext().getModelRepository().addModel(out_model);
							if (type == EOLCandidate.CONSOLE_OUTPUT)
								eol.getContext().setOutputStream(new PrintStream(new FileOutputStream(actual_out_path)));
							try {
								eol.execute();
							} catch (EolRuntimeException | StackOverflowError e1) {
								// runtime killed mutation
								killed = true;
							}
							eol.getContext().getModelRepository().dispose();
							if (type == EOLCandidate.CONSOLE_OUTPUT)
								eol.getContext().getOutputStream().close();

							String expected_path = expectedOutModels + File.separatorChar + input_test.getName() + "output";
							if (!killed) {

								// load expected model that correspond to the input test model and compare with actual model
								if (actual_out_path.endsWith(".xmi")) {
									// model comparison: compare actual and expected output models

									// 1) obtain expected model
									expected_path += ".xmi";
									Resource expected = getResource(rs, new File(expected_path));
									expected.load(Collections.EMPTY_MAP);

									// 2) obtain actual model
									Resource actual = getResource(rs, new File(actual_out_path));
									actual.load(Collections.EMPTY_MAP);

									// now compare
									IComparisonScope scope = new DefaultComparisonScope(expected, actual, null);
									Comparison result = comparator.compare(scope);

									if (result != null)
										killed = result.getDifferences().size() >= 1;
								}

								if (actual_out_path.endsWith(".text")) {
									// text comparison

									expected_path += ".text";
									File expected = new File(expected_path);
									File actual = new File(actual_out_path);
									boolean equal = FileUtils.contentEquals(expected, actual);
									if (!equal)
										killed = true;
								}
							}
							if (killed)
								total_killed++;
						}
					} // end executing the mutant against test inputs
				}

				if (total_killed == 0) {
					// not killed by any test input: live or equivalent
					getOperatorEntryByMutant(operators_stats, mutant_model.getName()).addNotKilled(mutant_model.getName());
				} else {
					getOperatorEntryByMutant(operators_stats, mutant_model.getName()).incrementKilledMutants();
				}

				// clear execution temporary files
				if (temp_folder != null && temp_folder.isDirectory()) {
					for (File f : temp_folder.listFiles())
						f.delete();
				}
			}
		}

		// finally print the execution report
		try {
			print_summary(operators_stats, mutations_execution.getAbsolutePath());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private IModel newEmfModel(String name, String m, String mm, boolean read, boolean store) throws EolModelLoadingException, URISyntaxException {
		IModel emfModel = new EmfModel();
		StringProperties properties = new StringProperties();
		properties.put(EmfModel.PROPERTY_NAME, name);
		properties.put(EmfModel.PROPERTY_FILE_BASED_METAMODEL_URI, new URI(mm).toString());
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

		String live_string = "";

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
			for (String live : operator.getAllNotKilled())
				live_string += "\n\t\t\\-->" + live;
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

		try (FileWriter log = new FileWriter(path + File.separatorChar + c_module_name + ".txt")) {

			log.write("\t\\--> Processed mutants-> " + sum_processed + "\n");
			log.write("\t\\--> Invalid mutants-> " + sum_invalid + "\n");
			log.write("\t\\--> Killed mutants-> " + sum_killed + "\n");
			log.write("\t\\--> Not killed mutants-> " + sum_not_killed);
			log.write(live_string + "\n");
			log.flush();
			log.close();
		}

		try (FileWriter file = new FileWriter(path + File.separatorChar + c_module_name + ".csv")) {
			file.write(entry.toString());
			file.flush();
			file.close();
		}
	}
}