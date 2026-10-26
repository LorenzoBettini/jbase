package jbase.compiler;

import com.google.inject.Inject;
import com.google.inject.Provider;
import jbase.jbase.XJGenericArrayTypeReference;
import jbase.jbase.XJInnerTypeReference;
import jbase.jbase.XJParameterizedTypeReference;
import jbase.jbase.XJWildcardTypeReference;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.common.types.JvmMultiTypeReference;
import org.eclipse.xtext.common.types.JvmParameterizedTypeReference;
import org.eclipse.xtext.common.types.JvmTypeReference;
import org.eclipse.xtext.xbase.compiler.IAppendable;
import org.eclipse.xtext.xbase.compiler.TypeReferenceSerializer;
import org.eclipse.xtext.xbase.compiler.output.ITreeAppendable;
import org.eclipse.xtext.xbase.annotations.xAnnotations.XAnnotation;

/**
 * Serializes JVM type references while preserving Jbase's source type-use annotations.
 *
 * <p>Annotations have different positions for array dimensions and nested types, so each
 * annotated reference kind is emitted at its Java source position. Nested type arguments
 * are serialized recursively through this serializer to preserve their annotations too.
 */
public class JbaseTypeReferenceSerializer extends TypeReferenceSerializer {

	@Inject
	private Provider<JbaseXbaseCompiler> compilerProvider;

	/**
	 * Emits source annotations before delegating standard type serialization, except where
	 * Java syntax places them on an array dimension or after a nested-type separator.
	 *
	 * @param reference the type reference to serialize
	 * @param context the model element owning the reference
	 * @param appendable the output receiving the Java source
	 * @param typeReference whether the reference is serialized as a type reference
	 * @param keepLocalTypeParameters whether local type parameters are retained
	 * @param keepWildcards whether wildcard types are retained
	 * @param useTypeParameters whether type parameters are used
	 */
	@Override
	public void serialize(final JvmTypeReference reference, final EObject context,
			final IAppendable appendable, final boolean typeReference,
			final boolean keepLocalTypeParameters, final boolean keepWildcards,
			final boolean useTypeParameters) {
		switch (reference) {
			case XJParameterizedTypeReference annotated
					when !annotated.getSourceAnnotations().isEmpty() -> {
				appendAnnotations(annotated.getSourceAnnotations(), appendable);
				super.serialize(reference, context, appendable, typeReference,
					keepLocalTypeParameters, keepWildcards, useTypeParameters);
			}
			case XJGenericArrayTypeReference annotated
					when !annotated.getSourceAnnotations().isEmpty() -> {
				serialize(annotated.getComponentType(), context, appendable, typeReference,
					keepLocalTypeParameters, keepWildcards, true);
				appendable.append(" ");
				appendAnnotations(annotated.getSourceAnnotations(), appendable);
				appendable.append("[]");
			}
			case XJInnerTypeReference annotated
					when !annotated.getSourceAnnotations().isEmpty() -> {
				serialize(annotated.getOuter(), context, appendable, typeReference,
					keepLocalTypeParameters, keepWildcards, true);
				appendable.append(".");
				appendAnnotations(annotated.getSourceAnnotations(), appendable);
				final var simpleName = annotated.getType().getSimpleName();
				// Remove the package or enclosing type prefix before emitting a nested type.
				final var separator = Math.max(simpleName.lastIndexOf('.'),
					simpleName.lastIndexOf('$'));
				appendable.append(simpleName.substring(separator + 1));
				serializeAnnotatedTypeArguments(annotated, context, appendable, typeReference,
					keepLocalTypeParameters, keepWildcards);
			}
			case XJWildcardTypeReference annotated
					when !annotated.getSourceAnnotations().isEmpty() -> {
				appendAnnotations(annotated.getSourceAnnotations(), appendable);
				super.serialize(reference, context, appendable, typeReference,
					keepLocalTypeParameters, keepWildcards, useTypeParameters);
			}
			case JvmMultiTypeReference intersection -> {
				for (var index = 0; index < intersection.getReferences().size(); index++) {
					if (index > 0) {
						appendable.append(" & ");
					}
					serialize(intersection.getReferences().get(index), context, appendable,
						typeReference, keepLocalTypeParameters, keepWildcards, useTypeParameters);
				}
			}
			case null, default -> super.serialize(reference, context, appendable, typeReference,
				keepLocalTypeParameters, keepWildcards, useTypeParameters);
		}
	}

	/**
	 * Serializes generic arguments recursively so annotations on nested arguments survive.
	 *
	 * @param reference the parameterized type whose arguments are emitted
	 * @param context the model element owning the reference
	 * @param appendable the output receiving the Java source
	 * @param typeReference whether the reference is serialized as a type reference
	 * @param keepLocalTypeParameters whether local type parameters are retained
	 * @param keepWildcards whether wildcard types are retained
	 */
	private void serializeAnnotatedTypeArguments(final JvmParameterizedTypeReference reference,
			final EObject context, final IAppendable appendable, final boolean typeReference,
			final boolean keepLocalTypeParameters, final boolean keepWildcards) {
		if (!reference.getArguments().isEmpty()) {
			appendable.append("<");
			for (var index = 0; index < reference.getArguments().size(); index++) {
				if (index > 0) {
					appendable.append(", ");
				}
				serialize(reference.getArguments().get(index), context, appendable,
					typeReference, keepLocalTypeParameters, keepWildcards, false);
			}
			appendable.append(">");
		}
	}

	/**
	 * Emits annotations through the normal compiler path so annotation expressions and imports
	 * are handled consistently with other generated Java.
	 *
	 * @param annotations source annotations to emit
	 * @param appendable the output receiving the annotations
	 * @throws IllegalStateException if the output cannot manage generated imports
	 */
	private void appendAnnotations(final Iterable<XAnnotation> annotations,
			final IAppendable appendable) {
		if (!(appendable instanceof ITreeAppendable treeAppendable)) {
			throw new IllegalStateException("Source annotations require a tree appendable");
		}
		compilerProvider.get().appendSourceAnnotations(annotations, treeAppendable);
	}
}
