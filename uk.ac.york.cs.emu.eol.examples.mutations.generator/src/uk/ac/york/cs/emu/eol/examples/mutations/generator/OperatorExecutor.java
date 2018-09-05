package uk.ac.york.cs.emu.eol.examples.mutations.generator;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
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
    public static final String REPORT_DIR = "report" + File.separatorChar;
    public static final String OPERATORS_DIR = "operators" + File.separatorChar;

    private String eol_name = null;
    private String eol_code = null;
    private PrintStream logger = null;
    private File report_dir = null;
    private String operators_source = null;

    public OperatorExecutor(Map<String, String> config, String source_dir) throws Exception {
	eol_name = config.get(Configuration.EOL_NAME);
	eol_code = config.get(Configuration.EOL_CODE);
	operators_source = source_dir;
	report_dir = new File(REPORT_DIR + eol_name);
	report_dir.mkdirs();
	logger = new PrintStream(new File(report_dir.getPath() + File.separatorChar + eol_name + ".log"));
    }

    @Override
    public void run() {
	long mins = System.currentTimeMillis();
	try {
	    logger.println("Operators Execution: " + eol_name);
	    mutateEolModule();
	} catch (Exception e) {
	    logger.println("Exception: " + e.getMessage());
	} finally {
	    int time = (int) ((System.currentTimeMillis() - mins) / 1000) / 60;
	    logger.println(String.format("Done Execution...(%d mins)", time));
	    logger.close();
	}
    }

    private void mutateEolModule() throws Exception {

	File mutations_dir = new File(MUTATIONS_DIR + eol_name);
	mutations_dir.mkdirs();

	String model_path = null;
	model_path = parseEolCodeToModel();

	File emu_programs_dir = new File(OPERATORS_DIR + operators_source + File.separatorChar);
	final File emu_programs[] = emu_programs_dir.listFiles();

	OMatrix operators_matrix = new OMatrix(mutations_dir.getPath());

	EmuModule module = null;
	IModel emfModel = null;
	for (File entry : emu_programs) {
	    if (!entry.isDirectory() && entry.getName().endsWith(".emu")) {

		logger.println("\tExecuting mutation operator: " + entry.getName());
		module = new EmuModule();
		module.setMutants_dir(mutations_dir);
		module.setOperatorsMatrix(operators_matrix);
		module.parse(entry.getAbsoluteFile());
		if (module.getParseProblems().size() >= 1) {
		    throw new Exception("Unable to parse file: " + entry.getName() + "\n" + module.getParseProblems().toString() + "\n");
		}
		emfModel = createEmfModel("Eol_" + eol_name, model_path, METAMODEL_DIR + File.separatorChar + "EOL.ecore", true, false);
		module.getContext().getModelRepository().addModel(emfModel);
		module.setRepeatWhileMatches(false);
		module.execute();
		module.getContext().getModelRepository().dispose();
	    }
	} // end of executing mutation operators
	print_summary(operators_matrix);
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

    private void print_summary(OMatrix mat) throws IOException {
	if (mat == null)
	    return;
	int valid_mutants;
	int invalid_mutants;
	Iterator<Map.Entry<String, List<String>>> it = mat.getContent().entrySet().iterator();
	FileWriter file = new FileWriter(report_dir.getPath() + File.separatorChar + eol_name + ".csv");

	file.write("Mutation Operator,");
	file.write("Valid,");
	file.write("Invalid\n");
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

	    file.write(pair.getKey().toString() + ",");
	    file.write(String.format("%d", valid_mutants) + ",");
	    file.write(String.format("%d", invalid_mutants) + "\n");
	}

	file.write("Total Mutants,");
	file.write(totol_valid + ",");
	file.write(totol_invalid + "\n");
	file.close();
    }

    private Resource getResource(ResourceSet rs, String path) {
	return rs.createResource(org.eclipse.emf.common.util.URI.createFileURI(new File(path).getAbsolutePath()));
    }
}