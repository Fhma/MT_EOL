package org.eclipse.epsilon.emc.mutant;

import org.eclipse.epsilon.eol.exceptions.EolRuntimeException;
import org.eclipse.epsilon.eol.models.IModel;

public interface IProperty {

	public int getLowerBound();

	public void setLowerBound(int lb);

	public int getUpperBound();

	public void setUpperBound(int ub);

	public String getName();

	public void setName(String name);

	public IModel getModel();

	public Object getPropertyValue() throws EolRuntimeException;

	public Object getContainer();

	public String getContainerName();

	public boolean isRequired();

	public boolean isSingleValued();

	public boolean isMultiValued();

	public boolean isReadOnly();

	public boolean isDerived();

	public boolean isSerializable();

	public boolean isAttribute();

	public boolean isAssociation();

	public void prepare(IModel model, Object container);

	boolean isCompatibleValue(Object value);

	boolean isPrimitiveType();
}
