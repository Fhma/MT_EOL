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

public class PrintMain {

	public static void main(String[] args) throws Exception {
		new PrintMain().run();
	}

	public void run() throws Exception {

		EolPackage.eINSTANCE.eClass();
		
		String model_name = "IfStatement_condition_replace_1";

		ResourceSet resSet = new ResourceSetImpl();
		Resource.Factory.Registry.INSTANCE.getExtensionToFactoryMap().put("xmi", new XMIResourceFactoryImpl());
		Resource model = resSet.getResource(
				URI.createFileURI(new File(getClass().getResource(model_name + ".xmi").getPath()).getAbsolutePath()),
				true);
		model.load(null);

		EObject root = model.getContents().get(0);

		EolPrinter printer = new EolPrinter();

		printer.run((EOLElement) root);

		try (FileWriter fw = new FileWriter(new File(model_name + ".eol"))) {
			fw.write(printer.getPrintedProgram());
		} catch (IOException e) {
			e.printStackTrace();
		}
		System.out.println(printer.getPrintedProgram());
	}
}