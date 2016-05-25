package jbase.tests.util;

import org.eclipse.xtext.xbase.jvmmodel.IJvmModelInferrer;

import jbase.JbaseInjectorProvider;
import jbase.JbaseRuntimeModule;

public class SimpleJvmModelTestInjectorProvider extends JbaseInjectorProvider {

	@Override
	protected JbaseRuntimeModule createRuntimeModule() {
		return new JbaseRuntimeModule() {
			@Override
			public Class<? extends IJvmModelInferrer> bindIJvmModelInferrer() {
				return SimpleJvmModelInferrer.class;
			}
		};
	}

}
