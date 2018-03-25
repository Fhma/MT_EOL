package org.eclipse.epsilon.haetae.eol.ast2eol.workbench;

import java.io.File;
import java.net.URL;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;
import org.eclipse.epsilon.eol.EolModule;
import org.eclipse.epsilon.eol.ast2eol.context.Ast2EolContext;
import org.eclipse.epsilon.eol.metamodel.EOLElement;

public class ParseMain {

	public static void main(String[] args) throws Exception {
		new ParseMain().run();
	}

	public void run() throws Exception {

		String code_name = "Test";
		URL url = getClass().getResource(code_name + ".eol");

		EolModule eolModule = new EolModule();
		try {
			eolModule.parse(new File(url.getPath()));
		} catch (Exception e) {
			e.printStackTrace();
		}

		Ast2EolContext context = new Ast2EolContext();
		EOLElement domElements = context.getEolElementCreatorFactory().createEOLElement(eolModule.getAst(), null,
				context);

		System.err.println(context.getEolElementCreatorFactory().isProperlyContained()
				? "DomElements are property contained" : "DomElements are NOT properly contained");

		ResourceSet resourceSet = new ResourceSetImpl();
		resourceSet.getResourceFactoryRegistry().getExtensionToFactoryMap().put("*", new XMIResourceFactoryImpl());
		Resource resource = resourceSet
				.createResource(URI.createFileURI(new File(code_name + ".xmi").getAbsolutePath()));
		resource.getContents().add(domElements);
		resource.save(null);
	}
}
