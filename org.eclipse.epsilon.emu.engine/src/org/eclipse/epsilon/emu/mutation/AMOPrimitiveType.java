/**
 * 
 * @author Faisal Alhwikem
 * @version 1.0
 * @since March-2017
 */

package org.eclipse.epsilon.emu.mutation;

import org.eclipse.epsilon.emc.mutant.IProperty;

public abstract class AMOPrimitiveType {

	public static Object checkAdditionConditions(IProperty property, Object value) {
		return IMutationChecker.INVALID;
	}

	public static Object checkDeletionConditions(IProperty property, Object value) {
		return IMutationChecker.INVALID;
	}

	public static Object checkReplacementConditions(IProperty property, Object value) {
		if (value != null)
			return IMutationChecker.VALID;
		return IMutationChecker.INVALID;
	}

	public static Object checkAdditionConditions(IProperty property, Object value, Object newValue) {
		return IMutationChecker.INVALID;
	}

	public static Object checkDeletionConditions(IProperty property, Object value, Object newValue) {
		return IMutationChecker.INVALID;
	}

	public static Object checkReplacementConditions(IProperty property, Object value, Object newValue) {
		if (checkReplacementConditions(property, value).equals(IMutationChecker.VALID)) {
			if (newValue == null)
				return IMutationChecker.INVALID;
			if (newValue.equals(value))
				return IMutationChecker.INVALID;
			if (!property.isCompatibleValue(newValue))
				return IMutationChecker.INVALID;
			return IMutationChecker.VALID;
		}
		return IMutationChecker.INVALID;
	}
}