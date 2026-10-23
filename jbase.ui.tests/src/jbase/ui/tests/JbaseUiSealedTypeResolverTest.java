package jbase.ui.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Proxy;
import java.util.List;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.xtext.common.types.JvmGenericType;
import org.eclipse.xtext.common.types.TypesFactory;
import org.eclipse.xtext.testing.InjectWith;
import org.eclipse.xtext.testing.XtextRunner;
import org.eclipse.xtext.testing.util.ParseHelper;
import org.eclipse.xtext.xbase.typesystem.references.LightweightTypeReference;
import org.eclipse.xtext.xbase.typesystem.references.StandardTypeReferenceOwner;
import org.eclipse.xtext.xbase.typesystem.util.CommonTypeComputationServices;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.google.inject.Inject;

import jbase.testlanguage.ui.tests.JbaseTestlanguageUiInjectorProvider;
import jbase.ui.typesystem.JbaseUiSealedTypeResolver;

/**
 * Test corner cases of {@link JbaseUiSealedTypeResolver} that are hard to reproduce in a real Eclipse environment.
 */
@RunWith(XtextRunner.class)
@InjectWith(JbaseTestlanguageUiInjectorProvider.class)
public class JbaseUiSealedTypeResolverTest {
	@Inject
	private ParseHelper<EObject> parser;

	@Inject
	private CommonTypeComputationServices typeServices;

	@Inject
	private ExposedResolver resolver;

	@Test
	public void nonSealedTypesAndEmptyPermitsAreHandled() {
		assertTrue(resolver.permittedTypeNames(
			javaType(false, new String[0], null, null, false)).isEmpty());
		assertEquals(List.of(), resolver.permittedTypeNames(
			javaType(true, new String[0], null, null, false)));
	}

	@Test
	public void jdtFailuresAreReportedAsUnavailableCoverage() {
		assertTrue(resolver.permittedTypeNames(
			javaType(true, new String[0], null, null, true)).isEmpty());
	}

	/**
	 * When calling {@link JbaseUiSealedTypeResolver#jbase.ui.typesystem.JbaseUiSealedTypeResolver.permittedTypeNames(EObject, LightweightTypeReference)}
	 * and there's a {@link JavaModelException}, the fallback implementation should be used and return an empty list.
	 */
	@Test
	public void jdtFailuresInContextAreReportedAsUnavailableCoverage() throws Exception {
		final var sourceType = javaType(true, new String[0], null, null, false);
		final var javaProject = javaProject(sourceType, true);
		final var context = parser.parse("Object value = null;");
		final JvmGenericType type = TypesFactory.eINSTANCE.createJvmGenericType();
		type.setPackageName("missing");
		type.setSimpleName("MissingSealedType");
		final var owner = new StandardTypeReferenceOwner(typeServices, context);
		final LightweightTypeReference reference = owner.toLightweightTypeReference(type);

		assertTrue(resolver.permittedTypeNames(context, reference, javaProject).isEmpty());
	}

	@Test
	public void resolvesPermittedTypeNames() {
		final var child = javaType(false, new String[0], null, null, false);
		final var javaProject = javaProject(child);
		final var sourceType = javaType(true, new String[] { "Child" },
			new String[][] { { "sample", "Child" } }, javaProject, false);
		assertEquals(List.of("sample.Child"),
			resolver.permittedTypeNames(sourceType));

		final var defaultPackageType = javaType(true, new String[] { "Child" },
			new String[][] { { "", "Child" } }, javaProject, false);
		assertEquals(List.of("sample.Child"),
			resolver.permittedTypeNames(defaultPackageType));
	}

	@Test
	public void unresolvedOrAmbiguousPermittedTypesAreNotAccepted() throws Exception {
		assertNull(resolver.resolvePermittedType(
			javaType(true, new String[0], null, null, false), "Missing"));
		assertNull(resolver.resolvePermittedType(javaType(true, new String[0],
			new String[][] { { "one", "Child" }, { "two", "Child" } }, null, false),
			"Child"));

		final var sourceType = javaType(true, new String[] { "Missing" },
			new String[][] { { "sample", "Missing" } }, javaProject(null), false);
		assertTrue(resolver.permittedTypeNames(sourceType).isEmpty());
	}

	private static IType javaType(final boolean sealed, final String[] permittedNames,
			final String[][] resolvedNames, final IJavaProject javaProject,
			final boolean failToReadPermittedNames) {
		return (IType) Proxy.newProxyInstance(IType.class.getClassLoader(),
			new Class<?>[] { IType.class }, (proxy, method, arguments) -> switch (method.getName()) {
				case "isSealed" -> sealed;
				case "getPermittedSubtypeNames" -> {
					if (failToReadPermittedNames) {
						throw new JavaModelException(
							new Status(IStatus.ERROR, "jbase.ui.tests", "Test failure"));
					}
					yield permittedNames;
				}
				case "resolveType" -> resolvedNames;
				case "getJavaProject" -> javaProject;
				case "getFullyQualifiedName" ->
					arguments == null || arguments.length == 0
						? "sample.Shape" : "sample.Child";
				case "toString" -> "sample.Shape";
				default -> null;
			});
	}

	private static IJavaProject javaProject(final IType child) {
		return javaProject(child, false);
	}

	private static IJavaProject javaProject(final IType child, final boolean failToFindType) {
		return (IJavaProject) Proxy.newProxyInstance(IJavaProject.class.getClassLoader(),
			new Class<?>[] { IJavaProject.class }, (proxy, method, arguments) -> {
				if (method.getName().equals("findType")) {
					if (failToFindType) {
						throw new JavaModelException(
							new Status(IStatus.ERROR, "jbase.ui.tests", "Test failure"));
					}
					return child;
				} else {
					return null;
				}
			});
	}

	/**
	 * Expose methods for testing.
	 */
	private static final class ExposedResolver extends JbaseUiSealedTypeResolver {
		@Override
		public List<String> permittedTypeNames(EObject context, LightweightTypeReference type) {
			return super.permittedTypeNames(context, type);
		}

		@Override
		protected List<String> permittedTypeNames(EObject context, LightweightTypeReference type,
				IJavaProject javaProject) {
			return super.permittedTypeNames(context, type, javaProject);
		}

		@Override
		public List<String> permittedTypeNames(IType javaType) {
			return super.permittedTypeNames(javaType);
		}

		@Override
		public IType resolvePermittedType(IType sealedType, String name) throws JavaModelException {
			return super.resolvePermittedType(sealedType, name);
		}
	}
}
