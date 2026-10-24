package jbase.typesystem;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.common.types.JvmGenericType;
import org.eclipse.xtext.common.types.JvmType;
import org.eclipse.xtext.common.types.access.IJvmTypeProvider;
import org.eclipse.xtext.xbase.typesystem.references.LightweightTypeReference;

import com.google.inject.Inject;

/** Resolves the permitted subclasses of a sealed Java type. */
public class JbaseSealedTypeResolver {
	@Inject
	private ClassLoader classLoader;

	@Inject
	private IJvmTypeProvider.Factory typeProviderFactory;

	public JbaseSealedTypeResolver() {
	}

	protected JbaseSealedTypeResolver(final ClassLoader classLoader,
			final IJvmTypeProvider.Factory typeProviderFactory) {
		this.classLoader = classLoader;
		this.typeProviderFactory = typeProviderFactory;
	}

	public List<LightweightTypeReference> permittedTypes(final EObject context,
			final LightweightTypeReference type) {
		if (!(type.getType() instanceof JvmGenericType) || context == null
				|| context.eResource() == null) {
			return List.of();
		}
		final var names = permittedTypeNames(context, type);
		if (names.isEmpty()) {
			return List.of();
		}
		final var typeProvider = typeProviderFactory.findOrCreateTypeProvider(
			context.eResource().getResourceSet());
		final var result = new ArrayList<LightweightTypeReference>();
		for (final var name : names) {
			if (!(typeProvider.findTypeByName(name) instanceof JvmType permittedType)) {
				return List.of();
			}
			result.add(type.getOwner().toLightweightTypeReference(permittedType));
		}
		return List.copyOf(result);
	}

	/**
	 * Returns the fully qualified names of the permitted subclasses of a sealed
	 * Java type.
	 * 
	 * @param context subclasses can use the context to retrieve additional
	 *                information; in the default implementation it is not used
	 * @param type    the Java type for which to retrieve the permitted subclasses
	 * @return a list of fully qualified names of the permitted subclasses, or an
	 *         empty list if the type is not sealed or cannot be loaded
	 */
	protected List<String> permittedTypeNames(final EObject context,
			final LightweightTypeReference type) {
		final Class<?> javaType;
		try {
			javaType = classLoader.loadClass(type.getType().getIdentifier());
		} catch (final ClassNotFoundException exception) {
			return List.of();
		}
		if (!javaType.isSealed()) {
			return List.of();
		}
		return Arrays.stream(javaType.getPermittedSubclasses())
			.map(Class::getName).toList();
	}
}
