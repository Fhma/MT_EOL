package uk.ac.york.cs.emu.eol.examples.executor;

import org.eclipse.epsilon.eol.EolModule;

public class EolCloneFactory {

	public static EolModule clone(EolModule base) throws Exception {
		EolCloneModule copy = new EolCloneModule();
		copy.copyModule(base);
		return copy;
	}
}
