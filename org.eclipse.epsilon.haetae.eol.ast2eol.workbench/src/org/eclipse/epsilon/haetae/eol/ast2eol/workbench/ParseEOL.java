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

		File model_folder = new File("EOL_Programs" + File.separatorChar);

		for (File model_file : model_folder.listFiles()) {

			EolModule eolModule = new EolModule();
			try {
				eolModule.parse(model_file.getAbsoluteFile());
			} catch (Exception e) {
				e.printStackTrace();
			}

			Ast2EolContext context = new Ast2EolContext();
			EOLElement domElements = context.getEolElementCreatorFactory().createEOLElement(eolModule.getAst(), null, context);

			if (!context.getEolElementCreatorFactory().isProperlyContained()) {
				System.err.println("DomElements are NOT properly contained");
				continue;
			}

			ResourceSet rs = new ResourceSetImpl();
			rs.getResourceFactoryRegistry().getExtensionToFactoryMap().put("*", new XMIResourceFactoryImpl());
			Resource resource = rs.createResource(URI.createFileURI(new File(model_file.getAbsolutePath() + ".xmi").getAbsolutePath()));
			resource.getContents().add(domElements);
			resource.save(null);
		}
	}
}
