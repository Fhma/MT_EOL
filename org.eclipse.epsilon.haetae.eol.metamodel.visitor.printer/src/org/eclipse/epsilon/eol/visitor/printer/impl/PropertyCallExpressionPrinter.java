package org.eclipse.epsilon.eol.visitor.printer.impl;

import org.eclipse.epsilon.eol.visitor.printer.context.EOLPrinterContext;

import org.eclipse.epsilon.eol.metamodel.PropertyCallExpression;
import org.eclipse.epsilon.eol.metamodel.visitor.EolVisitorController;
import org.eclipse.epsilon.eol.metamodel.visitor.PropertyCallExpressionVisitor;

public class PropertyCallExpressionPrinter extends PropertyCallExpressionVisitor<EOLPrinterContext, Object> {

    @Override
    public Object visit(PropertyCallExpression propertyCallExpression, EOLPrinterContext context, EolVisitorController<EOLPrinterContext, Object> controller) {
	String result = "";
	if (propertyCallExpression.getTarget() != null) {
	    result += controller.visit(propertyCallExpression.getTarget(), context);
	}

	result += propertyCallExpression.isArrow() ? "->" : ".";

	// remove ~ from name if extended = false
	if (!propertyCallExpression.isExtended() && propertyCallExpression.getProperty().getName().startsWith("~")) {
	    String name = propertyCallExpression.getProperty().getName();
	    name = name.replace("~", "");
	    propertyCallExpression.getProperty().setName(name);
	}

	if (propertyCallExpression.getProperty() != null) {
	    result += controller.visit(propertyCallExpression.getProperty(), context);
	}

	if (propertyCallExpression.isInBrackets()) {
	    result = "(" + result + ")";
	}

	return result;
    }

}
