package org.eclipse.epsilon.haetae.eol.metamodel.visitor.printer.workbench;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;
import org.eclipse.epsilon.eol.metamodel.EOLElement;
import org.eclipse.epsilon.eol.metamodel.EolPackage;
import org.eclipse.epsilon.eol.visitor.printer.impl.EolPrinter;

public class PrintEOL {

	public static void main(String[] args) throws Exception {
		new PrintEOL().run();
	}

	public void run() throws Exception {

		EolPackage.eINSTANCE.eClass();

		// read all models in "EOL_Programs" and print the corresponding EOL code for each
		File model_folder = new File("EOL_Programs" + File.separatorChar);
		for (File model_file : model_folder.listFiles()) {
			if (!model_file.getName().endsWith(".xmi"))
				continue;
			ResourceSet rs = new ResourceSetImpl();
			rs.getResourceFactoryRegistry().getExtensionToFactoryMap().put("xmi", new XMIResourceFactoryImpl());
			Resource model = rs.getResource(URI.createFileURI(model_file.getAbsolutePath()), true);
			model.load(null);

			EObject root = model.getContents().get(0);

			EolPrinter printer = new EolPrinter();

			printer.run((EOLElement) root);

			try (FileWriter fw = new FileWriter(new File(model_file.getAbsolutePath() + ".eol"))) {
				fw.write(printer.getPrintedProgram());
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}