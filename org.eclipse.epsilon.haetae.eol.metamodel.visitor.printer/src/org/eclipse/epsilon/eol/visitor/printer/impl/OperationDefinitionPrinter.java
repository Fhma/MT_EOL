package org.eclipse.epsilon.eol.visitor.printer.impl;

import org.eclipse.epsilon.eol.metamodel.AnyType;
import org.eclipse.epsilon.eol.metamodel.CollectionType;
import org.eclipse.epsilon.eol.metamodel.InvalidType;
import org.eclipse.epsilon.eol.metamodel.MapType;
import org.eclipse.epsilon.eol.metamodel.ModelElementType;
import org.eclipse.epsilon.eol.metamodel.ModelType;
import org.eclipse.epsilon.eol.metamodel.NativeType;
import org.eclipse.epsilon.eol.metamodel.OperationDefinition;
import org.eclipse.epsilon.eol.metamodel.PrimitiveType;
import org.eclipse.epsilon.eol.metamodel.PseudoType;
import org.eclipse.epsilon.eol.metamodel.Type;
import org.eclipse.epsilon.eol.metamodel.visitor.EolVisitorController;
import org.eclipse.epsilon.eol.metamodel.visitor.OperationDefinitionVisitor;
import org.eclipse.epsilon.eol.visitor.printer.context.EOLPrinterContext;

public class OperationDefinitionPrinter extends OperationDefinitionVisitor<EOLPrinterContext, Object>{

	@Override
	public Object visit(OperationDefinition operationDefinition,
			EOLPrinterContext context,
			EolVisitorController<EOLPrinterContext, Object> controller) {
		String result = "";
		if(operationDefinition.getAnnotationBlock() != null)
		{
			result += controller.visit(operationDefinition.getAnnotationBlock(), context);
			context.newline();
		}
		
		result += "operation";
		
		// FIXED: remove the extra space after 'operation' keyword if there is no contextType
		if(operationDefinition.getContextType() != null)
		{
			String context_type = "" + controller.visit(operationDefinition.getContextType(), context);
			if(context_type.length() > 0)
				result+=" "+ context_type + " ";
			else
				result+=" ";
		}
		
		result += controller.visit(operationDefinition.getName(), context) + "(";
		
		if(operationDefinition.getParameters() != null && operationDefinition.getParameters().size() != 0)
		{
			result += controller.visit(operationDefinition.getParameters().get(0), context);
			for(int i = 1; i < operationDefinition.getParameters().size(); i++)
			{
				//FIXED: include a comma ',' after each parameter in multiple parameters operation
				result += ", "+controller.visit(operationDefinition.getParameters().get(i), context);
			}
			result += ")";
		}
		else {
			result += ")";
		}
		if(operationDefinition.getReturnType() != null)
		{
			Type returnType = operationDefinition.getReturnType();
			if (isTypeOfAnyType(returnType)) {
				if (((AnyType)returnType).isDeclared()) {
					result += " : ";
				}
			}
			else {
				result += " : ";
			}
			result += controller.visit(operationDefinition.getReturnType(), context) + " ";
		}
		result += " {" + context.newline();
		context.indent();
		result += controller.visit(operationDefinition.getBody(), context);
		context.outdent();
		result += context.whitespace() + "}";

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
