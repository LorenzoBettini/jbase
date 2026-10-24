package jbase.ui.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Proxy;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
import jbase.ui.jdt.JbaseJdtHelper;

/** Tests permitted subtype lookup in {@link JbaseJdtHelper}, including JDT model failures. */
@RunWith(XtextRunner.class)
@InjectWith(JbaseTestlanguageUiInjectorProvider.class)
public class JbaseJdtHelperTest {
	@Inject
	private ParseHelper<EObject> parser;

	@Inject
	private CommonTypeComputationServices typeServices;

	@Test
	public void returnsEmptyListForNonSealedTypesAndTypesWithoutPermits() {
		assertTrue(JbaseJdtHelper.permittedTypeNames(
			javaType(false, new String[0], null, null, false)).isEmpty());
		assertEquals(List.of(), JbaseJdtHelper.permittedTypeNames(
			javaType(true, new String[0], null, null, false)));
	}

	@Test
	public void returnsEmptyListWhenJdtCannotReadPermittedSubtypes() {
		assertTrue(JbaseJdtHelper.permittedTypeNames(
			javaType(true, new String[0], null, null, true)).isEmpty());
	}

	@Test
	public void usesFallbackWhenJdtCannotFindTheType() throws Exception {
		final var sourceType = javaType(true, new String[0], null, null, false);
		final var javaProject = javaProject(sourceType, true);
		final var context = parser.parse("Object value = null;");
		final JvmGenericType type = TypesFactory.eINSTANCE.createJvmGenericType();
		type.setPackageName("missing");
		type.setSimpleName("MissingSealedType");
		final var owner = new StandardTypeReferenceOwner(typeServices, context);
		final LightweightTypeReference reference = owner.toLightweightTypeReference(type);

		assertTrue(JbaseJdtHelper.permittedTypeNames(context, reference, javaProject, (c1, c2) -> Collections.emptyList()).isEmpty());
	}

	@Test
	public void returnsPermittedSubtypeNamesFromJavaProject() throws Exception {
		final var child = javaType(false, new String[0], null, null, false);
		final Map<String, IType> typesByName = new HashMap<>();
		final var javaProject = javaProjectForTypes(typesByName);
		final var sourceType = javaType(true, new String[] { "Child" },
			new String[][] { { "sample", "Child" } }, javaProject, false);
		typesByName.put("sample.Shape", sourceType);
		typesByName.put("sample.Child", child);
		final var context = parser.parse("Object value = null;");
		final JvmGenericType type = TypesFactory.eINSTANCE.createJvmGenericType();
		type.setPackageName("sample");
		type.setSimpleName("Shape");
		final var owner = new StandardTypeReferenceOwner(typeServices, context);
		final LightweightTypeReference reference = owner.toLightweightTypeReference(type);

		assertEquals(List.of("sample.Child"),
			JbaseJdtHelper.permittedTypeNames(context, reference, javaProject,
				(c1, c2) -> Collections.emptyList()));
	}

	@Test
	public void returnsResolvedNamesForPermittedTypesIncludingDefaultPackage() {
		final var child = javaType(false, new String[0], null, null, false);
		final var javaProject = javaProject(child);
		final var sourceType = javaType(true, new String[] { "Child" },
			new String[][] { { "sample", "Child" } }, javaProject, false);
		assertEquals(List.of("sample.Child"),
				JbaseJdtHelper.permittedTypeNames(sourceType));

		final var defaultPackageType = javaType(true, new String[] { "Child" },
			new String[][] { { "", "Child" } }, javaProject, false);
		assertEquals(List.of("sample.Child"),
				JbaseJdtHelper.permittedTypeNames(defaultPackageType));
	}

	@Test
	public void returnsEmptyForMissingOrAmbiguousTypesAndEmptyListForUnresolvedPermits()
			throws Exception {
		assertNull(JbaseJdtHelper.resolveType(
			javaType(true, new String[0], null, null, false), "Missing"));
		assertNull(JbaseJdtHelper.resolveType(javaType(true, new String[0],
			new String[][] { { "one", "Child" }, { "two", "Child" } }, null, false),
			"Child"));

		final var sourceType = javaType(true, new String[] { "Missing" },
			new String[][] { { "sample", "Missing" } }, javaProject(null), false);
		assertTrue(JbaseJdtHelper.permittedTypeNames(sourceType).isEmpty());
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

	private static IJavaProject javaProjectForTypes(final Map<String, IType> typesByName) {
		return (IJavaProject) Proxy.newProxyInstance(IJavaProject.class.getClassLoader(),
			new Class<?>[] { IJavaProject.class }, (proxy, method, arguments) -> {
				if (method.getName().equals("findType")) {
					return typesByName.get(arguments[0]);
				} else {
					return null;
				}
			});
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

}
