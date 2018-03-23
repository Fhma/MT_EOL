/**
 * 
 * @author Faisal Alhwikem
 * @version 1.0
 * @since March-2017
 */

package org.eclipse.epsilon.emu.mutation.execute;

import org.eclipse.epsilon.emc.mutant.IProperty;
import org.eclipse.epsilon.emu.mutation.IMutationChecker;
import org.eclipse.epsilon.emu.mutation.AMOBoolean;
import org.eclipse.epsilon.emu.mutation.AMOInteger;
import org.eclipse.epsilon.emu.mutation.AMOMultiProperty;
import org.eclipse.epsilon.emu.mutation.AMOReal;
import org.eclipse.epsilon.emu.mutation.AMOSingleProperty;
import org.eclipse.epsilon.emu.mutation.AMOString;

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
			if (property.isDataType()) {
				if (mutation_action.equals(ADD_MUTATION_ACTION)) {
					Object instanceClass = (Object) property.getType();
					if (instanceClass.equals(Boolean.class) || instanceClass.equals(boolean.class))
						return AMOBoolean.checkAdditionConditions(property, value);
					if (instanceClass.equals(Integer.class) || instanceClass.equals(int.class))
						return AMOInteger.checkAdditionConditions(property, value);
					if (instanceClass.equals(Float.class) || instanceClass.equals(float.class))
						return AMOReal.checkAdditionConditions(property, value);
					if (instanceClass.equals(String.class))
						return AMOString.checkAdditionConditions(property, value);
				}
				if (mutation_action.equals(DEL_MUTATION_ACTION)) {
					Object instanceClass = (Object) property.getType();
					if (instanceClass.equals(Boolean.class) || instanceClass.equals(boolean.class))
						return AMOBoolean.checkDeletionConditions(property, value);
					if (instanceClass.equals(Integer.class) || instanceClass.equals(int.class))
						return AMOInteger.checkDeletionConditions(property, value);
					if (instanceClass.equals(Float.class) || instanceClass.equals(float.class))
						return AMOReal.checkDeletionConditions(property, value);
					if (instanceClass.equals(String.class))
						return AMOString.checkDeletionConditions(property, value);
				}
				if (mutation_action.equals(REPLACE_MUTATION_ACTION)) {
					Object instanceClass = (Object) property.getType();
					if (instanceClass.equals(Boolean.class) || instanceClass.equals(boolean.class))
						return AMOBoolean.checkReplacementConditions(property, value);
					if (instanceClass.equals(Integer.class) || instanceClass.equals(int.class))
						return AMOInteger.checkReplacementConditions(property, value);
					if (instanceClass.equals(Float.class) || instanceClass.equals(float.class))
						return AMOReal.checkReplacementConditions(property, value);
					if (instanceClass.equals(String.class))
						return AMOString.checkReplacementConditions(property, value);
				}
			} else if (property.isSingleValued()) {
				if (mutation_action.contentEquals(ADD_MUTATION_ACTION))
					return AMOSingleProperty.checkAdditionConditions(property, value);
				if (mutation_action.equals(DEL_MUTATION_ACTION))
					return AMOSingleProperty.checkDeletionConditions(property, value);
				if (mutation_action.equals(REPLACE_MUTATION_ACTION))
					return AMOSingleProperty.checkReplacementConditions(property, value);

			}
		} else {
			if (mutation_action.equals(ADD_MUTATION_ACTION))
				return AMOMultiProperty.checkAdditionConditions(property, value);
			if (mutation_action.equals(DEL_MUTATION_ACTION))
				return AMOMultiProperty.checkDeletionConditions(property, value);
			if (mutation_action.equals(REPLACE_MUTATION_ACTION))
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
			if (property.isDataType()) {
				// single-valued attribute
				if (mutation_action.equals(ADD_MUTATION_ACTION)) {
					Object instanceClass = (Object) property.getType();
					if (instanceClass.equals(Boolean.class) || instanceClass.equals(boolean.class))
						return AMOBoolean.checkAdditionConditions(property, value, new_value);
					if (instanceClass.equals(Integer.class) || instanceClass.equals(int.class))
						return AMOInteger.checkAdditionConditions(property, value, new_value);
					if (instanceClass.equals(Float.class) || instanceClass.equals(float.class))
						return AMOReal.checkAdditionConditions(property, value, new_value);
					if (instanceClass.equals(String.class))
						return AMOString.checkAdditionConditions(property, value, new_value);
				}
				if (mutation_action.equals(DEL_MUTATION_ACTION)) {
					Object instanceClass = (Object) property.getType();
					if (instanceClass.equals(Boolean.class) || instanceClass.equals(boolean.class))
						return AMOBoolean.checkDeletionConditions(property, value, new_value);
					if (instanceClass.equals(Integer.class) || instanceClass.equals(int.class))
						return AMOInteger.checkDeletionConditions(property, value, new_value);
					if (instanceClass.equals(Float.class) || instanceClass.equals(float.class))
						return AMOReal.checkDeletionConditions(property, value, new_value);
					if (instanceClass.equals(String.class))
						return AMOString.checkDeletionConditions(property, value, new_value);
				}
				if (mutation_action.equals(REPLACE_MUTATION_ACTION)) {
					Object instanceClass = (Object) property.getType();
					if (instanceClass.equals(Boolean.class) || instanceClass.equals(boolean.class))
						return AMOBoolean.checkReplacementConditions(property, value, new_value);
					if (instanceClass.equals(Integer.class) || instanceClass.equals(int.class))
						return AMOInteger.checkReplacementConditions(property, value, new_value);
					if (instanceClass.equals(Float.class) || instanceClass.equals(float.class))
						return AMOReal.checkReplacementConditions(property, value, new_value);
					if (instanceClass.equals(String.class))
						return AMOString.checkReplacementConditions(property, value, new_value);
				}
			} else {
				// single-valued reference
				if (mutation_action.equals(ADD_MUTATION_ACTION))
					return AMOSingleProperty.checkAdditionConditions(property, value, new_value);
				if (mutation_action.equals(DEL_MUTATION_ACTION))
					return AMOSingleProperty.checkDeletionConditions(property, value, new_value);
				if (mutation_action.equals(REPLACE_MUTATION_ACTION))
					return AMOSingleProperty.checkReplacementConditions(property, value, new_value);
			}
		} else {
			if (mutation_action.equals(ADD_MUTATION_ACTION))
				return AMOMultiProperty.checkAdditionConditions(property, value, new_value);
			if (mutation_action.equals(DEL_MUTATION_ACTION))
				return AMOMultiProperty.checkDeletionConditions(property, value, new_value);
			if (mutation_action.equals(REPLACE_MUTATION_ACTION))
				return AMOMultiProperty.checkReplacementConditions(property, value, new_value);
		}
		throw new IllegalArgumentException("Unsupported property feature type: " + property);
	}
}
