package jbase.tests.util;

import org.eclipse.xtext.xbase.jvmmodel.IJvmModelInferrer;

import com.google.inject.Guice;
import com.google.inject.Injector;

import jbase.JbaseRuntimeModule;
import jbase.JbaseStandaloneSetup;
import jbase.JbaseInjectorProvider;

public class SimpleJvmModelTestInjectorProvider extends JbaseInjectorProvider {
	@Override
	protected Injector internalCreateInjector() {
		return new SimpleJvmModelTestStandaloneSetup().createInjectorAndDoEMFRegistration();
	}

	public static class SimpleJvmModelTestStandaloneSetup extends JbaseStandaloneSetup {
		@Override
		public Injector createInjector() {
			return Guice.createInjector(new JbaseRuntimeModule() {
				@Override
				public Class<? extends IJvmModelInferrer> bindIJvmModelInferrer() {
					return SimpleJvmModelInferrer.class;
				}
			});
		}
	}
	
}
