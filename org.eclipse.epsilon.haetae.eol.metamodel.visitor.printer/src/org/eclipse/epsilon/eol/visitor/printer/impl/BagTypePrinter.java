package org.eclipse.epsilon.eol.visitor.printer.impl;

import org.eclipse.epsilon.eol.metamodel.BagType;
import org.eclipse.epsilon.eol.metamodel.FormalParameterExpression;
import org.eclipse.epsilon.eol.metamodel.OperationDefinition;
import org.eclipse.epsilon.eol.metamodel.visitor.BagTypeVisitor;
import org.eclipse.epsilon.eol.metamodel.visitor.EolVisitorController;
import org.eclipse.epsilon.eol.visitor.printer.context.EOLPrinterContext;

public class BagTypePrinter extends BagTypeVisitor<EOLPrinterContext, Object> {

	@Override
	public Object visit(BagType bagType, EOLPrinterContext context,
			EolVisitorController<EOLPrinterContext, Object> controller) {
		String result = "";
		//FIXED: the collection type has no contentType at operation declaration
		if (bagType.getContainer() instanceof OperationDefinition || bagType.getContentType() == null) {
			result = "Bag";
		}
		else if (bagType.getContainer() instanceof FormalParameterExpression && bagType.getContainer().getContainer() instanceof OperationDefinition) {
			// FIXED: prevent printing parentheses for a formal parameter type of a collection
			result = "Bag";
		}
		else {
			result += "Bag(" + controller.visit(bagType.getContentType(), context) + ")";
		}
		return result;
	}

}
