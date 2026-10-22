package jbase.typesystem;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

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

	public Optional<List<LightweightTypeReference>> permittedTypes(final EObject context,
			final LightweightTypeReference type) {
		if (!(type.getType() instanceof JvmGenericType) || context == null
				|| context.eResource() == null) {
			return Optional.empty();
		}
		final var names = permittedTypeNames(context, type);
		if (names.isEmpty() || names.get().isEmpty()) {
			return Optional.empty();
		}
		final var typeProvider = typeProviderFactory.findOrCreateTypeProvider(
			context.eResource().getResourceSet());
		final var result = new ArrayList<LightweightTypeReference>();
		for (final var name : names.get()) {
			if (!(typeProvider.findTypeByName(name) instanceof JvmType permittedType)) {
				return Optional.empty();
			}
			result.add(type.getOwner().toLightweightTypeReference(permittedType));
		}
		return Optional.of(List.copyOf(result));
	}

	protected Optional<List<String>> permittedTypeNames(final EObject context,
			final LightweightTypeReference type) {
		final Class<?> javaType;
		try {
			javaType = classLoader.loadClass(type.getType().getIdentifier());
		} catch (final ClassNotFoundException exception) {
			return Optional.empty();
		}
		if (!javaType.isSealed()) {
			return Optional.empty();
		}
		return Optional.of(java.util.Arrays.stream(javaType.getPermittedSubclasses())
			.map(Class::getName).toList());
	}
}
