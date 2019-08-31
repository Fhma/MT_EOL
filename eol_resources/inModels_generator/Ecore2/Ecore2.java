package uk.ac.york.epsilon.emg.model.generator.resources.Ecore2;

import java.util.HashMap;
import java.util.Map;
import org.eclipse.epsilon.emc.emf.EmfMetaModel;
import uk.ac.york.epsilon.emg.model.generator.PropertyKey;

public class Ecore2 {
	static private Map<Short, Object> properties;

	public static Map<Short, Object> properties() {
		properties = new HashMap<Short, Object>();

		// meta-variables
		properties.put(PropertyKey.METAMODEL_ALIASE, "Ecore");
		properties.put(PropertyKey.MAIN_METAMODEL_URI, "http://www.eclipse.org/emf/2002/Ecore");
		properties.put(PropertyKey.MAIN_METAMODEL_FILE, null);
		
		EmfMetaModel h_metamodels[] = { new EmfMetaModel("ECore", "http://www.eclipse.org/emf/2002/Ecore") };
		String h_metamodels_path[] = null;
		String h_models_path[] = null;
		
		properties.put(PropertyKey.HELP_METAMODELS, h_metamodels);
		properties.put(PropertyKey.HELP_METAMODELS_PATH, h_metamodels_path);
		properties.put(PropertyKey.HELP_MODELS_PATH, h_models_path);
		properties.put(PropertyKey.EMG_FILE, Ecore2.class.getResource("generator.emg"));
		properties.put(PropertyKey.OUT_MODEL_FOLDER, "Ecore2");
		properties.put(PropertyKey.OUT_MODEL_FILE, "Ecore");

		// Model size (number of instances)
		properties.put(PropertyKey.MIN_MODEL_SIZE, 4);

		return properties;
	}
}
