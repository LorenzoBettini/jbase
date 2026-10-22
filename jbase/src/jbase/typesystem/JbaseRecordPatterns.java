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

	public static List<XJComponentPattern> bindings(final XJRecordPattern pattern) {
		return EcoreUtil2.getAllContentsOfType(pattern, XJComponentPattern.class).stream()
			.filter(component -> component.getRecordPattern() == null).toList();
	}

	public LightweightTypeReference recordType(final LightweightTypeReference declared,
			final LightweightTypeReference source) {
		if (!declared.isRawType()) {
			return declared;
		}
		final var inferred = new JbaseInstanceOfTypes().inferType(source, declared);
		return inferred == null ? declared : inferred;
	}

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

	public LightweightTypeReference componentType(final LightweightTypeReference record,
			final JvmField field) {
		final var mapping = new DeclaratorTypeArgumentCollector().getTypeParameterMapping(record);
		return new StandardTypeParameterSubstitutor(mapping, record.getOwner())
			.substitute(field.getType()).getUpperBoundSubstitute();
	}

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

	public JvmTypeReference typeReference(final XJRecordPattern pattern) {
		return switch (pattern.eContainer()) {
			case XJInstanceOfExpression expression -> expression.getType();
			case XJCasePart part -> part.getTypeGuard();
			default -> ((XJComponentPattern) pattern.eContainer()).getParameterType();
		};
	}

	public LightweightTypeReference declaredType(final XJRecordPattern pattern,
			final IResolvedTypes types) {
		return sourceType(pattern, types).getOwner()
			.toLightweightTypeReference(typeReference(pattern));
	}

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
