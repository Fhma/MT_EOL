package uk.ac.york.cs.emu.eol.examples.executor.configurations;

import java.util.HashMap;
import java.util.Map;
import org.eclipse.epsilon.emc.emf.EmfMetaModel;

import uk.ac.york.cs.emu.eol.examples.executor.candidates.EOLCandidate;
import uk.ac.york.cs.emu.eol.examples.executor.metamodels.EcoreModel;

public class Formatting {

	static private Map<String, Object> properties;

	public static Map<String, Object> properties() {
		properties = new HashMap<String, Object>();

		properties.put(Configuration.EOL_NAME, "Formatting");
		properties.put(Configuration.EOL_CODE, EOLCandidate.LOCATION + "Formatting.eol");
		properties.put(Configuration.IMPORTED_BY, EOLCandidate.LOCATION + "ECore2GMF.eol");
		properties.put(Configuration.IMPORTING, EOLCandidate.LOCATION + "ECoreUtil.eol" + "," + EOLCandidate.LOCATION + "Formatting.eol");
		properties.put(Configuration.PROGRAM_TYPE, EOLCandidate.MODEL_CREATE_MULTI_TYPE);
		properties.put(Configuration.MAX_EXE, 5000);
		properties.put(Configuration.FLAG_DELEGATES, false);

		// add metamodels
		EmfMetaModel emf_metamodels[] = { new EmfMetaModel("ECore", "http://www.eclipse.org/emf/2002/Ecore"), new EmfMetaModel("GmfGraph", "http://www.eclipse.org/gmf/2006/GraphicalDefinition"),
				new EmfMetaModel("GmfTool", "http://www.eclipse.org/gmf/2005/ToolDefinition"), new EmfMetaModel("GmfMap", "http://www.eclipse.org/gmf/2008/mappings") };

		properties.put(Configuration.MM_METAMODELS, emf_metamodels);

		// String metamodels_paths[] = { EcoreModel.LOCATION + "GmfGraph.ecore", EcoreModel.LOCATION + "GmfTool.ecore", EcoreModel.LOCATION + "GmfMap.ecore" };

		String metamodels_paths[] = { EcoreModel.LOCATION + "gmf_all.ecore" };

		properties.put(Configuration.MM_PATHS, metamodels_paths);

		return properties;
	}
}