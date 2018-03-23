package org.eclipse.epsilon.emc.mutant.emf;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.xmi.XMLResource;
import org.eclipse.emf.ecore.resource.Resource;

import org.eclipse.epsilon.emc.emf.EmfModel;
import org.eclipse.epsilon.emc.emf.EmfUtil;
import org.eclipse.epsilon.emc.mutant.IMutant;
import org.eclipse.epsilon.emc.mutant.IProperty;

public class EmfMutant extends EmfModel implements IMutant {

	public EmfMutant() {
		super();
	}

	@Override
	public boolean store() {
		if (modelImpl == null)
			return false;
		try {
			Map<String, Boolean> options = null;
			if (!metamodelFileUris.isEmpty()) {
				options = new HashMap<String, Boolean>();
				options.put(XMLResource.OPTION_SCHEMA_LOCATION, true);
			}
			modelImpl.save(options);
			return true;
		}
		catch(Resource.IOWrappedException e)
		{
			// Dangling Exception: do nothing
			return false;
		}
		catch (IOException e) {
			e.printStackTrace();
			return false;
		}
	}

	@Override
	public IProperty getProperty(Object object, String p_name) {
		EObject eObject = null;
		if (object instanceof EObject)
			eObject = (EObject) object;
		if (eObject == null)
			throw new IllegalArgumentException("The object is not of EObject [ " + object + " ]");
		EStructuralFeature sf = EmfUtil.getEStructuralFeature(eObject.eClass(), p_name);
		return new EMFPropertyImpl(sf);
	}
}
