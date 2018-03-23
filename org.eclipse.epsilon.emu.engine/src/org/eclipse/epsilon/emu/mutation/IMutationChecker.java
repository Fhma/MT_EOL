/**
 * 
 * @author Faisal Alhwikem
 * @version 1.0
 * @since March-2017
 */

package org.eclipse.epsilon.emu.mutation;

import org.eclipse.epsilon.emc.mutant.IProperty;

public interface IMutationChecker {
	public final Object VALID = "valid";
	public final Object INVALID = "invalid";
	public final Object NOT_MUTATABLE = "not able to be mutated";

	public final String ADD_MUTATION_ACTION = "add";
	public final String DEL_MUTATION_ACTION = "delete";
	public final String REPLACE_MUTATION_ACTION = "replace";

	public Object checkConditions(IProperty property, Object value, String mutation_action);

	public Object checkConditions(IProperty property, Object value, Object new_value, String mutation_action);
}