package jbase.typesystem;

import java.util.ArrayList;
import java.util.List;

import com.google.inject.Inject;
import org.eclipse.xtext.common.types.JvmFormalParameter;
import org.eclipse.xtext.xbase.XAbstractWhileExpression;
import org.eclipse.xtext.xbase.XBasicForLoopExpression;
import org.eclipse.xtext.xbase.XBinaryOperation;
import org.eclipse.xtext.xbase.XBlockExpression;
import org.eclipse.xtext.xbase.XBooleanLiteral;
import org.eclipse.xtext.xbase.XExpression;
import org.eclipse.xtext.xbase.XIfExpression;
import org.eclipse.xtext.xbase.XReturnExpression;
import org.eclipse.xtext.xbase.XSynchronizedExpression;
import org.eclipse.xtext.xbase.XThrowExpression;
import org.eclipse.xtext.xbase.XTryCatchFinallyExpression;
import org.eclipse.xtext.xbase.XUnaryOperation;
import org.eclipse.xtext.xbase.XWhileExpression;
import org.eclipse.xtext.xbase.typesystem.computation.ITypeComputationState;
import org.eclipse.xtext.xbase.typesystem.internal.AbstractTypeComputationState;

import jbase.controlflow.JbaseBreakStatementDetector;
import jbase.jbase.XJBranchingStatement;
import jbase.jbase.XJConditionalExpression;
import jbase.jbase.XJInstanceOfExpression;
import jbase.jbase.XJSemicolonStatement;

/**
 * Java's definitely-matched scopes (JLS 6.3.1 and 6.3.2). Parentheses do not have
 * their own model node. A binding belongs to a successful match, never to the
 * checked operand or to a saved boolean result.
 */
public class JbasePatternVariables {

	@Inject
	private JbaseBreakStatementDetector breakStatementDetector;

	/**
	 * Assigns types to the bindings of an {@code instanceof} pattern and checks their names.
	 * The operand must already have been typed. A checkpoint validates declarations
	 * without exposing them in the caller's current scope.
	 *
	 * @param expression the computed {@code instanceof} expression
	 * @param state the active Xbase computation state receiving the binding types
	 */
	public void assignType(final XJInstanceOfExpression expression,
			final ITypeComputationState state) {
		final var variable = expression.getPatternVariable();
		if (variable != null) {
			state.assignType(variable, state.getReferenceOwner()
				.toLightweightTypeReference(expression.getType()), false);
			// Validate the declaration even when no guarded expression uses it.
			state.withTypeCheckpoint(expression).addLocalToCurrentScope(variable);
		}
		if (expression.getRecordPattern() != null) {
			new JbaseRecordPatterns().assignTypes(expression.getRecordPattern(), state
				.getReferenceOwner().toLightweightTypeReference(expression.getType()),
				((AbstractTypeComputationState) state).getResolvedTypes()
					.getActualType(expression.getExpression()), state);
			final var declarations = state.withTypeCheckpoint(expression);
			JbaseRecordPatterns.bindings(expression.getRecordPattern())
				.forEach(declarations::addLocalToCurrentScope);
		}
	}

	/**
	 * Adds bindings that are definitely matched on entry to an expression.
	 * Handles branches, short-circuit right operands, while bodies, and basic-for bodies
	 * and updates. The original scope is left unchanged.
	 *
	 * @param expression the child expression about to be computed
	 * @param state the computation state before entering the child's flow scope
	 * @return a checkpoint containing the visible bindings, or the original state when
	 *         no additional bindings are available
	 */
	public ITypeComputationState enterScope(final XExpression expression,
			final ITypeComputationState state) {
		final var parent = expression.eContainer();
		List<JvmFormalParameter> variables = List.of();
		if (parent instanceof XIfExpression conditional) {
			if (expression == conditional.getThen()) {
				variables = matched(conditional.getIf(), true);
			} else if (expression == conditional.getElse()) {
				variables = matched(conditional.getIf(), false);
			}
		} else if (parent instanceof XBinaryOperation binary
				&& expression == binary.getRightOperand()) {
			final var operator = binary.getConcreteSyntaxFeatureName();
			if ("&&".equals(operator) || "||".equals(operator)) {
				variables = matched(binary.getLeftOperand(), "&&".equals(operator));
			}
		} else if (parent instanceof XWhileExpression loop && expression == loop.getBody()) {
			variables = matched(loop.getPredicate(), true);
		} else if (parent instanceof XBasicForLoopExpression loop
				&& (expression == loop.getEachExpression()
					|| loop.getUpdateExpressions().contains(expression))) {
			variables = matched(loop.getExpression(), true);
		}
		if (variables.isEmpty()) {
			return state;
		}
		final var scoped = state.withTypeCheckpoint(expression);
		variables.forEach(scoped::addLocalToCurrentScope);
		return scoped;
	}

