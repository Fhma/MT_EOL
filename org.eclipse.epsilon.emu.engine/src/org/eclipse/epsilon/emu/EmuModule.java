/**
 * 
 * @author Faisal Alhwikem
 * @version 1.0
 * @since March-2017
 */
package org.eclipse.epsilon.emu;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.eclipse.epsilon.common.parse.AST;
import org.eclipse.epsilon.common.parse.problem.ParseProblem;
import org.eclipse.epsilon.emu.execute.EmuPatternMatcher;
import org.eclipse.epsilon.emu.mutation.matrix.OMatrix;
import org.eclipse.epsilon.eol.exceptions.EolRuntimeException;
import org.eclipse.epsilon.eol.execute.context.IEolContext;
import org.eclipse.epsilon.epl.EplModule;
import org.eclipse.epsilon.epl.dom.Domain;
import org.eclipse.epsilon.epl.dom.Pattern;
import org.eclipse.epsilon.epl.dom.Role;
import org.eclipse.epsilon.epl.parse.EplParser;
import org.eclipse.epsilon.eol.dom.ExecutableBlock;

public class EmuModule extends EplModule {

	protected File mutants_dir;
	protected OMatrix operatorsMatrix;

	@Override
	public AST adapt(AST cst, AST parentAst) {
		switch (cst.getType()) {
		case EplParser.PATTERN:
			return new Pattern();
		case EplParser.GUARD:
			return new ExecutableBlock<Boolean>(Boolean.class);
		case EplParser.DOMAIN:
			return new Domain();
		case EplParser.ROLE:
			return new Role();
		}
		return super.adapt(cst, parentAst);
	}

	@Override
	public Object execute() throws EolRuntimeException {
		checkUniqueness();
		prepareContext(context);
		execute(getPre(), context);
		EmuPatternMatcher patternMatcher = new EmuPatternMatcher();
		try {
			patternMatcher.match(this);
		} catch (Exception ex) {
			EolRuntimeException.propagate(ex);
		}
		execute(getPost(), context);
		return patternMatcher;
	}

	public OMatrix getOperatorsMatrix() {
		if (operatorsMatrix == null) {
			if (getMutantsDir() != null) {
				operatorsMatrix = new OMatrix(getMutantsDir().getAbsolutePath() + File.separatorChar + getMutantsDir().getName());
			}
		}
		return operatorsMatrix;
	}

	public File getMutantsDir() {
		if (mutants_dir == null) {
			File src = this.getSourceFile();
			if (src != null) {
				String path = src.getAbsolutePath();
				path = path.substring(0, path.length() - 4);
				mutants_dir = new File(path + "_mutants" + File.separatorChar);
				mutants_dir.mkdir();
			}
		}
		return mutants_dir;
	}

	public void saveOperatorsMatrix() throws IOException {
		getOperatorsMatrix().saveMatrix();
	}

	@Override
	public boolean isRepeatWhileMatches() {
		return false;
	}

	public void checkUniqueness() throws EolRuntimeException {
		List<String> names = new ArrayList<String>();
		for (Pattern pt : this.getPatterns()) {
			if (names.contains(pt.getName()))
				throw new EolRuntimeException("Dublicate pattern names [" + pt.getName() + "] is found in [" + this.getSourceFile().getPath() + "]");
			names.add(pt.getName());
		}
	}

	public void setMutants_dir(File mutants_dir) {
		this.mutants_dir = mutants_dir;
	}

	public void setOperatorsMatrix(OMatrix operatorsMatrix) {
		this.operatorsMatrix = operatorsMatrix;
	}

	@Override
	public IEolContext getContext() {
		return super.getContext();
	}

	@Override
	public boolean parse(File file) throws Exception {
		return super.parse(file);
	}

	@Override
	public List<ParseProblem> getParseProblems() {
		return super.getParseProblems();
	}

	@Override
	public boolean parse(String string) throws Exception {
		throw new EolRuntimeException("Parsing a string is not allowed with EMU.");
	}
}
