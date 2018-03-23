package org.eclipse.epsilon.emc.mutant.emf;

import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.epsilon.emc.mutant.IProperty;
import org.eclipse.epsilon.eol.exceptions.EolRuntimeException;
import org.eclipse.epsilon.eol.exceptions.models.EolModelElementTypeNotFoundException;
import org.eclipse.epsilon.eol.models.IModel;

public class EMFPropertyImpl implements IProperty {

	private EStructuralFeature sf = null;

	private IModel containerIModel;
	private Object container;
	private Object type;

	public EMFPropertyImpl(EStructuralFeature feature) {
		sf = feature;
	}

	@Override
	public int getLowerBound() {
		return sf.getLowerBound();
	}

	@Override
	public int getUpperBound() {
		return sf.getUpperBound();
	}

	@Override
	public boolean isRequired() {
		return sf.isRequired();
	}

	@Override
	public boolean isSingleValued() {
		return !isMultiValued();
	}

	@Override
	public boolean isMultiValued() {
		return sf.isMany();
	}

	@Override
	public boolean isReadOnly() {
		return !(sf.isChangeable());
	}

	@Override
	public boolean isDerived() {
		return sf.isDerived();
	}

	@Override
	public boolean isSerializable() {
		return !sf.isTransient();
	}

	@Override
	public String getName() {
		return sf.getName();
	}

	@Override
	public Object getType() {
		if (type == null) {
			if (isDataType()) {
				type = sf.getEType().getInstanceClass();
			} else {
				type = sf.getEType().getName();
			}
		}
		return type;
	}

	@Override
	public void setLowerBound(int lb) {
		sf.setLowerBound(lb);

	}

	@Override
	public void setUpperBound(int ub) {
		sf.setUpperBound(ub);

	}

	@Override
	public void setName(String name) {
		sf.setName(name);

	}

	@Override
	public boolean isDataType() {
		if (sf instanceof EAttribute)
			return true;
		return false;
	}

	@Override
	public IModel getContainerModel() {
		return containerIModel;
	}

	@Override
	public void setContainerModel(IModel model) {
		this.containerIModel = model;
	}

	@Override
	public Object getPropertyValue() throws EolRuntimeException {
		return getContainerModel().getPropertyGetter().invoke(getContainer(), getName());
	}

	@Override
	public Object getContainer() {
		return container;
	}

	@Override
	public void setContainer(Object container) {
		this.container = container;
	}

	@Override
	public boolean isPropertyTypeOfKind(Object target_type) throws EolModelElementTypeNotFoundException {
		return getContainerModel().isOfKind(getContainer(), getContainerModel().getTypeNameOf(target_type));
	}

	@Override
	public boolean isPropertyTypeOfType(Object target_type) throws EolModelElementTypeNotFoundException {
		return getContainerModel().isOfType(getContainer(), getContainerModel().getTypeNameOf(target_type));
	}

	@Override
	public String getContainerName() {
		String s[] = getContainerModel().getTypeNameOf(container).split(":");
		return s[s.length - 1];
	}

	@Override
	public void setType(Object type) {
		this.type = type;
	}
}
