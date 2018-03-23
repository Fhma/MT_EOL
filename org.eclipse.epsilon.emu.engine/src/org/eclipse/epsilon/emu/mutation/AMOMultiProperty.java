/**
 * 
 * @author Faisal Alhwikem
 * @version 1.0
 * @since March-2017
 */
package org.eclipse.epsilon.emu.mutation;

import java.util.Collection;
import org.eclipse.epsilon.emc.mutant.IProperty;

public abstract class AMOMultiProperty {

	@SuppressWarnings("unchecked")
	public static Object checkAdditionConditions(IProperty property, Object value) {
		if (value == null)
			return IMutationChecker.INVALID;
		if (property.getUpperBound() != -1) {
			Collection<Object> col = (Collection<Object>) value;
			if (col.size() + 1 > property.getUpperBound())
				return IMutationChecker.INVALID;
		}
		return IMutationChecker.VALID;
	}

	@SuppressWarnings("unchecked")
	public static Object checkDeletionConditions(IProperty property, Object value) {
		if (value == null)
			return IMutationChecker.INVALID;
		Collection<Object> col = (Collection<Object>) value;
		if (col.size() - 1 < property.getLowerBound())
			return IMutationChecker.INVALID;
		return IMutationChecker.VALID;
	}

	@SuppressWarnings("unchecked")
	public static Object checkReplacementConditions(IProperty property, Object value) {
		if (value == null)
			return IMutationChecker.INVALID;
		Collection<Object> col = (Collection<Object>) value;
		if (col.isEmpty())
			return IMutationChecker.INVALID;
		return IMutationChecker.VALID;
	}

	@SuppressWarnings("unchecked")
	public static Object checkAdditionConditions(IProperty property, Object value, Object valueNew) {
		if (checkAdditionConditions(property, value).equals(IMutationChecker.VALID)) {
			if (valueNew == null)
				return IMutationChecker.INVALID;
			Collection<Object> oldCol = (Collection<Object>) value;
			Collection<Object> newCol = (Collection<Object>) valueNew;
			if (oldCol.size() + 1 != newCol.size())
				return IMutationChecker.INVALID;
			return IMutationChecker.VALID;
		}
		return IMutationChecker.INVALID;
	}

	@SuppressWarnings("unchecked")
	public static Object checkDeletionConditions(IProperty property, Object value, Object valueNew) {
		if (checkDeletionConditions(property, value).equals(IMutationChecker.VALID)) {
			if (valueNew == null)
				return IMutationChecker.INVALID;
			Collection<Object> oldCol = (Collection<Object>) value;
			Collection<Object> newCol = (Collection<Object>) valueNew;
			if (newCol.contains(null))
				return IMutationChecker.INVALID;
			if (newCol.size() < property.getLowerBound())
				return IMutationChecker.INVALID;
			if (oldCol.size() - 1 != newCol.size())
				return IMutationChecker.INVALID;
			return IMutationChecker.VALID;
		}
		return IMutationChecker.INVALID;
	}

	@SuppressWarnings("unchecked")
	public static Object checkReplacementConditions(IProperty property, Object value, Object valueNew) {
		if (checkReplacementConditions(property, value).equals(IMutationChecker.VALID)) {
			if (valueNew == null)
				return IMutationChecker.INVALID;
			Collection<Object> newCol = (Collection<Object>) valueNew;
			if (newCol.contains(null))
				return IMutationChecker.INVALID;
			Collection<Object> oldSet = (Collection<Object>) value;
			int oldSize = oldSet.size();
			if (oldSize == newCol.size()) {
				oldSet.retainAll(newCol);
				if (oldSet.size() + 1 != oldSize)
					return IMutationChecker.INVALID;
			} else
				return IMutationChecker.INVALID;
			return IMutationChecker.VALID;
		}
		return IMutationChecker.INVALID;
	}
}
