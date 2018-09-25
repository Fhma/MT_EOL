package uk.ac.york.cs.emu.eol.examples.executor;

import org.eclipse.epsilon.eol.EolModule;

public class EolCloneModule extends EolModule {

	public EolCloneModule() {
		super();
	}

	public void copyModule(EolModule module) throws Exception {
		this.ast = module.getAst();
		this.main = module.getMain();
		this.imports.addAll(module.getImports());
		this.declaredModelDeclarations.addAll(module.getDeclaredModelDeclarations());
		this.declaredOperations.addAll(module.getDeclaredOperations());
	}
}
