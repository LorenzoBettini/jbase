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
 * Provides helpers for reading permitted subclasses from the Eclipse JDT model.
 *
 * @author Lorenzo Bettini
 */
public class JbaseJdtHelper {

	private static final Logger LOGGER = Logger.getLogger(JbaseJdtHelper.class);

	/** Prevents instantiation. */
	private JbaseJdtHelper() {
		// only static methods
	}

	/**
	 * Returns the permitted subtype names for the Java type represented by {@code type}.
	 * If the project cannot look up the Java type, delegates to {@code fallback}.
	 *
	 * @param context the expression context passed to the fallback
	 * @param type the type whose permitted subtypes are requested
	 * @param javaProject the Java project used to look up the type
	 * @param fallback the fallback used when the JDT lookup throws a model exception
	 * @return the permitted subtype names, or the fallback result when lookup fails
	 */
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

	/**
	 * Returns the fully qualified names of a sealed type's permitted subtypes.
	 * Returns an empty list when the type is not sealed, a permitted subtype cannot be
	 * resolved uniquely, or the JDT model cannot provide the required information.
	 *
	 * @param javaType the JDT type to inspect
	 * @return the fully qualified names of the permitted subtypes
	 */
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

	/**
	 * Resolves a permitted subtype name relative to the given JDT type.
	 *
	 * @param type the type used as the resolution context
	 * @param name the subtype name to resolve
	 * @return the resolved type, or {@code null} if the name has no unique resolution
	 * @throws JavaModelException if the JDT model fails while resolving or looking up the type
	 */
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
