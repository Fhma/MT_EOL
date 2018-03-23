/**
 * 
 * @author Faisal Alhwikem
 * @version 1.0
 * @since March-2017
 */

package org.eclipse.epsilon.emu.mutation;

import org.eclipse.epsilon.emc.mutant.IProperty;

public abstract class AMOString {

	public static Object checkAdditionConditions(IProperty property, Object value) {
		isTypeOfString(property);
		if (value != null)
			return IMutationChecker.INVALID;
		return IMutationChecker.VALID;
	}

	public static Object checkDeletionConditions(IProperty property, Object value) {
		isTypeOfString(property);
		if (property.isRequired() || value == null)
			return IMutationChecker.INVALID;
		return IMutationChecker.VALID;
	}

	public static Object checkReplacementConditions(IProperty property, Object value) {
		isTypeOfString(property);
		if (value == null)
			return IMutationChecker.INVALID;
		return IMutationChecker.VALID;
	}

	public static Object checkAdditionConditions(IProperty property, Object value, Object valueNew) {
		isTypeOfString(property);
		if (checkAdditionConditions(property, value).equals(IMutationChecker.VALID)) {
			if (valueNew == null)
				return IMutationChecker.INVALID;
			if (property.isRequired()) {
				String s = (String) valueNew;
				if (s.length() <= 0)
					return IMutationChecker.INVALID;
			}
			return IMutationChecker.VALID;
		}
		return IMutationChecker.INVALID;
	}

	public static Object checkDeletionConditions(IProperty property, Object value, Object valueNew) {
		isTypeOfString(property);
		if (checkDeletionConditions(property, value).equals(IMutationChecker.VALID)) {
			if (valueNew != null)
				return IMutationChecker.INVALID;
			return IMutationChecker.VALID;
		}
		return IMutationChecker.INVALID;
	}

	public static Object checkReplacementConditions(IProperty property, Object value, Object valueNew) {
		isTypeOfString(property);
		if (checkReplacementConditions(property, value).equals(IMutationChecker.VALID)) {
			if (valueNew == null || value == null)
				return IMutationChecker.INVALID;
			if (valueNew.equals(value))
				return IMutationChecker.INVALID;
			String s = (String) valueNew;
			if (property.isRequired() && s.length() <= 0)
				return IMutationChecker.INVALID;
			return IMutationChecker.VALID;
		}
		return IMutationChecker.INVALID;
	}

	private static boolean isTypeOfString(IProperty property) {
		Object instanceClass = property.getType();
		if (instanceClass.equals(String.class))
			return true;
		throw new IllegalArgumentException("Non-string property type: " + property);
	}
}