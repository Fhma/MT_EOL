package uk.ac.york.cs.emu.eol.examples.execution.configurations;

import java.util.HashMap;
import java.util.Map;

import uk.ac.york.cs.emu.eol.examples.execution.candidates.EOLCandidate;
import uk.ac.york.cs.emu.eol.examples.execution.candidates.metamodels.EcoreModel;

public class ShortestPath {

	static private Map<String, Object> properties;

	public static Map<String, Object> properties() {
		properties = new HashMap<String, Object>();

		// meta-variables
		String c_module_name = "ShortestPath";
		String c_path = EOLCandidate.class.getResource(c_module_name + ".eol").getPath();
		String m_path = EOLCandidate.class.getResource(c_module_name + ".xmi").getPath();
		int type = EOLCandidate.CONSOLE_OUTPUT; // console-output-type EOL code
		String inMM_name = "Graph";
		String inMM_File = EcoreModel.class.getResource("Graph.ecore").getPath();
		String outMM_name = null;
		String outMM_File = null;

		properties.put("EOL_NAME", c_module_name);
		properties.put("EOL_CODE", c_path);
		properties.put("EOL_MODEL", m_path);
		properties.put("EOL_TYPE", type);
		properties.put("IN_MM_NAME", inMM_name);
		properties.put("IN_MM_FILE", inMM_File);
		properties.put("OUT_MM_NAME", outMM_name);
		properties.put("OUT_MM_FILE", outMM_File);
		return properties;
	}

	private ShortestPath() {
	}
}