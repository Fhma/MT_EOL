package org.eclipse.epsilon.eol.visitor.printer.impl;

import org.eclipse.epsilon.eol.metamodel.AnyType;
import org.eclipse.epsilon.eol.metamodel.CollectionType;
import org.eclipse.epsilon.eol.metamodel.FormalParameterExpression;
import org.eclipse.epsilon.eol.metamodel.InvalidType;
import org.eclipse.epsilon.eol.metamodel.MapType;
import org.eclipse.epsilon.eol.metamodel.ModelElementType;
import org.eclipse.epsilon.eol.metamodel.ModelType;
import org.eclipse.epsilon.eol.metamodel.NativeType;
import org.eclipse.epsilon.eol.metamodel.PrimitiveType;
import org.eclipse.epsilon.eol.metamodel.PseudoType;
import org.eclipse.epsilon.eol.metamodel.Type;
import org.eclipse.epsilon.eol.metamodel.visitor.EolVisitorController;
import org.eclipse.epsilon.eol.metamodel.visitor.FormalParameterExpressionVisitor;
import org.eclipse.epsilon.eol.visitor.printer.context.EOLPrinterContext;

public class FormalParameterExpressionPrinter extends FormalParameterExpressionVisitor<EOLPrinterContext, Object>{

	@Override
	public Object visit(FormalParameterExpression formalParameterExpression,
			EOLPrinterContext context,
			EolVisitorController<EOLPrinterContext, Object> controller) {
		String result = "";
		result += controller.visit(formalParameterExpression.getName(), context);
		Type resolvedType = formalParameterExpression.getResolvedType();
		if(resolvedType != null)
		{
			/*
			if (resolvedType instanceof AnyType) {
				if (((AnyType)resolvedType).isDeclared()) {
					result += " : ";
				}
			}*/
			// FIXED: fix the drop of ':' between a variable and its type
			if(isTypeOfAnyType(resolvedType)) {
				if (((AnyType)resolvedType).isDeclared()) {
					result += " : ";
				}
			}
			else {
				result += " : ";
			}
			result += controller.visit(formalParameterExpression.getResolvedType(), context); 
		}
		
		if (formalParameterExpression.isInBrackets()) {
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
