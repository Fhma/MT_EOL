package org.eclipse.epsilon.eol.visitor.printer.impl;

import org.eclipse.epsilon.eol.metamodel.FormalParameterExpression;
import org.eclipse.epsilon.eol.metamodel.OperationDefinition;
import org.eclipse.epsilon.eol.metamodel.OrderedSetType;
import org.eclipse.epsilon.eol.metamodel.visitor.EolVisitorController;
import org.eclipse.epsilon.eol.metamodel.visitor.OrderedSetTypeVisitor;
import org.eclipse.epsilon.eol.visitor.printer.context.EOLPrinterContext;

public class OrderedSetTypePrinter extends OrderedSetTypeVisitor<EOLPrinterContext, Object>{

	@Override
	public Object visit(OrderedSetType orderedSetType, EOLPrinterContext context,
			EolVisitorController<EOLPrinterContext, Object> controller) {
		String result = "";
		//FIXED: the collection type has no contentType at operation declaration
		if (orderedSetType.getContainer() instanceof OperationDefinition || orderedSetType.getContentType() == null) {
			result = "OrderedSet";
		}
		else if (orderedSetType.getContainer() instanceof FormalParameterExpression && orderedSetType.getContainer().getContainer() instanceof OperationDefinition) {
			// FIXED: prevent printing parentheses for a formal parameter type of a collection
			result = "OrderedSet";
		}
		else {
			result += "OrderedSet(" + controller.visit(orderedSetType.getContentType(), context) + ")";
		}
		return result;
	}

}
