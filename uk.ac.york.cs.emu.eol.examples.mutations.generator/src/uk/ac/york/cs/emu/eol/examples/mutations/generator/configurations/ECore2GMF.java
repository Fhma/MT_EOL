package uk.ac.york.cs.emu.eol.examples.mutations.generator.configurations;

import java.util.HashMap;
import java.util.Map;
import uk.ac.york.cs.emu.eol.examples.mutations.generator.candidates.EOLCandidate;
import uk.ac.york.cs.emu.eol.examples.mutations.generator.metamodels.EcoreModel;

public class ECore2GMF {

	static private Map<String, Object> properties;

	public static Map<String, Object> properties() {
		properties = new HashMap<String, Object>();

		// meta-variables
		String module_name = "ECore2GMF";
		String code_path = EOLCandidate.class.getResource(module_name + ".eol").getPath();
		int type = EOLCandidate.MODEL_CREATE_TYPE;

		// input/output metamodels
		String inMM_name = null;
		String inMM_File = null;// EcoreModel.class.getResource("Graph.ecore").getPath();
		String outMM_name = null;
		String outMM_File = null;

		properties.put("EOL_NAME", module_name);
		properties.put("EOL_CODE", code_path);
		properties.put("PROGRAM_TYPE", type);
		properties.put("IN_MM_NAME", inMM_name);
		properties.put("IN_MM_FILE", inMM_File);
		properties.put("OUT_MM_NAME", outMM_name);
		properties.put("OUT_MM_FILE", outMM_File);
		return properties;
	}

	private ECore2GMF() {
	}
}