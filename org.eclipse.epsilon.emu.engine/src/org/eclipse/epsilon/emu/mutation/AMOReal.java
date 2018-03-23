/**
 * 
 * @author Faisal Alhwikem
 * @version 1.0
 * @since March-2017
 */

package org.eclipse.epsilon.emu.mutation;

import org.eclipse.epsilon.emc.mutant.IProperty;

public abstract class AMOReal {

	public static Object checkAdditionConditions(IProperty property, Object value) {
		isTypeOfReal(property);
		if (value != null)
			return IMutationChecker.INVALID;
		return IMutationChecker.VALID;
	}

	public static Object checkDeletionConditions(IProperty property, Object value) {
		isTypeOfReal(property);
		if (property.isRequired())
			return IMutationChecker.INVALID;
		if (value == null)
			return IMutationChecker.INVALID;
		return IMutationChecker.VALID;
	}

	public static Object checkReplacementConditions(IProperty property, Object value) {
		isTypeOfReal(property);
		if (value == null)
			return IMutationChecker.INVALID;
		return IMutationChecker.VALID;
	}

	public static Object checkAdditionConditions(IProperty property, Object value, Object newValue) {
		isTypeOfReal(property);
		if (checkAdditionConditions(property, value).equals(IMutationChecker.VALID)) {
			if (newValue == null)
				return IMutationChecker.INVALID;
			return IMutationChecker.VALID;
		}
		return IMutationChecker.INVALID;
	}

	public static Object checkDeletionConditions(IProperty property, Object value, Object newValue) {
		isTypeOfReal(property);
		if (checkDeletionConditions(property, value).equals(IMutationChecker.VALID)) {
			if (newValue != null)
				return IMutationChecker.INVALID;
			return IMutationChecker.VALID;
		}
		return IMutationChecker.INVALID;
	}

	public static Object checkReplacementConditions(IProperty property, Object value, Object newValue) {
		isTypeOfReal(property);
		if (checkReplacementConditions(property, value).equals(IMutationChecker.VALID)) {
			if (value == null || newValue == null)
				return IMutationChecker.INVALID;
			if (value.equals(newValue))
				return IMutationChecker.INVALID;
			return IMutationChecker.VALID;
		}
		return IMutationChecker.INVALID;
	}

	private static boolean isTypeOfReal(IProperty property) {
		Object instanceClass = property.getType();
		if (instanceClass.equals(Float.class) || instanceClass.equals(float.class))
			return true;
		throw new IllegalArgumentException("Non-real property type " + property);
	}
}