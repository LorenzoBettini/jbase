package jbase.tests;

import static org.junit.Assert.*;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.EcoreUtil2;
import org.eclipse.xtext.common.types.JvmDeclaredType;
import org.eclipse.xtext.scoping.IScope;
import org.eclipse.xtext.testing.util.ParseHelper;
import org.eclipse.xtext.xbase.typesystem.IBatchTypeResolver;
import org.junit.Test;

import jbase.jbase.XJAnonymousClass;
import jbase.jvmmodel.JbaseAnonymousClassUtil;
import jbase.testlanguage.JbaseTestlanguageRuntimeModule;
import jbase.testlanguage.tests.JbaseTestlanguageInjectorProvider;

/** Missing target-platform classes must leave a recoverable model, without inference exceptions. */
public class JbaseAnonymousClassRecoveryTest {
	public static class UnavailableTypes extends JbaseAnonymousClassUtil {
		@Override
		public JvmDeclaredType getSuperTypeNonResolving(final XJAnonymousClass source,
				final IScope scope) {
			return null;
		}
	}

	@Test
	@SuppressWarnings("unchecked")
	public void missingJavaTypeEnvironmentDoesNotThrow() throws Exception {
		final var provider = new JbaseTestlanguageInjectorProvider() {
			@Override
			protected JbaseTestlanguageRuntimeModule createRuntimeModule() {
				return new JbaseTestlanguageRuntimeModule() {
					@SuppressWarnings("unused")
					public Class<? extends JbaseAnonymousClassUtil> bindJbaseAnonymousClassUtil() {
						return UnavailableTypes.class;
					}
				};
			}
		};
		provider.setupRegistry();
		try {
			final var injector = provider.getInjector();
			final ParseHelper<EObject> parser = injector.getInstance(ParseHelper.class);
			final var model = parser.parse("var value = new Missing() {};");
			final var anonymous = EcoreUtil2
				.getAllContentsOfType(model, XJAnonymousClass.class).getFirst();
			assertNotNull(injector.getInstance(IBatchTypeResolver.class).resolveTypes(model)
				.getActualType(anonymous));
		} finally {
			provider.restoreRegistry();
		}
	}
}
