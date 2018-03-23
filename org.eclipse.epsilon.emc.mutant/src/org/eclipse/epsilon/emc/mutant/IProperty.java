package org.eclipse.epsilon.emc.mutant;

import org.eclipse.epsilon.eol.exceptions.EolRuntimeException;
import org.eclipse.epsilon.eol.exceptions.models.EolModelElementTypeNotFoundException;
import org.eclipse.epsilon.eol.models.IModel;

public interface IProperty {

	public int getLowerBound();

	public void setLowerBound(int lb);

	public int getUpperBound();

	public void setUpperBound(int ub);

	public String getName();

	public void setName(String name);

	public IModel getContainerModel();

	public void setContainerModel(IModel model);

	public Object getPropertyValue() throws EolRuntimeException;

	public Object getContainer();

	public String getContainerName();

	public Object getType();

	public void setContainer(Object container);
	
	public void setType (Object type);

	public boolean isRequired();

	public boolean isSingleValued();

	public boolean isMultiValued();

	public boolean isReadOnly();

	public boolean isDerived();

	public boolean isSerializable();

	public boolean isDataType();

	public boolean isPropertyTypeOfKind(Object target_type) throws EolModelElementTypeNotFoundException;

	public boolean isPropertyTypeOfType(Object target_type) throws EolModelElementTypeNotFoundException;
	
	

}
