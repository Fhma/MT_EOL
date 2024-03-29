package uk.ac.york.cs.emu.eol.examples.mutations.executor.configurations;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.epsilon.emc.emf.EmfMetaModel;

import uk.ac.york.cs.emu.eol.examples.mutations.executor.candidates.EOLCandidate;
import uk.ac.york.cs.emu.eol.examples.mutations.executor.metamodels.EcoreModel;

public class ShortestPath {

    static private Map<String, Object> properties;

    public static Map<String, Object> properties() {
	properties = new HashMap<String, Object>();

	properties.put(Configuration.EOL_NAME, "ShortestPath");
	properties.put(Configuration.EOL_CODE, EOLCandidate.LOCATION + "ShortestPath.eol");
	properties.put(Configuration.IMPORTED_BY, null);
	properties.put(Configuration.IMPORTING, null);
	properties.put(Configuration.PROGRAM_TYPE, EOLCandidate.CONSOLE_TYPE);
	properties.put("MAX_EXE", 2000);

	// add metamodels
	EmfMetaModel emf_metamodels[] = { new EmfMetaModel("DirectedGraph", "DirectedGraph") };

	properties.put(Configuration.MM_METAMODELS, emf_metamodels);

	// properties.put(Configuration.MM_PATHS, EcoreModel.class.getResource("DirectedGraph.ecore").getPath());
	properties.put(Configuration.MM_PATHS, EcoreModel.LOCATION + "DirectedGraph.ecore");

	return properties;
    }

    private ShortestPath() {
    }
}