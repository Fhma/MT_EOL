package org.eclipse.epsilon.haetae.eol.ast2eol.workbench;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EcorePackage;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;
import org.eclipse.epsilon.common.util.StringProperties;
import org.eclipse.epsilon.emc.emf.EmfModel;
import org.eclipse.epsilon.eol.exceptions.models.EolModelLoadingException;
import org.eclipse.epsilon.eol.models.IModel;
import org.eclipse.epsilon.eol.models.IRelativePathResolver;

public class LoadModelTest {
	private static String[] mm_paths = { "model/gmf_all.ecore" };

	public static void main(String[] args) throws Exception {

		File models = new File("model/");
		// String uri = "http://www.eclipse.org/emf/2002/Ecore";
		// String uri = "http://www.eclipse.org/gmf/2006/GraphicalDefinition";
		// String uri = "http://www.eclipse.org/gmf/2008/mappings";
		String uri = "http://www.eclipse.org/gmf/2005/ToolDefinition";
		registerAndLoadMetamodels();
		for (File f : models.listFiles()) {
			if (f.getName().endsWith(".xmi")) {
				IModel m = newEmfModel(f.getName(), f.getName(), f.getPath(), uri, true, false, false);
			}
		}

	}

	private static IModel newEmfModel(String name, String aliases, String m, String mm, boolean read, boolean store, boolean cached) throws EolModelLoadingException, URISyntaxException {
		IModel emfModel = new EmfModel();
		StringProperties properties = new StringProperties();
		properties.put(EmfModel.PROPERTY_NAME, name);
		properties.put(EmfModel.PROPERTY_ALIASES, aliases);
		properties.put(EmfModel.PROPERTY_METAMODEL_URI, mm);
		properties.put(EmfModel.PROPERTY_MODEL_URI, new URI(m).toString());
		properties.put(EmfModel.PROPERTY_READONLOAD, read + "");
		properties.put(EmfModel.PROPERTY_CACHED, cached + "");
		properties.put(EmfModel.PROPERTY_STOREONDISPOSAL, store + "");
		try {
			emfModel.load(properties, (IRelativePathResolver) null);
		} catch (Exception e) {
			System.err.println(String.format("Unable to load model [%s]\n", m));
			throw e;
		}
		return emfModel;
	}

	private static void registerAndLoadMetamodels() throws Exception {
		EcorePackage.eINSTANCE.eClass();
		Resource.Factory.Registry.INSTANCE.getExtensionToFactoryMap().put("*", new XMIResourceFactoryImpl());
		ResourceSet rs = new ResourceSetImpl();
		Resource r = null;
		org.eclipse.emf.common.util.URI uri = null;
		if (mm_paths != null) {
			List<EObject> ps = new ArrayList<EObject>();
			for (int i = 0; i < mm_paths.length; i++) {
				uri = org.eclipse.emf.common.util.URI.createURI(new File(mm_paths[i]).getAbsolutePath());
				r = rs.createResource(uri);
				r.load(null);
				getAllEPackages(r.getContents().get(0), ps);
			}
			if (ps.size() == 0) throw new Exception("Unable to find EPackage in resource " + r.getURI());
			for (Object o : ps) {
				if (!(o instanceof EPackage)) throw new Exception("Invalid EPackage object" + o);
				EPackage p = (EPackage) o;
				EPackage.Registry.INSTANCE.put(p.getNsURI(), p);
			}
		}
	}

	private static void getAllEPackages(EObject o, List<EObject> ps) {
		boolean contain = false;
		if (o instanceof EPackage) {
			for (EObject eo : o.eContents()) {
				if (eo instanceof EPackage) {
					contain = true;
					getAllEPackages(eo, ps);
				}
			}
			if (!contain && !ps.contains(o)) ps.add(o);
		}
	}

}
