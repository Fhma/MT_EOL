package uk.ac.york.cs.emu.eol.examples.mutations.generator.configurations;

import java.util.HashMap;
import java.util.Map;
import uk.ac.york.cs.emu.eol.examples.mutations.generator.candidates.EOLCandidate;

public class ShortestPath {

	static private Map<String, Object> properties;

	public static Map<String, Object> properties() {
		properties = new HashMap<String, Object>();

		properties.put("EOL_NAME", "ShortestPath");
		properties.put("EOL_CODE", EOLCandidate.LOCATION + "ShortestPath.eol");

		return properties;
	}

	private ShortestPath() {
	}
}