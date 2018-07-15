package uk.ac.york.cs.emu.eol.examples.mutations.executor;

import org.eclipse.epsilon.common.parse.AST;
import org.eclipse.epsilon.eol.execute.context.IEolContext;
import org.eclipse.epsilon.eol.execute.control.ExecutionController;

public class MutationExecutionController implements ExecutionController {

	private boolean terminated = false;

	@Override
	public void control(AST ast, IEolContext context) {
		// TODO Auto-generated method stub

	}

	@Override
	public void done(AST ast, IEolContext context) {
		// TODO Auto-generated method stub

	}

	@Override
	public boolean isTerminated() {
		return terminated;
	}
	
	public void terminate() {
		terminated = true;
	}

	@Override
	public void report(IEolContext context) {
		// TODO Auto-generated method stub

	}

	@Override
	public void dispose() {
		terminated = true;
	}

}
