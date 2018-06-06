/**
 * 
 * @author Faisal Alhwikem
 * @version 1.0
 * @since March-2017
 */

package org.eclipse.epsilon.emu.mutation.execute;

import org.eclipse.epsilon.emc.mutant.IProperty;
import org.eclipse.epsilon.emu.mutation.IMutationChecker;
import org.eclipse.epsilon.emu.mutation.AMOMultiProperty;
import org.eclipse.epsilon.emu.mutation.AMOPrimitiveType;
import org.eclipse.epsilon.emu.mutation.AMOSingleProperty;

public class MutationCheckerImpl implements IMutationChecker {

	@Override
	public Object checkConditions(IProperty property, Object value, String mutation_action) {
		if (property == null)
			throw new NullPointerException("Undefined feature object");
		if (mutation_action == null)
			throw new NullPointerException("Undefined mutation action");

		if (property.isDerived() || property.isReadOnly() || !property.isSerializable())
			return IMutationChecker.NOT_MUTATABLE;

		if (!property.isMultiValued()) {
			if (isPrimitiveType(property)) {
				if (mutation_action.equals(ADD_MUTATION_ACTION)) {
					return AMOPrimitiveType.checkAdditionConditions(property, value);
				}
				if (mutation_action.equals(DEL_MUTATION_ACTION)) {
					return AMOPrimitiveType.checkDeletionConditions(property, value);
				}
				if (mutation_action.equals(REP_MUTATION_ACTION)) {
					return AMOPrimitiveType.checkReplacementConditions(property, value);
				}
				return IMutationChecker.NOT_MUTATABLE;
			} else if (property.isSingleValued()) {
				if (mutation_action.contentEquals(ADD_MUTATION_ACTION))
					return AMOSingleProperty.checkAdditionConditions(property, value);
				if (mutation_action.equals(DEL_MUTATION_ACTION))
					return AMOSingleProperty.checkDeletionConditions(property, value);
				if (mutation_action.equals(REP_MUTATION_ACTION))
					return AMOSingleProperty.checkReplacementConditions(property, value);
			}
		} else {
			if (mutation_action.equals(ADD_MUTATION_ACTION))
				return AMOMultiProperty.checkAdditionConditions(property, value);
			if (mutation_action.equals(DEL_MUTATION_ACTION))
				return AMOMultiProperty.checkDeletionConditions(property, value);
			if (mutation_action.equals(REP_MUTATION_ACTION))
				return AMOMultiProperty.checkReplacementConditions(property, value);
		}
		throw new IllegalArgumentException("Unsupported property feature type: " + property);
	}

	@Override
	public Object checkConditions(IProperty property, Object value, Object new_value, String mutation_action) {

		if (property == null)
			throw new NullPointerException("Undefined property object");
		if (mutation_action == null)
			throw new NullPointerException("Undefined mutation action");

		if (property.isDerived() || property.isReadOnly() || !property.isSerializable())
			return IMutationChecker.NOT_MUTATABLE;

		if (!property.isMultiValued()) {
			if (isPrimitiveType(property)) {
				if (mutation_action.equals(ADD_MUTATION_ACTION)) {
					return AMOPrimitiveType.checkAdditionConditions(property, value, new_value);
				}
				if (mutation_action.equals(DEL_MUTATION_ACTION)) {
					return AMOPrimitiveType.checkDeletionConditions(property, value, new_value);
				}
				if (mutation_action.equals(REP_MUTATION_ACTION)) {
					return AMOPrimitiveType.checkReplacementConditions(property, value, new_value);
				}
			} else {
				if (mutation_action.equals(ADD_MUTATION_ACTION))
					return AMOSingleProperty.checkAdditionConditions(property, value, new_value);
				if (mutation_action.equals(DEL_MUTATION_ACTION))
					return AMOSingleProperty.checkDeletionConditions(property, value, new_value);
				if (mutation_action.equals(REP_MUTATION_ACTION))
					return AMOSingleProperty.checkReplacementConditions(property, value, new_value);
			}
		} else {
			if (mutation_action.equals(ADD_MUTATION_ACTION))
				return AMOMultiProperty.checkAdditionConditions(property, value, new_value);
			if (mutation_action.equals(DEL_MUTATION_ACTION))
				return AMOMultiProperty.checkDeletionConditions(property, value, new_value);
			if (mutation_action.equals(REP_MUTATION_ACTION))
				return AMOMultiProperty.checkReplacementConditions(property, value, new_value);
		}
		throw new IllegalArgumentException("Unsupported property feature type: " + property);
	}

	private boolean isPrimitiveType(IProperty property) {
		return property.isPrimitiveType();
	}
}
