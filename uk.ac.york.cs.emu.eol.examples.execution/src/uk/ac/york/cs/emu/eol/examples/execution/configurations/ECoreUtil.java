package uk.ac.york.cs.emu.eol.examples.execution.configurations;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.epsilon.emc.emf.EmfMetaModel;
import uk.ac.york.cs.emu.eol.examples.execution.candidates.EOLCandidate;
import uk.ac.york.cs.emu.eol.examples.execution.metamodels.EcoreModel;

public class  ECoreUtil {

	static private Map<String, Object> properties;

	public static Map<String, Object> properties() {
		properties = new HashMap<String, Object>();

		properties.put(Configuration.EOL_NAME, "ECoreUtil");
		properties.put(Configuration.EOL_CODE, EOLCandidate.class.getResource("ECoreUtil.eol").getPath());
		// must be one imported module
		properties.put(Configuration.IMPORTED_BY, EOLCandidate.class.getResource("ECore2GMF.eol").getPath());
		properties.put(Configuration.IMPORTING, EOLCandidate.class.getResource("ECoreUtil.eol").getPath() + "," + EOLCandidate.class.getResource("Formatting.eol").getPath());
		properties.put(Configuration.PROGRAM_TYPE, EOLCandidate.MODEL_CREATE_TYPE);
		properties.put(Configuration.MAX_EXE, 5000);

		// add metamodels
		EmfMetaModel emf_metamodels[] = { new EmfMetaModel("ECore", "http://www.eclipse.org/emf/2002/Ecore"),
				new EmfMetaModel("GmfGraph", "http://www.eclipse.org/gmf/2006/GraphicalDefinition"),
				new EmfMetaModel("GmfTool", "http://www.eclipse.org/gmf/2005/ToolDefinition"),
				new EmfMetaModel("GmfMap", "http://www.eclipse.org/gmf/2008/mappings") };
		
		properties.put(Configuration.MM_METAMODELS, emf_metamodels);

		properties.put(Configuration.MM_PATHS, EcoreModel.class.getResource("gmf_all.ecore").getPath());

		return properties;
	}
}