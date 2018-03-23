package org.eclipse.epsilon.emc.mutant;

import org.eclipse.epsilon.eol.models.IModel;
import org.eclipse.epsilon.emc.mutant.IProperty;;

public interface IMutant extends IModel {
	public IProperty getProperty(Object object, String p_name);
}
