package uk.ac.york.cs.emu.eol.examples.executor.configurations;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.epsilon.emc.emf.EmfMetaModel;

import uk.ac.york.cs.emu.eol.examples.executor.candidates.EOLCandidate;
import uk.ac.york.cs.emu.eol.examples.executor.metamodels.EcoreModel;

public class ShortestPath {

	static private Map<String, Object> properties;

	public static Map<String, Object> properties() {
		properties = new HashMap<String, Object>();

		properties.put(Configuration.EOL_NAME, "ShortestPath");
		properties.put(Configuration.EOL_CODE, EOLCandidate.LOCATION + "ShortestPath.eol");
		properties.put(Configuration.IMPORTED_BY, null);
		properties.put(Configuration.IMPORTING, null);
		properties.put(Configuration.PROGRAM_TYPE, EOLCandidate.CONSOLE_TYPE);
		properties.put("MAX_EXE", 5000);
		properties.put(Configuration.FLAG_DELEGATES, false);

		// add metamodels
		EmfMetaModel emf_metamodels[] = { new EmfMetaModel("DirectedGraph", "DirectedGraph") };

		properties.put(Configuration.MM_METAMODELS, emf_metamodels);

		String metamodels_paths[] = { EcoreModel.LOCATION + "DirectedGraph.ecore" };

		properties.put(Configuration.MM_PATHS, metamodels_paths);

		return properties;
	}

	private ShortestPath() {
	}
}