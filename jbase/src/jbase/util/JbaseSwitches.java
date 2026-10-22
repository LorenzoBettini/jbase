package jbase.util;

import java.util.ArrayList;
import java.util.HashSet;

import org.eclipse.xtext.common.types.JvmEnumerationType;
import org.eclipse.xtext.nodemodel.util.NodeModelUtils;
import org.eclipse.xtext.xbase.XAbstractFeatureCall;
import org.eclipse.xtext.xbase.XBlockExpression;
import org.eclipse.xtext.xbase.XBooleanLiteral;
import org.eclipse.xtext.xbase.XIfExpression;
import org.eclipse.xtext.xbase.XNullLiteral;
import org.eclipse.xtext.xbase.XSwitchExpression;
import org.eclipse.xtext.xbase.XbasePackage;
import org.eclipse.xtext.xbase.typesystem.IResolvedTypes;
import org.eclipse.xtext.xbase.typesystem.references.LightweightTypeReference;

import jbase.jbase.XJCasePart;
import jbase.jbase.XJConditionalExpression;
import jbase.jbase.XJSwitchExpression;
import jbase.typesystem.JbaseRecordPatterns;
import jbase.typesystem.JbaseSealedTypeResolver;
import jbase.typesystem.JbaseInstanceOfTypes;

/** Shared distinctions between Java switch statements, rules and expressions. */
public final class JbaseSwitches {
	private JbaseSwitches() {
	}

	/**
	 * Classifies a switch by its containment as a value expression or a statement.
	 * This uses its syntactic position rather than its inferred result type.
	 *
	 * @param expression the attached switch to classify
	 * @return {@code true} for a switch used as a value; {@code false} for a statement
	 *         position or a detached switch
	 */
	public static boolean isExpression(final XSwitchExpression expression) {
		final var parent = expression.eContainer();
		final var feature = expression.eContainingFeature();
		return parent != null && !(parent instanceof XBlockExpression
			|| parent instanceof XIfExpression && !(parent instanceof XJConditionalExpression)
				&& feature != XbasePackage.Literals.XIF_EXPRESSION__IF
			|| feature == XbasePackage.Literals.XABSTRACT_WHILE_EXPRESSION__BODY
			|| feature == XbasePackage.Literals.XFOR_LOOP_EXPRESSION__EACH_EXPRESSION
			|| feature == XbasePackage.Literals.XBASIC_FOR_LOOP_EXPRESSION__EACH_EXPRESSION);
	}

	/**
	 * Determines whether a switch needs generation that preserves Java 21 case syntax.
	 * This includes expressions, arrow rules, patterns, grouped labels, null labels,
	 * and cases that appear after the default.
	 *
	 * @param expression a Jbase switch represented by {@link XJSwitchExpression}
	 * @return whether the switch requires enhanced handling rather than the classic path
	 */
	public static boolean isEnhanced(final XSwitchExpression expression) {
		return isExpression(expression) || hasCasesAfterDefault(expression)
			|| ((XJSwitchExpression) expression).isDefaultArrow()
			|| ((XJSwitchExpression) expression).isNullDefault()
			|| expression.getCases().stream().anyMatch(part -> {
				final var rule = (XJCasePart) part;
				return rule.isArrow() || rule.getTypeGuard() != null
					|| !rule.getAdditionalLabels().isEmpty()
					|| rule.getCase() instanceof XNullLiteral;
			});
	}

	private static boolean hasCasesAfterDefault(final XSwitchExpression expression) {
		final var defaultNode = NodeModelUtils.getNode(expression.getDefault());
		if (defaultNode == null || expression.getCases().isEmpty()) {
			return false;
		}
		final var last = NodeModelUtils.getNode(expression.getCases().getLast());
		return last != null && last.getOffset() > defaultNode.getOffset();
	}

