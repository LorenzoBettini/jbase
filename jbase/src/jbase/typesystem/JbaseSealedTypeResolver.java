package jbase.typesystem;

import java.util.ArrayList;
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
		return java.util.Arrays.stream(javaType.getPermittedSubclasses())
			.map(Class::getName).toList();
	}
}
