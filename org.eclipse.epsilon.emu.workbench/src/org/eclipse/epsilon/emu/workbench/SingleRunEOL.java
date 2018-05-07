/**
 * 
 * @author Faisal Alhwikem
 * @version 1.0
 * @since March, 2017
 */
package org.eclipse.epsilon.emu.workbench;

import org.eclipse.epsilon.eol.exceptions.models.EolModelLoadingException;

import org.eclipse.epsilon.eol.models.IModel;
import org.eclipse.epsilon.eol.models.IRelativePathResolver;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.eclipse.epsilon.common.util.StringProperties;
import org.eclipse.epsilon.emc.emf.EmfModel;
import org.eclipse.epsilon.emc.mutant.IMutant;
import org.eclipse.epsilon.emc.mutant.emf.EmfMutant;
import org.eclipse.epsilon.emu.EmuModule;

public class SingleRunEOL {
	public static void main(String[] args) {
		new SingleRunEOL().run();
	}

	public void run() {

		String sourceFile = "EMU_script/inputEOL.emu";

		String modelString = SingleRunEOL.class.getResource("resources/ShortestPath.xmi").getPath();
		String metamodel = SingleRunEOL.class.getResource("resources/Eol.ecore").getPath();

		File output = new File("EMU_script/input_mutants");

		if (output.exists() && output.isDirectory()) {
			File[] files = output.listFiles();
			for (File f : files)
				f.delete();
			output.delete();
		}

		EmuModule module = new EmuModule();

		try {
			module.parse(new File(sourceFile));
			if (module.getParseProblems().size() >= 1) {
				System.out.println("Parsing Problems: " + module.getParseProblems().toString());
				return;
			}
			IModel model = createEmfModel("Model", modelString, metamodel, true, false);
			module.getContext().getModelRepository().addModel(model);
			module.execute();
			module.getContext().getModelRepository().dispose();
			toJson(module);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private static IModel createEmfModel(String name, String model, String metamodel, boolean readOnLoad,
			boolean storeOnDisposal) throws EolModelLoadingException, URISyntaxException {
		IMutant emfModel = new EmfMutant();
		StringProperties properties = new StringProperties();
		properties.put(EmfModel.PROPERTY_NAME, name);
		properties.put(EmfModel.PROPERTY_FILE_BASED_METAMODEL_URI, new URI(metamodel).toString());
		properties.put(EmfModel.PROPERTY_MODEL_URI, new URI(model).toString());
		properties.put(EmfModel.PROPERTY_READONLOAD, readOnLoad + "");
		properties.put(EmfModel.PROPERTY_STOREONDISPOSAL, storeOnDisposal + "");
		emfModel.load(properties, (IRelativePathResolver) null);
		return emfModel;
	}

	private static void toJson(EmuModule module) throws IOException {
		if (module.getOperatorsMatrix().getContent() == null)
			return;
		JSONObject json = new JSONObject();
		Iterator<Map.Entry<String, List<String>>> it = module.getOperatorsMatrix().getContent().entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry<String, List<String>> pair = it.next();
			List<String> values = pair.getValue();
			JSONArray list = new JSONArray();
			list.put(values);
			json.put(pair.getKey(), list);
		}

		try (FileWriter file = new FileWriter(
				module.getMutantsDir() + "/" + module.getMutantsDir().getName() + "_summary.json")) {
			file.write(json.toString(4));
		}
	}
}
