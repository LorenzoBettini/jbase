package org.eclipse.xtext.example.domainmodel.tests;

import org.eclipse.xtext.example.domainmodel.DomainmodelRuntimeModule;
import org.eclipse.xtext.util.JavaVersion;
import org.eclipse.xtext.xbase.testing.OnTheFlyJavaCompiler2;

import com.google.inject.Inject;
import com.google.inject.Singleton;

public class Java21InjectorProvider extends DomainmodelInjectorProvider {

	@Singleton
	private static class Java21OnTheFlyJavaCompiler2 extends OnTheFlyJavaCompiler2 {
		@Inject
		public Java21OnTheFlyJavaCompiler2(ClassLoader scope) {
			super(scope, JavaVersion.JAVA21);
		}
	}

	@Override
	protected DomainmodelRuntimeModule createRuntimeModule() {
		return new DomainmodelRuntimeModule() {
			// make it work also with Maven/Tycho and OSGI
			// see https://bugs.eclipse.org/bugs/show_bug.cgi?id=493672
			// allows for bindClassLoaderToInstance to get the class loader of the bundle
			// containing the instance of the injector provider (possibly inherited)
			@Override
			public ClassLoader bindClassLoaderToInstance() {
				return Java21InjectorProvider.this.getClass()
						.getClassLoader();
			}

			@SuppressWarnings("unused")
			public Class<? extends OnTheFlyJavaCompiler2> bindOnTheFlyJavaCompiler2() {
				return Java21OnTheFlyJavaCompiler2.class;
			}
		};
	}

}