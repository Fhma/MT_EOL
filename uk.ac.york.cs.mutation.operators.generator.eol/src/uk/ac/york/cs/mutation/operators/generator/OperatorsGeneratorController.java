package uk.ac.york.cs.mutation.operators.generator;

import java.io.File;
import java.net.URI;

import org.eclipse.epsilon.common.util.StringProperties;
import org.eclipse.epsilon.emc.emf.EmfModel;
import org.eclipse.epsilon.eol.models.IModel;
import org.eclipse.epsilon.etl.EtlModule;

public class OperatorsGeneratorController {

	private static String mutationMM = OperatorsGeneratorController.class
			.getResource("resources/MutationOperatorMM.ecore").getPath();;

	private static IModel mutationM = null;

	public static void main(String args[]) {
		if (OperatorsGeneratorController.generateOperatorsMutations()) {
			OperatorsGeneratorController.generateHtML();
		}
	}

	private static boolean generateOperatorsMutations() {
		String sourceMMPath = OperatorsGeneratorController.class.getResource("resources/Ecore.ecore").getPath();
		String etl_script = OperatorsGeneratorController.class.getResource("resources/etl_script.etl").getPath();

		File modelFile = new File("./metamodel/EOL.ecore");
		EtlModule module;

		File outFolder = new File("./models");
		outFolder.mkdir();
		String targetModelName = outFolder.toURI().toString() + "/" + modelFile.getName() + "_mutation_operators.xmi";

		try {
			IModel atl_mm = createEmfModel("Ecore", modelFile.getAbsolutePath(), sourceMMPath, true, false);
			mutationM = createEmfModel("MutationOperatorMM", targetModelName, mutationMM, false, true);

			module = new EtlModule();
			module.getContext().getModelRepository().addModel(atl_mm);
			module.getContext().getModelRepository().addModel(mutationM);

			module.parse(new File(etl_script));
			if (module.getParseProblems().size() >= 1) {
				System.out.println("Parsing Problems: " + module.getParseProblems().toString());
				return false;
			}
			module.execute();
			module.getContext().getModelRepository().dispose();
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}

	private static boolean generateHtML() {
		return false;
	}

	private static EmfModel createEmfModel(String name, String modelPath, String metamodelPath, boolean readOnLoad,
			boolean disposal) throws Exception {
		EmfModel model = new EmfModel();
		StringProperties properties = new StringProperties();
		properties.put(EmfModel.PROPERTY_FILE_BASED_METAMODEL_URI, new URI(metamodelPath).toString());
		properties.put(EmfModel.PROPERTY_MODEL_URI, new URI(modelPath).toString());
		properties.put(EmfModel.PROPERTY_READONLOAD, readOnLoad);
		properties.put(EmfModel.PROPERTY_STOREONDISPOSAL, disposal);
		model.load(properties);
		model.setName(name);
		return model;
	}
}
