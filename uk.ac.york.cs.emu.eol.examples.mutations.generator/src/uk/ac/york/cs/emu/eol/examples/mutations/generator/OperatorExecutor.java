package uk.ac.york.cs.emu.eol.examples.mutations.generator;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URI;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.eclipse.epsilon.eol.models.IModel;
import org.eclipse.epsilon.emu.EmuModule;
import org.eclipse.epsilon.emu.execute.EmuPatternMatcher;
import org.eclipse.epsilon.emu.mutation.matrix.OMatrix;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;
import org.eclipse.epsilon.common.util.StringProperties;
import org.eclipse.epsilon.emc.emf.EmfModel;
import org.eclipse.epsilon.emc.mutant.IMutant;
import org.eclipse.epsilon.emc.mutant.emf.EmfMutant;
import org.eclipse.epsilon.eol.models.IRelativePathResolver;
import uk.ac.york.cs.emu.eol.examples.mutations.generator.configurations.Configuration;
import org.eclipse.epsilon.eol.EolModule;
import org.eclipse.epsilon.eol.ast2eol.context.Ast2EolContext;
import org.eclipse.epsilon.eol.exceptions.models.EolModelLoadingException;
import org.eclipse.epsilon.eol.metamodel.EOLElement;

import java.net.URISyntaxException;

public class OperatorExecutor implements Runnable {

    public static final String MUTATIONS_DIR = "mutations" + File.separatorChar;
    public static final String METAMODEL_DIR = "metamodels" + File.separatorChar;
    public static final String MUTATIONS_SUMMARY_DIR = "mutations_summary" + File.separatorChar;
    public static final String OPERATORS_DIR = "operators" + File.separatorChar;

    private String eol_name;
    private String eol_code;

    public OperatorExecutor(Map<String, String> config) {
	eol_name = config.get(Configuration.EOL_NAME);
	eol_code = config.get(Configuration.EOL_CODE);
    }

    @Override
    public void run() {

	File mutations_dir = new File(MUTATIONS_DIR + eol_name);
	mutations_dir.mkdirs();
	File log_file = new File(MUTATIONS_DIR + eol_name + ".log");
	FileWriter logger = null;
	try {
	    logger = new FileWriter(log_file);
	    mutateEolModule(mutations_dir, logger);
	    logger.close();
	} catch (Exception e) {
	    e.printStackTrace();
	    try {
		logger.close();
	    } catch (IOException e1) {
		e1.printStackTrace();
	    }
	}
    }

    private void mutateEolModule(File dir, FileWriter logger) throws Exception {

	String model_path = null;
	model_path = parseEolCodeToModel();

	// File emu_programs_dir = new File(OPERATORS_DIR + "selected" + File.separatorChar);
	File emu_programs_dir = new File(OPERATORS_DIR + "all_operators" + File.separatorChar);
	final File emu_programs[] = emu_programs_dir.listFiles();

	OMatrix operators_matrix = new OMatrix(dir.getPath());

	EmuModule module = null;
	IModel emfModel = null;
	for (File entry : emu_programs) {
	    if (!entry.isDirectory() && entry.getName().endsWith(".emu")) {

		module = new EmuModule();
		module.setMutants_dir(dir);
		module.setOperatorsMatrix(operators_matrix);
		module.parse(entry.getAbsoluteFile());
		if (module.getParseProblems().size() >= 1) {
		    logger.close();
		    throw new Exception("Unable to parse file: " + entry.getName() + "\n" + module.getParseProblems().toString() + "\n");
		}
		emfModel = createEmfModel("Eol_" + eol_name, model_path, METAMODEL_DIR + File.separatorChar + "EOL.ecore", true, false);
		module.getContext().getModelRepository().addModel(emfModel);
		module.setRepeatWhileMatches(false);
		module.execute();
		module.getContext().getModelRepository().dispose();
	    }
	} // end of executing mutation operators
	print_summary(operators_matrix, MUTATIONS_SUMMARY_DIR + eol_name + File.separatorChar);
    }

    private String parseEolCodeToModel() throws Exception {

	// parse the EOL code
	EolModule eol = new EolModule();

	eol.parse(new File(eol_code).getAbsoluteFile());

	if (eol.getParseProblems().size() > 0)
	    throw new Exception("Unable to parse file: " + eol_code);

	// get the EMF model of the code
	Ast2EolContext context = new Ast2EolContext(eol);
	EOLElement dom = context.getEolElementCreatorFactory().createEOLElement(eol.getAst(), null, context);

	File eolModels = new File("Eol_Models");
	eolModels.mkdirs();
	String model_path = eolModels.getAbsolutePath() + File.separatorChar + eol_name + ".xmi";

	ResourceSet rs = new ResourceSetImpl();

	rs.getResourceFactoryRegistry().getExtensionToFactoryMap().put("*", new XMIResourceFactoryImpl());
	Resource resource = getResource(rs, model_path);
	resource.getContents().add(dom);
	resource.save(null);

	return model_path;
    }

    private IModel createEmfModel(String name, String m, String mm, boolean r, boolean s) throws URISyntaxException, EolModelLoadingException {
	IMutant emfModel = new EmfMutant();
	StringProperties properties = new StringProperties();
	properties.put(EmfModel.PROPERTY_NAME, name);
	properties.put(EmfModel.PROPERTY_FILE_BASED_METAMODEL_URI, new URI(mm).toString());
	properties.put(EmfModel.PROPERTY_MODEL_URI, new URI(m).toString());
	properties.put(EmfModel.PROPERTY_READONLOAD, r + "");
	properties.put(EmfModel.PROPERTY_STOREONDISPOSAL, s + "");
	emfModel.load(properties, (IRelativePathResolver) null);

	return emfModel;
    }

    private void print_summary(OMatrix mat, String loc) throws IOException {
	if (mat == null)
	    return;
	int valid_mutants;
	int invalid_mutants;
	Iterator<Map.Entry<String, List<String>>> it = mat.getContent().entrySet().iterator();
	StringBuilder entry = new StringBuilder();

	entry.append("Mutation Operator");
	entry.append(',');
	entry.append("Valid");
	entry.append(',');
	entry.append("Invalid");
	entry.append('\n');
	int totol_valid = 0;
	int totol_invalid = 0;
	while (it.hasNext()) {
	    Map.Entry<String, List<String>> pair = it.next();
	    List<String> values = pair.getValue();
	    valid_mutants = 0;
	    invalid_mutants = 0;
	    for (int i = 0; i < values.size(); i++) {
		if (values.get(i).equals(EmuPatternMatcher.INVALID_MUTATION))
		    invalid_mutants++;
		else
		    valid_mutants++;
	    }
	    totol_valid += valid_mutants;
	    totol_invalid += invalid_mutants;

	    entry.append(pair.getKey().toString());
	    entry.append(',');
	    entry.append(String.format("%d", valid_mutants));
	    entry.append(',');
	    entry.append(String.format("%d", invalid_mutants));
	    entry.append('\n');
	}

	entry.append("Total Mutants");
	entry.append(',');
	entry.append(totol_valid);
	entry.append(',');
	entry.append(totol_invalid);

	File folder = new File(loc);
	folder.mkdirs();
	FileWriter file = new FileWriter(loc + eol_name + "_summary.csv");
	file.write(entry.toString());
	file.flush();
	file.close();
    }

    private Resource getResource(ResourceSet rs, String path) {
	return rs.createResource(org.eclipse.emf.common.util.URI.createFileURI(new File(path).getAbsolutePath()));
    }
}