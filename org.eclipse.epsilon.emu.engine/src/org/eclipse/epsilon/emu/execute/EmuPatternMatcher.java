/**
 * 
 * @author Faisal Alhwikem
 * @version 1.0
 * @since March-2017
 */

package org.eclipse.epsilon.emu.execute;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.epsilon.common.parse.AST;
import org.eclipse.epsilon.emc.mutant.IMutant;
import org.eclipse.epsilon.emc.mutant.IProperty;
import org.eclipse.epsilon.emu.EmuModule;
import org.eclipse.epsilon.emu.mutation.IMutationChecker;
import org.eclipse.epsilon.emu.mutation.execute.MutationCheckerImpl;
import org.eclipse.epsilon.eol.dom.Annotation;
import org.eclipse.epsilon.eol.exceptions.EolInternalException;
import org.eclipse.epsilon.eol.exceptions.EolRuntimeException;
import org.eclipse.epsilon.eol.execute.context.FrameType;
import org.eclipse.epsilon.eol.execute.context.IEolContext;
import org.eclipse.epsilon.eol.execute.context.Variable;
import org.eclipse.epsilon.epl.dom.Pattern;
import org.eclipse.epsilon.epl.execute.PatternMatch;
import org.eclipse.epsilon.epl.execute.PatternMatchModel;
import org.eclipse.epsilon.epl.execute.PatternMatcher;

public class EmuPatternMatcher extends PatternMatcher {

    protected static final String MUTATION_ACTION_ANNOTATION = "action";
    protected static final String PROPERTY_ANNOTATION = "property";
    protected static final String ROLE_ANNOTATION = "role";
    public static final String INVALID_MUTATION = "N/A";

    private IMutationChecker mutationGeneratorImpl = null;
    private IProperty property;

    public PatternMatchModel match(EmuModule module) throws Exception {
	frame = null;
	IEolContext context = module.getContext();
	PatternMatchModel model = new PatternMatchModel();

	if (module.getMaximumLevel() > 0) {
	    model.setName(module.getPatternMatchModelName());
	    context.getModelRepository().addModel(model);
	}

	model.setPatterns(module.getPatterns());

	for (int level = 0; level <= module.getMaximumLevel(); level++) {
	    for (Pattern pattern : module.getPatterns()) {
		// Check annotations given by this pattern
		checkAnnotations(pattern);

		if (pattern.getLevel() == level) {
		    List<PatternMatch> pattern_matches = match(pattern, context);
		    for (PatternMatch match : pattern_matches) {
			model.addMatch(match);
		    }
		}
	    }

	    if (module.getMutantsDir() == null)
		throw new EolRuntimeException("No mutation dircotry found.");

	    int index;
	    String location = module.getMutantsDir().toURI().toString();

	    for (PatternMatch match : model.getMatches()) {
		if (match.getPattern().getLevel() == level) {

		    prepareForMutation(module, match, context);

		    Object valid_mutant = null;
		    String mu_action = getAnnotationValue(match.getPattern(), MUTATION_ACTION_ANNOTATION, context);

		    Object old_value = null;
		    if (property.isMultiValued()) {
			List<Object> toList = new ArrayList<Object>();
			@SuppressWarnings("unchecked")
			Collection<Object> collection = (Collection<Object>) property.getPropertyValue();
			for (Object obj : collection)
			    toList.add(obj);
			old_value = (Object) toList;
		    } else
			old_value = property.getPropertyValue();

		    Object res = getMutationChecker().checkConditions(property, old_value, mu_action);

		    if (res.equals(IMutationChecker.NOT_MUTATABLE))
			throw new IllegalArgumentException("Property [" + property.getName() + "] is unable to be mutated");

		    String operatorName = getConcreteOperator(mu_action) + "_" + property.getContainerName() + "_" + property.getName();

		    AST do_ = match.getPattern().getDoAst();

		    if (do_ != null && res.equals(IMutationChecker.VALID)) {

			context.getFrameStack().enterLocal(FrameType.UNPROTECTED, do_);
			for (String componentName : match.getRoleBindings().keySet()) {
			    context.getFrameStack().put(Variable.createReadOnlyVariable(componentName, match.getRoleBinding(componentName)));
			}

			context.getModelRepository().getTransactionSupport().startTransaction();

			try {
			    context.getExecutorFactory().executeAST(do_, context);
			} catch (EolInternalException e) {
			    context.getModelRepository().getTransactionSupport().rollbackTransaction();
			    context.getFrameStack().leaveLocal(do_);
			    module.getOperatorsMatrix().getValue(operatorName).add(INVALID_MUTATION);
			    continue;
			} catch (Exception e) {
			    throw new Exception(e);
			}

			Object new_value = property.getModel().getPropertyGetter().invoke(property.getContainer(), property.getName());

			valid_mutant = getMutationChecker().checkConditions(property, old_value, new_value, mu_action);

			if (valid_mutant == IMutationChecker.VALID) {
			    index = module.getOperatorsMatrix().getValue(operatorName).size() + 1;
			    String mutant = location + operatorName + "_" + index + ".xmi";
			    property.getModel().store(mutant);
			    module.getOperatorsMatrix().getValue(operatorName).add(mutant);
			}
			context.getModelRepository().getTransactionSupport().rollbackTransaction();
			context.getFrameStack().leaveLocal(do_);
		    }
		    if (valid_mutant == null || (valid_mutant != null && valid_mutant.equals(IMutationChecker.INVALID))) {
			module.getOperatorsMatrix().getValue(operatorName).add(INVALID_MUTATION);
		    }
		}
	    }
	}
	return model;
    }

