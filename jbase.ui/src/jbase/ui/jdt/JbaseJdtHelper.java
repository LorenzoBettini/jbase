package jbase.ui.jdt;

import java.util.List;
import java.util.function.BiFunction;

import org.apache.log4j.Logger;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.xtext.xbase.typesystem.references.LightweightTypeReference;

/**
 * Helper class for JDT-related functionality.
 * 
 * @author Lorenzo Bettini
 */
public class JbaseJdtHelper {

	private static final Logger LOGGER = Logger.getLogger(JbaseJdtHelper.class);

	private JbaseJdtHelper() {
		// only static methods
	}

	public static List<String> permittedTypeNames(final EObject context, final LightweightTypeReference type,
			final IJavaProject javaProject, final BiFunction<EObject, LightweightTypeReference, List<String>> fallback) {
		try {
			final var javaType = javaProject.findType(
					type.getType().getQualifiedName('.'));
			return permittedTypeNames(javaType);
		} catch (JavaModelException e) {
			return fallback.apply(context, type);
		}
	}

	public static List<String> permittedTypeNames(final IType javaType) {
		try {
			if (!javaType.isSealed()) {
				return List.of();
			}
			final var names = new java.util.ArrayList<String>();
			for (final var name : javaType.getPermittedSubtypeNames()) {
				final var permittedType = resolveType(javaType, name);
				if (permittedType == null) {
					return List.of();
				}
				names.add(permittedType.getFullyQualifiedName('$'));
			}
			return List.copyOf(names);
		} catch (final JavaModelException exception) {
			LOGGER.error("Could not read permitted subclasses of " + javaType.getFullyQualifiedName(),
				exception);
			return List.of();
		}
	}

	public static IType resolveType(final IType type, final String name)
			throws JavaModelException {
		final var resolved = type.resolveType(name);
		if (resolved == null || resolved.length != 1) {
			return null;
		}
		final var packageName = resolved[0][0];
		final var qualifiedName = packageName.isEmpty() ? resolved[0][1]
			: packageName + "." + resolved[0][1];
		return type.getJavaProject().findType(qualifiedName);
	}
}
