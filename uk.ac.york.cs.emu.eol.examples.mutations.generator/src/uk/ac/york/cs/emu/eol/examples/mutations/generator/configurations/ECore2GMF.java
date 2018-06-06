package uk.ac.york.cs.emu.eol.examples.mutations.generator.configurations;

import java.util.HashMap;
import java.util.Map;
import uk.ac.york.cs.emu.eol.examples.mutations.generator.candidates.EOLCandidate;

public class ECore2GMF {

	static private Map<String, Object> properties;

	public static Map<String, Object> properties() {
		properties = new HashMap<String, Object>();

		properties.put(Configuration.EOL_NAME, "ECore2GMF");
		properties.put(Configuration.EOL_CODE, EOLCandidate.LOCATION + "ECore2GMF.eol");

		return properties;
	}

	private ECore2GMF() {
	}
}