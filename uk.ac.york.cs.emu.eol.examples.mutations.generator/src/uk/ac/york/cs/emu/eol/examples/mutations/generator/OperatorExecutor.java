package uk.ac.york.cs.emu.eol.examples.mutations.generator;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URI;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.eclipse.epsilon.eol.models.IModel;
import org.eclipse.epsilon.emu.EmuModule;
import org.eclipse.epsilon.emu.execute.EmuPatternMatcher;
import org.eclipse.epsilon.emu.mutation.matrix.OMatrix;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;
import org.eclipse.epsilon.common.util.StringProperties;
import org.eclipse.epsilon.emc.emf.EmfModel;
import org.eclipse.epsilon.emc.mutant.IMutant;
import org.eclipse.epsilon.emc.mutant.emf.EmfMutant;
import org.eclipse.epsilon.eol.models.IRelativePathResolver;
import uk.ac.york.cs.emu.eol.examples.mutations.generator.metamodels.EcoreModel;
import org.eclipse.epsilon.eol.EolModule;
import org.eclipse.epsilon.eol.ast2eol.context.Ast2EolContext;
import org.eclipse.epsilon.eol.exceptions.models.EolModelLoadingException;
import org.eclipse.epsilon.eol.metamodel.EOLElement;

import java.net.URISyntaxException;

public class OperatorExecutor implements Runnable {

	private String eol_name;
	private String code_path;

	public OperatorExecutor(Map<String, String> config) {
		eol_name = config.get("EOL_NAME");
		code_path = config.get("EOL_CODE");
	}

	@Override
	public void run() {

		// covert the EOL code to EOL model
		EolModule eol = new EolModule();
		try {
			eol.parse(new File(code_path).getAbsoluteFile());
		} catch (Exception e) {
			e.printStackTrace();
			return;
		}

		// parse the code to emf model
		Ast2EolContext context = new Ast2EolContext();
		EOLElement dom = context.getEolElementCreatorFactory().createEOLElement(eol.getAst(), null, context);

		File eolModels = new File("Eol_Models");
		eolModels.mkdirs();
		// eolModels.deleteOnExit();
		String model_path = eolModels.getAbsolutePath() + File.separatorChar + eol_name + ".xmi";
		ResourceSet rs = new ResourceSetImpl();
		rs.getResourceFactoryRegistry().getExtensionToFactoryMap().put("*", new XMIResourceFactoryImpl());
		Resource resource = getResource(rs, model_path);
		resource.getContents().add(dom);

		try {
			resource.save(null);
		} catch (IOException e) {
			e.printStackTrace();
			return;
		}

		File mutations_dir = new File("generatedMutations" + File.separatorChar + eol_name);
		mutations_dir.mkdirs();

		EmuModule module = null;

		File emu_programs_dir = new File("operatorDefinitions" + File.separatorChar);
		final File emu_programs[] = emu_programs_dir.listFiles();

		OMatrix operators_matrix = new OMatrix(mutations_dir.getPath());

		for (File entry : emu_programs) {
			try {
				if (!entry.isDirectory() && entry.getName().endsWith(".emu")) {

					module = new EmuModule();
					module.setMutants_dir(mutations_dir);
					module.setOperatorsMatrix(operators_matrix);
					module.parse(entry.getAbsoluteFile());
					if (module.getParseProblems().size() >= 1) {
						System.err.println("\t- - - - - - - - - - - - - -");
						System.err.println("\t|---->Parsing Problems in emu program: " + entry);
						System.err.println(module.getParseProblems().toString());
						return;
					}

					IModel emfModel = createEmfModel("Eol_" + eol_name, model_path, EcoreModel.class.getResource("EOL.ecore").getPath(), true, false);
					module.getContext().getModelRepository().addModel(emfModel);
					module.setRepeatWhileMatches(false);
					module.execute();
					module.getContext().getModelRepository().dispose();
				}
			} catch (Exception e) {
				e.printStackTrace();
				return;
			}
		} // end of executing operators

		try {
			// print_summary(operators_matrix, "generatedMutations_summary" + File.separatorChar + eol_name + File.separatorChar);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private IModel createEmfModel(String name, String model, String metamodel, boolean readOnLoad, boolean storeOnDisposal) {
		IMutant emfModel = new EmfMutant();
		StringProperties properties = new StringProperties();
		properties.put(EmfModel.PROPERTY_NAME, name);
		try {
			properties.put(EmfModel.PROPERTY_FILE_BASED_METAMODEL_URI, new URI(metamodel).toString());
			properties.put(EmfModel.PROPERTY_MODEL_URI, new URI(model).toString());
			properties.put(EmfModel.PROPERTY_READONLOAD, readOnLoad + "");
			properties.put(EmfModel.PROPERTY_STOREONDISPOSAL, storeOnDisposal + "");
			emfModel.load(properties, (IRelativePathResolver) null);
		} catch (URISyntaxException | EolModelLoadingException e) {
			e.printStackTrace();
		}
		return emfModel;
	}

	private void print_summary(OMatrix matrix, String location) throws IOException {
		if (matrix == null)
			return;
		int valid_mutants;
		int invalid_mutants;
		Iterator<Map.Entry<String, List<String>>> it = matrix.getContent().entrySet().iterator();
		StringBuilder entry = new StringBuilder();

		entry.append("Mutation Operator");
		entry.append(',');
		entry.append("Valid");
		entry.append(',');
		entry.append("Invalid");
		entry.append('\n');
		int totol_valid = 0;
		int totol_invalid = 0;
		while (it.hasNext()) {
			Map.Entry<String, List<String>> pair = it.next();
			List<String> values = pair.getValue();
			valid_mutants = 0;
			invalid_mutants = 0;
			for (int i = 0; i < values.size(); i++) {
				if (values.get(i).equals(EmuPatternMatcher.INVALID_MUTATION))
					invalid_mutants++;
				else
					valid_mutants++;
			}
			totol_valid += valid_mutants;
			totol_invalid += invalid_mutants;

			entry.append(pair.getKey().toString());
			entry.append(',');
			entry.append(String.format("%d", valid_mutants));
			entry.append(',');
			entry.append(String.format("%d", invalid_mutants));
			entry.append('\n');
		}

		entry.append("Total Mutants");
		entry.append(',');
		entry.append(totol_valid);
		entry.append(',');
		entry.append(totol_invalid);

		File folder = new File(location);
		folder.mkdirs();
		try (FileWriter file = new FileWriter(location + eol_name + "_summary.csv")) {
			file.write(entry.toString());
			file.flush();
			file.close();
		}
	}

	private Resource getResource(ResourceSet rs, String path) {
		return rs.createResource(org.eclipse.emf.common.util.URI.createFileURI(new File(path).getAbsolutePath()));
	}
}