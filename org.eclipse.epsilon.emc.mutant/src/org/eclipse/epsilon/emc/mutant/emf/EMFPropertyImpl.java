package org.eclipse.epsilon.emc.mutant.emf;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Date;
import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EReference;
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

	public boolean isCompatibleValue(Object value) {
		if (value == null)
			return false;
		if (isAttribute())
			return isCompatibleValueAttribute(value);
		if (isAssociation())
			return isCompatibleValueAssociation(value);
		return false;
	}

	private boolean isCompatibleValueAttribute(Object v) {

		if ((type.equals(boolean.class) || type.equals(Boolean.class)) && v instanceof Boolean)
			return true;

		if ((type.equals(byte.class) || type.equals(Byte.class)) && v instanceof Byte)
			return true;

		if ((type.equals(short.class) || type.equals(Short.class)) && v instanceof Short)
			return true;

		if ((type.equals(int.class) || type.equals(Integer.class)) && v instanceof Integer)
			return true;

		if ((type.equals(long.class) || type.equals(Long.class)) && v instanceof Long)
			return true;

		if ((type.equals(float.class) || type.equals(Float.class)) && v instanceof Float)
			return true;

		if ((type.equals(double.class) || type.equals(Double.class)) && v instanceof Double)
			return true;

		if ((type.equals(char.class) || type.equals(Character.class)) && v instanceof Character)
			return true;

		if ((type.equals(String.class)) && v instanceof String)
			return true;

		if ((type.equals(Date.class)) && v instanceof Date)
			return true;

		if ((type.equals(BigInteger.class)) && v instanceof BigInteger)
			return true;

		if ((type.equals(BigDecimal.class)) && v instanceof BigDecimal)
			return true;
		return false;
	}

	@Override
	public boolean isPrimitiveType() {
		if (type.equals(boolean.class) || type.equals(byte.class) || type.equals(short.class) || type.equals(int.class) || type.equals(long.class) || type.equals(float.class)
				|| type.equals(double.class) || type.equals(char.class)) {
			return true;
		}
		return false;
	}

	private boolean isCompatibleValueAssociation(Object v) {
		try {
			return getModel().isOfKind(v, (String) type);
		} catch (EolModelElementTypeNotFoundException e) {
			return false;
		}
	}

	@Override
	public boolean isAttribute() {
		if (sf instanceof EAttribute)
			return true;
		return false;
	}

	@Override
	public boolean isAssociation() {
		if (sf instanceof EReference)
			return true;
		return false;
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
	public IModel getModel() {
		return containerIModel;
	}

	@Override
	public Object getPropertyValue() throws EolRuntimeException {
		return getModel().getPropertyGetter().invoke(getContainer(), getName());
	}

	@Override
	public Object getContainer() {
		return container;
	}

	@Override
	public String getContainerName() {
		String s[] = getModel().getTypeNameOf(container).split(":");
		return s[s.length - 1];
	}

	@Override
	public void prepare(IModel model, Object container) {
		this.containerIModel = model;
		this.container = container;

		if (isAttribute()) {
			type = sf.getEType().getInstanceClass();
		}

		if (isAssociation()) {
			EClass c = (EClass) sf.getEType();
			type = c.getName();
		}
	}
}
