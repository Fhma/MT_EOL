package org.eclipse.epsilon.eol.visitor.printer.impl;

import org.eclipse.epsilon.eol.metamodel.AnyType;
import org.eclipse.epsilon.eol.metamodel.CollectionType;
import org.eclipse.epsilon.eol.metamodel.InvalidType;
import org.eclipse.epsilon.eol.metamodel.MapType;
import org.eclipse.epsilon.eol.metamodel.ModelElementType;
import org.eclipse.epsilon.eol.metamodel.ModelType;
import org.eclipse.epsilon.eol.metamodel.NativeType;
import org.eclipse.epsilon.eol.metamodel.PrimitiveType;
import org.eclipse.epsilon.eol.metamodel.PseudoType;
import org.eclipse.epsilon.eol.metamodel.Type;
import org.eclipse.epsilon.eol.metamodel.VariableDeclarationExpression;
import org.eclipse.epsilon.eol.metamodel.visitor.EolVisitorController;
import org.eclipse.epsilon.eol.metamodel.visitor.VariableDeclarationExpressionVisitor;
import org.eclipse.epsilon.eol.visitor.printer.context.EOLPrinterContext;

public class VariableDeclarationExpressionPrinter extends VariableDeclarationExpressionVisitor<EOLPrinterContext, Object>{

	@Override
	public Object visit(
			VariableDeclarationExpression variableDeclarationExpression,
			EOLPrinterContext context,
			EolVisitorController<EOLPrinterContext, Object> controller) {
		
		String result = "var";
		
		result += " " + controller.visit(variableDeclarationExpression.getName(), context);
		
		Type resolvedType = variableDeclarationExpression.getResolvedType();
		
		if (resolvedType != null) {
			// FIXED: solve the bug of including ':' between the variable name and type
			if (isTypeOfAnyType(resolvedType)) {
				if (((AnyType) resolvedType).isDeclared()) {
					result += " : ";
				}
			}
			else {
				result += " : ";
			}
			if (variableDeclarationExpression.isCreate()) {
				result += "new ";
			}
			result += controller.visit(variableDeclarationExpression.getResolvedType(), context);
		}
		
		if (variableDeclarationExpression.isInBrackets()) {
			result = "(" + result + ")";
		}
		
		return result;
	}
	
	private boolean isTypeOfAnyType(Type type)
	{
		if(type instanceof MapType || type instanceof CollectionType  || type instanceof NativeType || type instanceof PrimitiveType)
			return false;
		if(type instanceof ModelElementType || type instanceof ModelType || type instanceof PseudoType || type instanceof InvalidType)
			return false;
		return true;
	}
}
