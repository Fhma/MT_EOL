/**
 * 
 * @author Faisal Alhwikem
 * @version 1.0
 * @since March-2017
 */

package org.eclipse.epsilon.emu.mutation;

import org.eclipse.epsilon.emc.mutant.IProperty;

public abstract class AMOInteger {

	public static Object checkAdditionConditions(IProperty property, Object value) {
		isIntegerType(property);
		if (value != null)
			return IMutationChecker.INVALID;
		return IMutationChecker.VALID;
	}

	public static Object checkDeletionConditions(IProperty property, Object value) {
		isIntegerType(property);
		if (property.isRequired())
			return IMutationChecker.INVALID;
		if (value == null)
			return IMutationChecker.INVALID;
		return IMutationChecker.VALID;
	}

	public static Object checkReplacementConditions(IProperty property, Object value) {
		isIntegerType(property);
		if (value == null)
			return IMutationChecker.INVALID;
		return IMutationChecker.VALID;
	}

	public static Object checkAdditionConditions(IProperty property, Object value, Object newValue) {
		isIntegerType(property);
		if (checkAdditionConditions(property, value).equals(IMutationChecker.VALID)) {
			if (newValue == null)
				return IMutationChecker.INVALID;
			return IMutationChecker.VALID;
		}
		return IMutationChecker.INVALID;
	}

	public static Object checkDeletionConditions(IProperty property, Object value, Object newValue) {
		isIntegerType(property);
		if (checkDeletionConditions(property, value).equals(IMutationChecker.VALID)) {
			if (newValue != null)
				return IMutationChecker.INVALID;
			return IMutationChecker.VALID;
		}
		return IMutationChecker.INVALID;
	}

	public static Object checkReplacementConditions(IProperty property, Object value, Object newValue) {
		isIntegerType(property);
		if (checkReplacementConditions(property, value).equals(IMutationChecker.VALID)) {
			if(value == null || newValue == null)
				return IMutationChecker.INVALID;
			if (value.equals(newValue))
				return IMutationChecker.INVALID;
			return IMutationChecker.VALID;
		}
		return IMutationChecker.INVALID;
	}

	private static boolean isIntegerType(IProperty property) {
		Object instanceClass = property.getType();
		if (instanceClass.equals(Integer.class) || instanceClass.equals(int.class))
			return true;
		throw new IllegalArgumentException("Non-integer property type: " + property);
	}
}