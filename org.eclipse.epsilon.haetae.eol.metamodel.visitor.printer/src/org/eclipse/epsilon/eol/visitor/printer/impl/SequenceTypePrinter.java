package org.eclipse.epsilon.eol.visitor.printer.impl;

import org.eclipse.epsilon.eol.metamodel.OperationDefinition;
import org.eclipse.epsilon.eol.metamodel.SequenceType;
import org.eclipse.epsilon.eol.metamodel.visitor.EolVisitorController;
import org.eclipse.epsilon.eol.metamodel.visitor.SequenceTypeVisitor;
import org.eclipse.epsilon.eol.visitor.printer.context.EOLPrinterContext;

public class SequenceTypePrinter extends SequenceTypeVisitor<EOLPrinterContext, Object>{

	@Override
	public Object visit(SequenceType sequenceType, EOLPrinterContext context,
			EolVisitorController<EOLPrinterContext, Object> controller) {
		String result = "";
		//FIXED: the collection type has no contentType at operation declaration
		if(sequenceType.getContainer() instanceof OperationDefinition || sequenceType.getContentType() == null) {
			result = "Sequence";
		}
		else {
			result += "Sequence(" + controller.visit(sequenceType.getContentType(), context) + ")";
		}
		return result;
	}

}
