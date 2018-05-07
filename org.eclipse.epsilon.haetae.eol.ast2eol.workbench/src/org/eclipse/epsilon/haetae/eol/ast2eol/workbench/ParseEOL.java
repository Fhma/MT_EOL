package org.eclipse.epsilon.haetae.eol.ast2eol.workbench;

import java.io.File;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;
import org.eclipse.epsilon.eol.EolModule;
import org.eclipse.epsilon.eol.ast2eol.context.Ast2EolContext;
import org.eclipse.epsilon.eol.metamodel.EOLElement;

public class ParseEOL {

	public static void main(String[] args) throws Exception {
		new ParseEOL().run();
	}

	public void run() throws Exception {

		// read all EOL code files in "EOL_Programs" and parse the generated AST to XMI models
		File model_folder = new File("EOL_Programs" + File.separatorChar);

		for (File eol_code : model_folder.listFiles()) {
			if (!eol_code.getName().endsWith(".eol"))
				continue;
			System.out.println("Parsing: "+eol_code.getName());
			EolModule eolModule = new EolModule();
			try {
				eolModule.parse(eol_code.getAbsoluteFile());
			} catch (Exception e) {
				e.printStackTrace();
			}

			Ast2EolContext context = new Ast2EolContext(eolModule);
			EOLElement domElements = context.getEolElementCreatorFactory().createEOLElement(eolModule.getAst(), null, context);
			
			ResourceSet rs = new ResourceSetImpl();
			rs.getResourceFactoryRegistry().getExtensionToFactoryMap().put("*", new XMIResourceFactoryImpl());
			Resource resource = rs.createResource(URI.createFileURI(new File(eol_code.getAbsolutePath() + ".xmi").getAbsolutePath()));
			resource.getContents().add(domElements);
			resource.save(null);
		}
	}
}
