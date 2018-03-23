package org.eclipse.epsilon.eol.visitor.printer.impl;

import org.eclipse.epsilon.eol.metamodel.Block;
import org.eclipse.epsilon.eol.metamodel.ExpressionOrStatementBlock;
import org.eclipse.epsilon.eol.metamodel.Statement;
import org.eclipse.epsilon.eol.metamodel.visitor.EolVisitorController;
import org.eclipse.epsilon.eol.metamodel.visitor.ExpressionOrStatementBlockVisitor;
import org.eclipse.epsilon.eol.visitor.printer.context.EOLPrinterContext;

public class ExpressionOrStatementBlockPrinter extends ExpressionOrStatementBlockVisitor<EOLPrinterContext, Object>{

	@Override
	public Object visit(ExpressionOrStatementBlock expressionOrStatementBlock,
			EOLPrinterContext context,
			EolVisitorController<EOLPrinterContext, Object> controller) {
		String result = "";
		if (expressionOrStatementBlock.getBlock() != null) {
			Block block = expressionOrStatementBlock.getBlock();
			for(Statement stmt: block.getStatements())
			{
				result += controller.visit(stmt, context);
				result += context.newline();
			}
		}
		else if (expressionOrStatementBlock.getExpression() != null) {
			// FIXED: fix whitespaces
			// FIXED: in case of a single statement, the statement lack of ';' at the end
			result += context.whitespace();
			result += controller.visit(expressionOrStatementBlock.getExpression(), context);
			result +=";";
			result += context.newline();
		}

		return result;
	}

}
