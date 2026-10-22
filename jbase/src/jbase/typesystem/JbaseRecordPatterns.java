package jbase.typesystem;

import java.util.List;
import java.util.stream.StreamSupport;

import org.eclipse.xtext.EcoreUtil2;
import org.eclipse.xtext.common.types.JvmField;
import org.eclipse.xtext.common.types.JvmGenericType;
import org.eclipse.xtext.common.types.JvmTypeReference;
import org.eclipse.xtext.xbase.XSwitchExpression;
import org.eclipse.xtext.xbase.typesystem.IResolvedTypes;
import org.eclipse.xtext.xbase.typesystem.computation.ITypeComputationState;
import org.eclipse.xtext.xbase.typesystem.references.LightweightTypeReference;
import org.eclipse.xtext.xbase.typesystem.util.DeclaratorTypeArgumentCollector;
import org.eclipse.xtext.xbase.typesystem.util.StandardTypeParameterSubstitutor;

import jbase.jbase.XJCasePart;
import jbase.jbase.XJComponentPattern;
import jbase.jbase.XJInstanceOfExpression;
import jbase.jbase.XJRecordPattern;

/** Component types and bindings shared by instanceof and switch record patterns. */
public class JbaseRecordPatterns {

	/**
	 * Collects the leaf variable declarations in a record pattern, recursively.
	 * Nested record-pattern nodes do not themselves declare variables.
	 *
	 * @param pattern the record pattern to traverse
	 * @return an unmodifiable list of bindings in containment order, possibly empty
	 */
	public static List<XJComponentPattern> bindings(final XJRecordPattern pattern) {
		return EcoreUtil2.getAllContentsOfType(pattern, XJComponentPattern.class).stream()
			.filter(component -> component.getRecordPattern() == null).toList();
	}

	/**
	 * Resolves omitted type arguments from the matched value's static type.
	 * Explicitly parameterized and non-generic references are returned unchanged.
	 *
	 * @param declared the type written in the record pattern
	 * @param source the static type of the value matched by this pattern
	 * @return the inferred record type, or the declared reference if inference is unavailable
	 */
	public LightweightTypeReference recordType(final LightweightTypeReference declared,
			final LightweightTypeReference source) {
		if (!declared.isRawType()) {
			return declared;
		}
		final var inferred = new JbaseInstanceOfTypes().inferType(source, declared);
		return inferred == null ? declared : inferred;
	}

	/**
	 * Returns the component fields used to type a record's deconstruction patterns.
	 * Records cannot declare additional instance fields, so static fields are excluded.
	 *
	 * @param type the resolved type to inspect
	 * @return an unmodifiable list of component fields in declaration order; empty for
	 *         non-record types and records without components
	 */
	public List<JvmField> fields(final LightweightTypeReference type) {
		if (!type.isSubtypeOf(Record.class)) {
			return List.of();
		}
		// Records cannot declare additional instance fields. The JVM model retains the
		// component fields in declaration order, including their generic signatures.
		return StreamSupport.stream(((JvmGenericType) type.getType())
			.getDeclaredFields().spliterator(), false)
			.filter(field -> !field.isStatic()).toList();
	}

	/**
	 * Substitutes a record's type arguments into one of its component field types.
	 * An outer wildcard is projected to its upper bound for an inferred component binding.
	 *
	 * @param record the record type, with any inferred arguments already applied
	 * @param field a component field declared by that record
	 * @return the substituted component type in the record's reference owner
	 */
	public LightweightTypeReference componentType(final LightweightTypeReference record,
			final JvmField field) {
		final var mapping = new DeclaratorTypeArgumentCollector().getTypeParameterMapping(record);
		return new StandardTypeParameterSubstitutor(mapping, record.getOwner())
			.substitute(field.getType()).getUpperBoundSubstitute();
	}

	/**
	 * Assigns types to leaf bindings throughout a record pattern without adding them to scope.
	 * Explicit component types are retained; {@code var} uses the corresponding component
	 * field type. Missing fields supply an unknown expected type during error recovery.
	 * Arity and pattern applicability are validated separately.
	 *
	 * @param pattern the record pattern whose bindings are to be typed
	 * @param declared the pattern's written record type
	 * @param source the matched value's static type, used for generic inference
	 * @param state the computation state receiving binding types; callers add bindings to
	 *              the appropriate flow scope after this method returns
	 */
	public void assignTypes(final XJRecordPattern pattern, final LightweightTypeReference declared,
			final LightweightTypeReference source, final ITypeComputationState state) {
		final var record = recordType(declared, source);
		final var components = fields(record);
		for (var i = 0; i < pattern.getComponents().size(); i++) {
			final var component = pattern.getComponents().get(i);
			final var expected = i < components.size()
				? componentType(record, components.get(i))
				: state.getReferenceOwner().newUnknownTypeReference();
			final var type = component.isInferred() ? expected
				: state.getReferenceOwner().toLightweightTypeReference(component.getParameterType());
			if (component.getRecordPattern() == null) {
				state.assignType(component, type, false);
			} else {
				assignTypes(component.getRecordPattern(), type, expected, state);
			}
		}
	}

