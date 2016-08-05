package jbase.tests.util;

import org.eclipse.xtext.util.JavaVersion;
import org.eclipse.xtext.xbase.compiler.OnTheFlyJavaCompiler2;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import jbase.testlanguage.JbaseTestlanguageRuntimeModule;
import jbase.testlanguage.tests.JbaseTestlanguageInjectorProvider;

public class Java7JbaseTestlanguageInjectorProvider extends JbaseTestlanguageInjectorProvider {

	@Singleton
	private static class Java7OnTheFlyJavaCompiler2 extends OnTheFlyJavaCompiler2 {
		@Inject
		public Java7OnTheFlyJavaCompiler2(ClassLoader scope) {
			super(scope, JavaVersion.JAVA7);
		}
	}

	@Override
	protected JbaseTestlanguageRuntimeModule createRuntimeModule() {
		return new JbaseTestlanguageRuntimeModule() {
			@Override
			public ClassLoader bindClassLoaderToInstance() {
				// in order to access example annotations in maven build
				return JbaseTestlanguageInjectorProvider.class.getClassLoader();
			}

			@SuppressWarnings("unused")
			public Class<? extends OnTheFlyJavaCompiler2> bindOnTheFlyJavaCompiler2() {
				return Java7OnTheFlyJavaCompiler2.class;
			}
		};
	}
}