	/**
	 * Checks the supported forms of switch exhaustiveness.
	 * Recognizes a default, a covering type or record pattern without a guard or with a
	 * literal {@code true} guard, complete enum-constant coverage, and coverage of all
	 * permitted subclasses of sealed types.
	 *
	 * @param expression a Jbase switch with resolved selector and pattern types
	 * @param types resolved types for the selector and component bindings
	 * @param sealedTypeResolver resolves permitted subclasses in the active type system
	 * @return whether a supported form proves the switch exhaustive
	 */
	public static boolean isExhaustive(final XSwitchExpression expression,
			final IResolvedTypes types, final JbaseSealedTypeResolver sealedTypeResolver) {
		if (expression.getDefault() != null) {
			return true;
		}
		final var selector = types.getActualType(expression.getSwitch());
		return hasTotalPattern(expression, selector, types) || coversAllLiterals(expression, selector)
			|| coversPermittedTypes(expression, selector, types, sealedTypeResolver);
	}

	private static boolean hasTotalPattern(final XSwitchExpression expression,
			final LightweightTypeReference selector, final IResolvedTypes types) {
		return expression.getCases().stream().map(XJCasePart.class::cast)
			.anyMatch(part -> isUnconditional(part) && part.getTypeGuard() != null
				&& coversPattern(part, selector, selector, types));
	}

	private static boolean isUnconditional(final XJCasePart part) {
		return part.getCase() == null || part.getCase() instanceof XBooleanLiteral literal
			&& literal.isIsTrue();
	}

	private static boolean coversPattern(final XJCasePart part,
			final LightweightTypeReference patternSource, final LightweightTypeReference branchType,
			final IResolvedTypes types) {
		final var patternType = patternSource.getOwner()
			.toLightweightTypeReference(part.getTypeGuard());
		if (!patternType.isAssignableFrom(branchType)) {
			return false;
		}
		final var records = new JbaseRecordPatterns();
		return part.getRecordPattern() == null
			|| records.covers(part.getRecordPattern(),
				records.recordType(patternType, branchType), types);
	}

	private static boolean coversPermittedTypes(final XSwitchExpression expression,
			final LightweightTypeReference selector, final IResolvedTypes types,
			final JbaseSealedTypeResolver sealedTypeResolver) {
		final var permittedTypes = sealedTypeResolver.permittedTypes(expression, selector);
		if (permittedTypes.isEmpty()) {
			return false;
		}
		for (final var permittedType : permittedTypes.get()) {
			if (!coversPermittedType(expression, selector, permittedType, types,
					sealedTypeResolver)) {
				return false;
			}
		}
		return true;
	}

	private static boolean coversPermittedType(final XSwitchExpression expression,
			final LightweightTypeReference selector, final LightweightTypeReference permittedType,
			final IResolvedTypes types, final JbaseSealedTypeResolver sealedTypeResolver) {
		final var inferredType = new JbaseInstanceOfTypes().inferType(selector, permittedType);
		final var branchType = inferredType == null ? permittedType : inferredType;
		if (inferredType != null && !selector.isAssignableFrom(inferredType)) {
			return true;
		}
		for (final var original : expression.getCases()) {
			final var part = (XJCasePart) original;
			if (isUnconditional(part) && part.getTypeGuard() != null
					&& coversPattern(part, selector, branchType, types)) {
				return true;
			}
		}
		if (coversAllLiterals(expression, branchType)) {
			return true;
		}
		final var nested = sealedTypeResolver.permittedTypes(expression, branchType);
		if (nested.isEmpty()) {
			return false;
		}
		for (final var nestedType : nested.get()) {
			if (!coversPermittedType(expression, branchType, nestedType, types,
					sealedTypeResolver)) {
				return false;
			}
		}
		return true;
	}

	private static boolean coversAllLiterals(final XSwitchExpression expression,
			final LightweightTypeReference selector) {
		if (!(selector.getType() instanceof JvmEnumerationType enumeration)) {
			return false;
		}
		final var remaining = new HashSet<>(enumeration.getLiterals());
		for (final var original : expression.getCases()) {
			final var part = (XJCasePart) original;
			final var labels = new ArrayList<>(part.getAdditionalLabels());
			labels.add(part.getCase());
			for (final var label : labels) {
				if (label instanceof XAbstractFeatureCall call) {
					remaining.remove(call.getFeature());
				}
			}
		}
		return remaining.isEmpty();
	}
}
