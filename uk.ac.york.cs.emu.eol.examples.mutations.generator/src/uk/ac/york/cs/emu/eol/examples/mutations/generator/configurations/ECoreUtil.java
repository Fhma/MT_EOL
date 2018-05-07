package uk.ac.york.cs.emu.eol.examples.mutations.generator.configurations;

import java.util.HashMap;
import java.util.Map;
import uk.ac.york.cs.emu.eol.examples.mutations.generator.candidates.EOLCandidate;

public class ECoreUtil {

	static private Map<String, Object> properties;

	public static Map<String, Object> properties() {
		properties = new HashMap<String, Object>();

		properties.put(Configuration.EOL_NAME, "ECoreUtil");
		properties.put(Configuration.EOL_CODE, EOLCandidate.class.getResource("ECoreUtil.eol").getPath());

		return properties;
	}

	private ECoreUtil() {
	}
}