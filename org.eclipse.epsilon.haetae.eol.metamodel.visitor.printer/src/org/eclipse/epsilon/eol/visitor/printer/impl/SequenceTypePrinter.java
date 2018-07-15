package org.eclipse.epsilon.eol.visitor.printer.impl;

import org.eclipse.epsilon.eol.metamodel.FormalParameterExpression;
import org.eclipse.epsilon.eol.metamodel.OperationDefinition;
import org.eclipse.epsilon.eol.metamodel.SequenceType;
import org.eclipse.epsilon.eol.metamodel.visitor.EolVisitorController;
import org.eclipse.epsilon.eol.metamodel.visitor.SequenceTypeVisitor;
import org.eclipse.epsilon.eol.visitor.printer.context.EOLPrinterContext;

public class SequenceTypePrinter extends SequenceTypeVisitor<EOLPrinterContext, Object> {

	@Override
	public Object visit(SequenceType sequenceType, EOLPrinterContext context, EolVisitorController<EOLPrinterContext, Object> controller) {
		String result = "";
		// FIXED: prevent printing parentheses for a return collection type of an operation declaration
		if (sequenceType.getContainer() instanceof OperationDefinition || sequenceType.getContentType() == null) {
			result = "Sequence";
		} else if (sequenceType.getContainer() instanceof FormalParameterExpression && sequenceType.getContainer().getContainer() instanceof OperationDefinition) {
			// FIXED: prevent printing parentheses for a formal parameter type of a collection
			result = "Sequence";
		} else {
			result += "Sequence(" + controller.visit(sequenceType.getContentType(), context) + ")";
		}
		return result;
	}

}
