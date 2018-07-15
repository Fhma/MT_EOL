package uk.ac.york.cs.eol.programs.selector;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EClassifier;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EcorePackage;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;
import org.eclipse.emf.common.util.URI;

public class Main {

	private static final int MAX_SELECTED_MODELS = 10;
	private static List<EClass> eClasses = new ArrayList<EClass>();

	public static void main(String[] args) {
		File files = new File("models" + File.separatorChar);
		int count = 0;
		for (File f : files.listFiles()) {
			if (f.getName().endsWith(".xmi"))
				count++;
		}
		File models[] = new File[count];
		int i = 0;
		for (File f : files.listFiles()) {
			if (f.getName().endsWith(".xmi"))
				models[i++] = f;
		}

		EcorePackage.eINSTANCE.eClass();
		ResourceSet rs = new ResourceSetImpl();
		rs.getResourceFactoryRegistry().getExtensionToFactoryMap().put("*", new XMIResourceFactoryImpl());
		Resource metamodel = rs.createResource(URI.createFileURI(new File("models/EOL.ecore").getAbsolutePath()));
		try {
			metamodel.load(Collections.EMPTY_MAP);
		} catch (Exception e) {
			e.printStackTrace();
		}
		EPackage p = (EPackage) metamodel.getContents().get(0);
		EPackage.Registry.INSTANCE.put(p.getNsURI(), p);
		for (EClassifier e : p.getEClassifiers()) {
			if (e instanceof EClass) {
				EClass ec = (EClass) e;
				if (!ec.isAbstract() && !eClasses.contains(ec))
					eClasses.add(ec);
			}
		}

		float coverages[] = new float[models.length];
		int instances[] = new int[models.length];

		for (i = 0; i < models.length; i++) {
			Map<EClass, Integer> map = new HashMap<EClass, Integer>();
			calculateInstances(models[i], map);
			Iterator<Entry<EClass, Integer>> it = map.entrySet().iterator();
			int sum = 0;
			while (it.hasNext()) {
				Entry<EClass, Integer> entry = it.next();
				sum = sum + entry.getValue();
			}
			instances[i] = sum;
			coverages[i] = (float) map.keySet().size() / eClasses.size();
		}
		sort(models, coverages, instances);
		System.out.println("Models after sort: ");
		for (i = 0; i < models.length; i++) {
			System.out.println(String.format("\t%s, %f, %d", models[i].getName(), coverages[i], instances[i]));
		}
		List<File> result = selection(models);
		System.out.println("Selected models: ");
		for (i = 0; i < result.size(); i++) {
			System.out.println(String.format("\t%s", result.get(i).getName()));
		}
	}

	private static void calculateInstances(File model, Map<EClass, Integer> instances) {
		ResourceSet rs = new ResourceSetImpl();
		rs.getResourceFactoryRegistry().getExtensionToFactoryMap().put("*", new XMIResourceFactoryImpl());
		Resource m = rs.createResource(URI.createFileURI(model.getAbsolutePath()));
		try {
			m.load(Collections.EMPTY_MAP);
		} catch (Exception e) {
			e.printStackTrace();
		}
		Iterator<EObject> it = m.getAllContents();
		EObject obj;
		while (it.hasNext()) {
			obj = it.next();
			if (eClasses.contains(obj.eClass())) {
				if (instances.get(obj.eClass()) == null) {
					instances.put(obj.eClass(), 0);
				}
				int old = instances.get(obj.eClass());
				instances.put(obj.eClass(), ++old);
			}
		}
	}

	private static void sort(File[] models, float[] coverage, int[] instances) {
		// sort by instances number
		for (int i = 1; i < models.length; i++) {
			for (int j = i; j > 0; j--) {
				if (instances[j - 1] < instances[j]) {
					File f = models[j];
					float c = coverage[j];
					int n = instances[j];
					models[j] = models[j - 1];
					coverage[j] = coverage[j - 1];
					instances[j] = instances[j - 1];
					models[j - 1] = f;
					coverage[j - 1] = c;
					instances[j - 1] = n;
				}
			}
		}

		// sort by coverage
		for (int i = 1; i < models.length; i++) {
			for (int j = i; j > 0; j--) {
				if (coverage[j - 1] < coverage[j]) {
					File f = models[j];
					float c = coverage[j];
					int n = instances[j];
					models[j] = models[j - 1];
					coverage[j] = coverage[j - 1];
					instances[j] = instances[j - 1];
					models[j - 1] = f;
					coverage[j - 1] = c;
					instances[j - 1] = n;
				}
			}
		}
	}

	private static List<File> selection(File[] models) {
		// create a map where a model is a key and instantiated eClasses as value
		Map<File, List<EClass>> table = new HashMap<File, List<EClass>>();
		for (int i = 0; i < models.length; i++) {
			List<EClass> list = getInstantiatedEClasses(models[i]);
			table.put(models[i], list);
		}

		// start selecting models
		List<File> selected = new ArrayList<File>();
		int current = 0;
		while (selected.size() < MAX_SELECTED_MODELS) {
			if (current >= models.length)
				break;
			if (!selected.contains(models[current])) {
				selected.add(models[current]);
			} else {
				current++;
				continue;
			}
			if (current + 1 >= models.length)
				break;

			// going through all other models and calculate coverage
			File optimal_model = null;
			int optimal_count = 0;
			for (int j = current + 1; j < models.length; j++) {
				if (!selected.contains(models[j])) {
					int count = calcuateDifference(table.get(models[current]), table.get(models[j]));
					System.out.println(String.format("%s, %s, %d", models[current].getName(), models[j].getName(), count));
					if (count > optimal_count) {
						optimal_count = count;
						optimal_model = models[j];
					}
				}
			}
			if (optimal_model != null)
				selected.add(optimal_model);
		}
		return selected;
	}

	private static int calcuateDifference(List<EClass> left, List<EClass> right) {
		Set<EClass> union = new HashSet<EClass>();
		union.addAll(left);
		union.addAll(right);
		return union.size();
	}

	private static List<EClass> getInstantiatedEClasses(File model) {
		ResourceSet rs = new ResourceSetImpl();
		rs.getResourceFactoryRegistry().getExtensionToFactoryMap().put("*", new XMIResourceFactoryImpl());
		Resource m = rs.createResource(URI.createFileURI(model.getAbsolutePath()));
		try {
			m.load(Collections.EMPTY_MAP);
		} catch (Exception e) {
			e.printStackTrace();
		}
		Iterator<EObject> it = m.getAllContents();
		List<EClass> instantiated = new ArrayList<EClass>();
		EObject obj;
		while (it.hasNext()) {
			obj = it.next();
			if (eClasses.contains(obj.eClass()) && !instantiated.contains(obj.eClass())) {
				instantiated.add(obj.eClass());
			}
		}
		return instantiated;
	}
}