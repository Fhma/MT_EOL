package org.eclipse.epsilon.eol.visitor.printer.impl;

import org.eclipse.epsilon.eol.metamodel.ExpressionOrStatementBlock;
import org.eclipse.epsilon.eol.metamodel.IfStatement;
import org.eclipse.epsilon.eol.metamodel.visitor.EolVisitorController;
import org.eclipse.epsilon.eol.metamodel.visitor.IfStatementVisitor;
import org.eclipse.epsilon.eol.visitor.printer.context.EOLPrinterContext;

public class IfStatementPrinter extends IfStatementVisitor<EOLPrinterContext, Object>{

	@Override
	public Object visit(IfStatement ifStatement, EOLPrinterContext context,
			EolVisitorController<EOLPrinterContext, Object> controller) {
		String result = "";
		result += context.whitespace();
		result += "if(" + controller.visit(ifStatement.getCondition(), context) + ") {";
		result += context.newline();
		context.indent();
		result += controller.visit(ifStatement.getIfBody(), context);
		context.outdent();
		//result += context.newline();
		result += context.whitespace() + "}";

		// FIXED: print elseIfBodies
		if(ifStatement.getElseIfBodies().size()>0)
		{
			for(ExpressionOrStatementBlock else_if:ifStatement.getElseIfBodies()) {
				result += context.newline();
				result += context.whitespace();
				result += "else if(" + controller.visit(else_if.getCondition(), context) + ") {";
				result += context.newline();
				context.indent();
				result += controller.visit(else_if.getBlock(), context);
				context.outdent();
				result += context.whitespace() + "}";
			}
		}
		
		if (ifStatement.getElseBody() != null) {
			result += context.newline() + context.whitespace() + "else {" + context.newline();
			context.indent();
			result += controller.visit(ifStatement.getElseBody(), context);
			//result += context.newline();
			context.outdent();
			result += context.whitespace() + "}";
		}
		
		return result;
	}

}