	/**
	 * Finds the written type reference belonging to a record pattern.
	 *
	 * @param pattern a pattern attached to an {@code instanceof} expression, switch case,
	 *                or enclosing component pattern
	 * @return the containing node's type reference, before generic inference
	 */
	public JvmTypeReference typeReference(final XJRecordPattern pattern) {
		return switch (pattern.eContainer()) {
			case XJInstanceOfExpression expression -> expression.getType();
			case XJCasePart part -> part.getTypeGuard();
			default -> ((XJComponentPattern) pattern.eContainer()).getParameterType();
		};
	}

	/**
	 * Converts a pattern's written type using the matched value's reference owner.
	 * This does not infer omitted record type arguments.
	 *
	 * @param pattern an attached top-level or nested record pattern
	 * @param types resolved types for the containing expression and its bindings
	 * @return the lightweight form of {@link #typeReference(XJRecordPattern)}
	 */
	public LightweightTypeReference declaredType(final XJRecordPattern pattern,
			final IResolvedTypes types) {
		return sourceType(pattern, types).getOwner()
			.toLightweightTypeReference(typeReference(pattern));
	}

	/**
	 * Finds the static type of the value matched by a record pattern.
	 * Top-level patterns use the operand or selector type. Nested patterns use the
	 * corresponding component field after substitution and enclosing record inference.
	 *
	 * @param pattern an attached top-level or nested record pattern
	 * @param types resolved types for the containing expression and its bindings
	 * @return the matched value's type, or an unknown reference for an excess nested
	 *         component in a malformed pattern
	 */
	public LightweightTypeReference sourceType(final XJRecordPattern pattern,
			final IResolvedTypes types) {
		return switch (pattern.eContainer()) {
			case XJInstanceOfExpression expression ->
				types.getActualType(expression.getExpression());
			case XJCasePart part -> types.getActualType(
				((XSwitchExpression) part.eContainer()).getSwitch());
			default -> {
				final var component = (XJComponentPattern) pattern.eContainer();
				final var parent = (XJRecordPattern) component.eContainer();
				final var record = recordType(declaredType(parent, types),
					sourceType(parent, types));
				final var components = fields(record);
				final var index = parent.getComponents().indexOf(component);
				yield index < components.size() ? componentType(record, components.get(index))
					: record.getOwner().newUnknownTypeReference();
			}
		};
	}

	/**
	 * Compares two record patterns recursively for switch-case dominance.
	 * The patterns must name the same record class and have matching arity. Each earlier
	 * component must dominate its counterpart; a nested record pattern cannot dominate
	 * an ordinary type binding. Case guards are checked by the caller.
	 *
	 * @param prior the earlier record pattern
	 * @param next the later record pattern
	 * @param types resolved types, including the assigned component binding types
	 * @return whether the earlier record pattern dominates the later pattern
	 */
	public boolean dominates(final XJRecordPattern prior, final XJRecordPattern next,
			final IResolvedTypes types) {
		if (declaredType(prior, types).getType() != declaredType(next, types).getType()
				|| prior.getComponents().size() != next.getComponents().size()) {
			return false;
		}
		for (var i = 0; i < prior.getComponents().size(); i++) {
			final var left = prior.getComponents().get(i);
			final var right = next.getComponents().get(i);
			if (left.getRecordPattern() != null) {
				if (right.getRecordPattern() == null
						|| !dominates(left.getRecordPattern(), right.getRecordPattern(), types)) {
					return false;
				}
			} else {
				final var rightType = right.getRecordPattern() == null ? types.getActualType(right)
					: recordType(declaredType(right.getRecordPattern(), types),
						sourceType(right.getRecordPattern(), types));
				if (!types.getActualType(left).isAssignableFrom(rightType)) {
					return false;
				}
			}
		}
		return true;
	}

	/**
	 * Checks a single record pattern's structural type coverage for switch exhaustiveness.
	 * Each component must cover its declared field type, recursively for nested records.
	 * Coverage does not imply that a null value matches a record pattern at runtime.
	 *
	 * @param pattern the record pattern to check
	 * @param record its record type with omitted arguments already inferred
	 * @param types resolved types, including the assigned component binding types
	 * @return whether the pattern has the correct arity and covers every component type
	 */
	public boolean covers(final XJRecordPattern pattern, final LightweightTypeReference record,
			final IResolvedTypes types) {
		final var components = fields(record);
		if (components.size() != pattern.getComponents().size()) {
			return false;
		}
		for (var i = 0; i < components.size(); i++) {
			final var component = pattern.getComponents().get(i);
			final var expected = componentType(record, components.get(i));
			if (component.getRecordPattern() != null) {
				final var nested = component.getRecordPattern();
				final var nestedType = recordType(declaredType(nested, types), expected);
				if (!nestedType.isAssignableFrom(expected) || !covers(nested, nestedType, types)) {
					return false;
				}
			} else if (!types.getActualType(component).isAssignableFrom(expected)) {
				return false;
			}
		}
		return true;
	}
}
