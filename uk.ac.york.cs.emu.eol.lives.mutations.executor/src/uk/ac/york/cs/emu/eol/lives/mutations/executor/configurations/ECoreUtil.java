package uk.ac.york.cs.emu.eol.lives.mutations.executor.configurations;

import java.util.HashMap;
import java.util.Map;
import org.eclipse.epsilon.emc.emf.EmfMetaModel;
import uk.ac.york.cs.emu.eol.lives.mutations.executor.candidates.EOLCandidate;
import uk.ac.york.cs.emu.eol.lives.mutations.executor.metamodels.EcoreModel;

public class ECoreUtil {

    static private Map<String, Object> properties;

    public static Map<String, Object> properties() {
	properties = new HashMap<String, Object>();

	properties.put(Configuration.EOL_NAME, "ECoreUtil");
	properties.put(Configuration.EOL_CODE, "ECoreUtil.eol");
	properties.put(Configuration.IMPORTED_BY, "ECore2GMF.eol");
	properties.put(Configuration.IMPORTING, "ECoreUtil.eol,Formatting.eol");
	properties.put(Configuration.PROGRAM_TYPE, EOLCandidate.MODEL_CREATE_TYPE);
	properties.put(Configuration.MAX_EXE, 20000);

	// add metamodels
	EmfMetaModel emf_metamodels[] = { new EmfMetaModel("ECore", "http://www.eclipse.org/emf/2002/Ecore"), new EmfMetaModel("GmfGraph", "http://www.eclipse.org/gmf/2006/GraphicalDefinition"),
		new EmfMetaModel("GmfTool", "http://www.eclipse.org/gmf/2005/ToolDefinition"), new EmfMetaModel("GmfMap", "http://www.eclipse.org/gmf/2008/mappings") };

	properties.put(Configuration.MM_METAMODELS, emf_metamodels);

	properties.put(Configuration.MM_PATHS, EcoreModel.LOCATION + "gmf_all.ecore");

	return properties;
    }
}