	/**
	 * Collects bindings definitely matched when a boolean expression has the given result.
	 * Negation reverses the result; applicable short-circuit operands combine their
	 * bindings. Saved boolean values and other expressions introduce no bindings.
	 *
	 * @param expression the condition to inspect; may be {@code null}
	 * @param result the boolean outcome for which bindings are required
	 * @return bindings in traversal order, possibly empty; duplicate declarations are
	 *         retained so validation can report them
	 */
	public List<JvmFormalParameter> matched(final XExpression expression, final boolean result) {
		if (expression instanceof XJInstanceOfExpression instanceOf
				&& result) {
			if (instanceOf.getRecordPattern() != null) {
				return new ArrayList<>(JbaseRecordPatterns.bindings(instanceOf.getRecordPattern()));
			}
			if (instanceOf.getPatternVariable() != null) {
				return List.of(instanceOf.getPatternVariable());
			}
		}
		if (expression instanceof XUnaryOperation unary
				&& "!".equals(unary.getConcreteSyntaxFeatureName())) {
			return matched(unary.getOperand(), !result);
		}
		if (expression instanceof XBinaryOperation binary
				&& (result && "&&".equals(binary.getConcreteSyntaxFeatureName())
					|| !result && "||".equals(binary.getConcreteSyntaxFeatureName()))) {
			final var variables = new ArrayList<>(matched(binary.getLeftOperand(), result));
			variables.addAll(matched(binary.getRightOperand(), result));
			return variables;
		}
		return List.of();
	}

	/**
	 * Exposes bindings established by a completed statement to following statements.
	 * This handles guard clauses and the false conditions of loops without an exiting
	 * break. When bindings are introduced, the computation state's scope is rewritten
	 * for the completed statement.
	 *
	 * @param statement the statement whose types and control flow have been computed
	 * @param state the enclosing block state to update
	 */
	public void addIntroducedByStatement(final XExpression statement,
			final ITypeComputationState state) {
		List<JvmFormalParameter> introduced = List.of();
		if (statement instanceof XIfExpression conditional
				&& !(statement instanceof XJConditionalExpression)) {
			final var thenExits = cannotCompleteNormally(conditional.getThen());
			final var elseExits = cannotCompleteNormally(conditional.getElse());
			if (thenExits != elseExits) {
				introduced = matched(conditional.getIf(), elseExits);
			}
		} else if (statement instanceof XAbstractWhileExpression loop && !hasBreak(loop)) {
			introduced = matched(loop.getPredicate(), false);
		} else if (statement instanceof XBasicForLoopExpression loop && !hasBreak(loop)) {
			introduced = matched(loop.getExpression(), false);
		}
		if (!introduced.isEmpty()) {
			introduced.forEach(state::addLocalToCurrentScope);
			state.rewriteScope(statement);
		}
	}

	/** Resolves break targets structurally without forcing reentrant feature linking. */
	private boolean hasBreak(final XExpression target) {
		return breakStatementDetector.containsPossibleBreakStatement(target);
	}

	private boolean isTrue(final XExpression expression) {
		return expression instanceof XBooleanLiteral literal && literal.isIsTrue();
	}

	private boolean cannotCompleteNormally(final XExpression expression) {
		return switch (expression) {
			case null -> false;
			case XJBranchingStatement branch -> true;
			case XJSemicolonStatement wrapper -> cannotCompleteNormally(wrapper.getExpression());
			case XBlockExpression block ->
				block.getExpressions().stream().anyMatch(this::cannotCompleteNormally);
			case XIfExpression conditional -> cannotCompleteNormally(conditional.getThen())
				&& cannotCompleteNormally(conditional.getElse());
			case XReturnExpression returned -> true;
			case XThrowExpression thrown -> true;
			case XSynchronizedExpression synchronizedBlock ->
				cannotCompleteNormally(synchronizedBlock.getExpression());
			case XTryCatchFinallyExpression tried -> cannotCompleteNormally(tried.getFinallyExpression())
				|| cannotCompleteNormally(tried.getExpression()) && tried.getCatchClauses().stream()
					.allMatch(caught -> cannotCompleteNormally(caught.getExpression()));
			case XWhileExpression loop -> isTrue(loop.getPredicate()) && !hasBreak(loop);
			case XBasicForLoopExpression loop ->
				(loop.getExpression() == null || isTrue(loop.getExpression())) && !hasBreak(loop);
			default -> false;
		};
	}
}
