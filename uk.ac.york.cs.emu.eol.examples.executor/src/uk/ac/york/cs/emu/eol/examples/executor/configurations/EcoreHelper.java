package uk.ac.york.cs.emu.eol.examples.executor.configurations;

import java.util.HashMap;
import java.util.Map;
import org.eclipse.epsilon.emc.emf.EmfMetaModel;
import uk.ac.york.cs.emu.eol.examples.executor.candidates.EOLCandidate;

public class EcoreHelper {

    static private Map<String, Object> properties;

    public static Map<String, Object> properties() {
	properties = new HashMap<String, Object>();

	properties.put(Configuration.EOL_NAME, "EcoreHelper");
	properties.put(Configuration.EOL_CODE, EOLCandidate.LOCATION + "EcoreHelper.eol");
	properties.put(Configuration.IMPORTED_BY, EOLCandidate.LOCATION + "EcoreHelperInvoker.eol");
	properties.put(Configuration.IMPORTING, null);
	properties.put(Configuration.PROGRAM_TYPE, EOLCandidate.CONSOLE_TYPE);
	properties.put(Configuration.MAX_EXE, 5000);
	properties.put(Configuration.FLAG_DELEGATES, false);

	// add metamodels
	EmfMetaModel emf_metamodels[] = { new EmfMetaModel("Ecore", "http://www.eclipse.org/emf/2002/Ecore"),
		new EmfMetaModel("EpsilonExecutionTrace.ecore", "http://www.eclipse.epsilon.org/incremental/base/ExecutionTrace/1.0") };

	properties.put(Configuration.MM_METAMODELS, emf_metamodels);

	// String metamodels_paths[] = { EcoreModel.LOCATION + "EpsilonExecutionTrace.ecore" };
	String metamodels_paths[] = null;

	properties.put(Configuration.MM_PATHS, metamodels_paths);
	return properties;
    }
}