    private void prepareForMutation(EmuModule module, PatternMatch match, IEolContext context) throws Exception {

	// check which role has the target instance and type
	Pattern ptr = match.getPattern();
	String property_name = getAnnotationValue(ptr, PROPERTY_ANNOTATION, context);

	Set<Map.Entry<String, Object>> bindings = match.getRoleBindings().entrySet();
	String role = getAnnotationValue(ptr, ROLE_ANNOTATION, context);
	String msg = "Unable to find the target binding role for property [" + property_name + "]";
	msg += " in pattern [" + ptr.getName() + "] in file [" + module.getSourceFile().getPath() + "].";

	if (bindings.size() > 1 && role == null)
	    throw new IllegalArgumentException(msg);

	Iterator<Map.Entry<String, Object>> it = bindings.iterator();
	boolean found = false;
	Object bindingRole = null;
	IMutant owning_model = null;
	while (it.hasNext() && !found) {

	    Map.Entry<String, Object> pair = it.next();
	    owning_model = (IMutant) module.getContext().getModelRepository().getOwningModel(pair.getValue());
	    boolean isContaining = false;
	    if (role != null) {
		if (pair.getKey().equals(role))
		    isContaining = owning_model.getPropertyGetter().hasProperty(pair.getValue(), property_name);
	    } else
		isContaining = true;

	    if (isContaining) {
		bindingRole = pair.getValue();
		found = true;
	    }
	}

	if (!found)
	    throw new IllegalArgumentException(msg);

	property = owning_model.getProperty(bindingRole, property_name);
	property.prepare(owning_model, bindingRole);
    }

    private void checkAnnotations(Pattern pattern) throws Exception {
	if (!pattern.hasAnnotation(MUTATION_ACTION_ANNOTATION))
	    throw new Exception("Mutation action is not specified.");
	if (!pattern.hasAnnotation(PROPERTY_ANNOTATION))
	    throw new Exception("Property name is not specified.");

	for (Annotation anno : pattern.getAnnotationBlock().getAnnotations()) {
	    if (anno.getName().equals(MUTATION_ACTION_ANNOTATION) && !anno.hasValue())
		throw new Exception("Value of mutation action is not specified.");
	    if (anno.getName().equals(PROPERTY_ANNOTATION) && !anno.hasValue())
		throw new Exception("Value of target property is not specified.");
	}
    }

    public IMutationChecker getMutationChecker() {
	if (mutationGeneratorImpl == null)
	    mutationGeneratorImpl = new MutationCheckerImpl();
	return mutationGeneratorImpl;
    }

    private String getAnnotationValue(Pattern pattern, String name, final IEolContext context) throws EolRuntimeException {
	for (Annotation anno : pattern.getAnnotationBlock().getAnnotations()) {
	    if (anno.getName().equals(name))
		return anno.getValue(context).toString();
	}
	return null;
    }

    private String getConcreteOperator(String mu_action) {
	String name = "CMO-";
	if (property.getUpperBound() == 1) {
	    name = name + "S-";
	} else
	    name = name + "M-";
	if (mu_action.equals("add")) {
	    name = name + "ADD";
	} else if (mu_action.equals("delete")) {
	    name = name + "DEL";
	} else {
	    name = name + "REP";
	}
	return name;
    }
}
