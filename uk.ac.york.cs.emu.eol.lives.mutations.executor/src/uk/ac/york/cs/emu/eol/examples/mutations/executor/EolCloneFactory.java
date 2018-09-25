package uk.ac.york.cs.emu.eol.examples.mutations.executor;

import org.eclipse.epsilon.eol.EolModule;

public class EolCloneFactory {

	public static EolModule clone(EolModule base) {
		EolCloneModule copy = new EolCloneModule();
		copy.copyModule(base);
		return copy;
	}
}
