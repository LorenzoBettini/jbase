package jbase.ui.typesystem;

import java.util.List;

import org.apache.log4j.Logger;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.xtext.common.types.access.jdt.IJavaProjectProvider;
import org.eclipse.xtext.xbase.typesystem.references.LightweightTypeReference;

import com.google.inject.Inject;

import jbase.typesystem.JbaseSealedTypeResolver;

/** Reads permitted subclasses from the JDT model used by the IDE type provider. */
public class JbaseUiSealedTypeResolver extends JbaseSealedTypeResolver {
	private static final Logger LOGGER = Logger.getLogger(JbaseUiSealedTypeResolver.class);

	@Inject
	private IJavaProjectProvider javaProjectProvider;

	@Override
	protected List<String> permittedTypeNames(final EObject context,
			final LightweightTypeReference type) {
		final var resource = type.getType().eResource();
		final var javaProject = javaProjectProvider.getJavaProject(resource.getResourceSet());
		try {
			final var javaType = javaProject.findType(
					type.getType().getQualifiedName('.'));
			return permittedTypeNames(javaType);
		} catch (JavaModelException e) {
			return super.permittedTypeNames(context, type);
		}
	}

	protected List<String> permittedTypeNames(final IType javaType) {
		try {
			if (!javaType.isSealed()) {
				return List.of();
			}
			final var names = new java.util.ArrayList<String>();
			for (final var name : javaType.getPermittedSubtypeNames()) {
				final var permittedType = resolvePermittedType(javaType, name);
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

	protected IType resolvePermittedType(final IType sealedType, final String name)
			throws JavaModelException {
		final var resolved = sealedType.resolveType(name);
		if (resolved == null || resolved.length != 1) {
			return null;
		}
		final var packageName = resolved[0][0];
		final var qualifiedName = packageName.isEmpty() ? resolved[0][1]
			: packageName + "." + resolved[0][1];
		return sealedType.getJavaProject().findType(qualifiedName);
	}
}
