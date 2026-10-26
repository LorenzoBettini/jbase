package jbase.compiler;

import com.google.inject.Inject;
import com.google.inject.Provider;
import jbase.jbase.XJGenericArrayTypeReference;
import jbase.jbase.XJInnerTypeReference;
import jbase.jbase.XJParameterizedTypeReference;
import jbase.jbase.XJWildcardTypeReference;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.common.types.JvmParameterizedTypeReference;
import org.eclipse.xtext.common.types.JvmTypeReference;
import org.eclipse.xtext.xbase.compiler.IAppendable;
import org.eclipse.xtext.xbase.compiler.TypeReferenceSerializer;
import org.eclipse.xtext.xbase.compiler.output.ITreeAppendable;
import org.eclipse.xtext.xbase.annotations.xAnnotations.XAnnotation;

public class JbaseTypeReferenceSerializer extends TypeReferenceSerializer {

	@Inject
	private Provider<JbaseXbaseCompiler> compilerProvider;

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
				final var separator = Math.max(simpleName.lastIndexOf('.'),
					simpleName.lastIndexOf('$'));
				appendable.append(simpleName.substring(separator + 1));
				serializeTypeArguments(annotated, context, appendable, typeReference,
					keepLocalTypeParameters, keepWildcards);
			}
			case XJWildcardTypeReference annotated
					when !annotated.getSourceAnnotations().isEmpty() -> {
				appendAnnotations(annotated.getSourceAnnotations(), appendable);
				super.serialize(reference, context, appendable, typeReference,
					keepLocalTypeParameters, keepWildcards, useTypeParameters);
			}
			case null, default -> super.serialize(reference, context, appendable, typeReference,
				keepLocalTypeParameters, keepWildcards, useTypeParameters);
		}
	}

	private void serializeTypeArguments(final JvmParameterizedTypeReference reference,
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

	private void appendAnnotations(final Iterable<XAnnotation> annotations,
			final IAppendable appendable) {
		if (!(appendable instanceof ITreeAppendable treeAppendable)) {
			throw new IllegalStateException("Source annotations require a tree appendable");
		}
		compilerProvider.get().appendSourceAnnotations(annotations, treeAppendable);
	}
}
