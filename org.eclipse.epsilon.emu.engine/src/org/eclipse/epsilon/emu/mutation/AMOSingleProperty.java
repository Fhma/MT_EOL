/**
 * 
 * @author Faisal Alhwikem
 * @version 1.0
 * @since March-2017
 */

package org.eclipse.epsilon.emu.mutation;

import org.eclipse.epsilon.emc.mutant.IProperty;

public abstract class AMOSingleProperty {

	public static Object checkAdditionConditions(IProperty property, Object value) {
		if (value != null)
			return IMutationChecker.INVALID;
		return IMutationChecker.VALID;
	}

	public static Object checkDeletionConditions(IProperty property, Object value) {
		if (property.isRequired())
			return IMutationChecker.INVALID;
		if (value == null)
			return IMutationChecker.INVALID;
		return IMutationChecker.VALID;
	}

	public static Object checkReplacementConditions(IProperty property, Object value) {
		if (value == null)
			return IMutationChecker.INVALID;
		return IMutationChecker.VALID;
	}

	public static Object checkAdditionConditions(IProperty property, Object value, Object newValue) {
		if (checkAdditionConditions(property, value).equals(IMutationChecker.VALID)) {
			if (newValue == null)
				return IMutationChecker.INVALID;
			if (newValue.equals(value))
				return IMutationChecker.INVALID;
			return IMutationChecker.VALID;
		}
		return IMutationChecker.INVALID;
	}

	public static Object checkDeletionConditions(IProperty property, Object value, Object newValue) {
		if (checkDeletionConditions(property, value).equals(IMutationChecker.VALID)) {
			if (newValue != null)
				return IMutationChecker.INVALID;
			return IMutationChecker.VALID;
		}
		return IMutationChecker.INVALID;
	}

	public static Object checkReplacementConditions(IProperty property, Object value, Object newValue) {
		if (checkReplacementConditions(property, value).equals(IMutationChecker.VALID)) {
			if (newValue == null)
				return IMutationChecker.INVALID;
			if (newValue.equals(value))
				return IMutationChecker.INVALID;
			return IMutationChecker.VALID;
		}
		return IMutationChecker.INVALID;
	}
}
