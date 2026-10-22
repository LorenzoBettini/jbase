package jbase.typesystem;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.eclipse.xtext.common.types.JvmTypeParameter;
import org.eclipse.xtext.common.types.JvmTypeParameterDeclarator;
import org.eclipse.xtext.xbase.typesystem.references.LightweightMergedBoundTypeArgument;
import org.eclipse.xtext.xbase.typesystem.references.LightweightBoundTypeArgument;
import org.eclipse.xtext.xbase.typesystem.references.LightweightTypeReference;
import org.eclipse.xtext.xbase.typesystem.references.ParameterizedTypeReference;
import org.eclipse.xtext.xbase.typesystem.util.BoundTypeArgumentSource;
import org.eclipse.xtext.xbase.typesystem.util.StandardTypeParameterSubstitutor;
import org.eclipse.xtext.xbase.typesystem.util.TypeArgumentFromComputedTypeCollector;
import org.eclipse.xtext.xbase.typesystem.util.VarianceInfo;

/** Checked parameterized narrowing conversions for Java instanceof (JLS 5.1.6.2). */
public class JbaseInstanceOfTypes {

	/**
	 * Checks whether generic type information permits a test without an unchecked conversion.
	 * Arrays are checked recursively, and the null type is accepted. For example, a
	 * {@code List<String>} can be tested against {@code ArrayList<String>}, whereas an
	 * {@code Object} cannot. This complements ordinary cast-compatibility validation;
	 * it does not replace it.
	 *
	 * @param source the operand's resolved static type
	 * @param target the non-reifiable class, interface, or array type being tested
	 * @return whether the source's constraints make the parameterized test checkable
	 */
	public boolean isChecked(final LightweightTypeReference source,
			final LightweightTypeReference target) {
		if (source.isAny()) {
			return true;
		}
		if (target.isArray()) {
			return source.isArray()
				&& isChecked(source.getComponentType(), target.getComponentType());
		}
		if (!source.isRawType() && target.isAssignableFrom(source)) {
			return true;
		}
		final var inferred = inferType(source, target);
		return inferred != null && target.isAssignableFrom(inferred);
	}

	/**
	 * Infers the widest target instantiation compatible with the source's static type.
	 * Projects the target declaration onto the source's constraint type, then substitutes
	 * inferred arguments into the target, including its enclosing types. Unconstrained
	 * parameters remain wildcards within their declared bounds. No diagnostics are emitted.
	 *
	 * @param source the resolved static type supplying generic constraints
	 * @param target a class or interface type whose declaration supplies the type parameters
	 * @return an inferred reference owned by the target's reference owner, or {@code null}
	 *         if the source constraint is raw or cannot be projected from the target
	 */
	public LightweightTypeReference inferType(final LightweightTypeReference source,
			final LightweightTypeReference target) {
		final var parameters = new ArrayList<JvmTypeParameter>();
		final var declaration = declaration(target, parameters);
		final var constraint = source.getConstraintSubstitute();
		final var projection = declaration.getSuperType(constraint.getType());
		if (projection == null || constraint.isRawType()) {
			return null;
		}
		final var collector = new TypeArgumentFromComputedTypeCollector(parameters,
			BoundTypeArgumentSource.INFERRED, target.getOwner());
		collector.populateTypeParameterMapping(projection, constraint);
		final var inferred = collector.rawGetTypeParameterMapping();
		final var mapping = new HashMap<JvmTypeParameter, LightweightMergedBoundTypeArgument>();
		for (final var parameter : parameters) {
			final var bounds = inferred.get(parameter);
			final LightweightTypeReference argument;
			if (bounds == null) {
				final var wildcard = target.getOwner().newWildcardTypeReference();
				wildcard.addUpperBound(target.getOwner().newParameterizedTypeReference(parameter)
					.getConstraintSubstitute());
				argument = wildcard;
			} else {
				argument = inferredArgument(bounds.getFirst());
			}
			mapping.put(parameter, new LightweightMergedBoundTypeArgument(
				argument, VarianceInfo.INVARIANT));
		}
		// Unconstrained parameters stay wildcards within their declared bounds.
		// The caller checks containment or projects the resulting component types.
		return new StandardTypeParameterSubstitutor(mapping, target.getOwner())
			.substitute(declaration);
	}

	private LightweightTypeReference inferredArgument(final LightweightBoundTypeArgument bound) {
		final var type = bound.getTypeReference();
		if (bound.getActualVariance() == VarianceInfo.INVARIANT) {
			return type;
		}
		final var wildcard = type.getOwner().newWildcardTypeReference();
		if (bound.getActualVariance() == VarianceInfo.IN) {
			wildcard.setLowerBound(type);
		} else {
			wildcard.addUpperBound(type);
		}
		return wildcard;
	}

	private ParameterizedTypeReference declaration(final LightweightTypeReference target,
			final List<JvmTypeParameter> parameters) {
		final var owner = target.getOwner();
		final var result = target.getOuter() == null
			? owner.newParameterizedTypeReference(target.getType())
			: owner.newParameterizedTypeReference(declaration(target.getOuter(), parameters),
				target.getType());
		final var generic = (JvmTypeParameterDeclarator) target.getType();
		for (final var parameter : generic.getTypeParameters()) {
			parameters.add(parameter);
			result.addTypeArgument(owner.newParameterizedTypeReference(parameter));
		}
		return result;
	}
}
