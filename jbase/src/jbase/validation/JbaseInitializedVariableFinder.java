package jbase.validation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;

import org.eclipse.xtext.xbase.XAbstractFeatureCall;
import org.eclipse.xtext.xbase.XAssignment;
import org.eclipse.xtext.xbase.XBasicForLoopExpression;
import org.eclipse.xtext.xbase.XBlockExpression;
import org.eclipse.xtext.xbase.XClosure;
import org.eclipse.xtext.xbase.XDoWhileExpression;
import org.eclipse.xtext.xbase.XExpression;
import org.eclipse.xtext.xbase.XForLoopExpression;
import org.eclipse.xtext.xbase.XIfExpression;
import org.eclipse.xtext.xbase.XSwitchExpression;
import org.eclipse.xtext.xbase.XSynchronizedExpression;
import org.eclipse.xtext.xbase.XTryCatchFinallyExpression;
import org.eclipse.xtext.xbase.XVariableDeclaration;
import org.eclipse.xtext.xbase.XWhileExpression;
import org.eclipse.xtext.xbase.lib.Procedures.Procedure1;

import com.google.inject.Inject;

import jbase.controlflow.JbaseBranchingStatementDetector;
import jbase.jbase.XJAdditionalXVariableDeclaration;
import jbase.jbase.XJArrayAccess;
import jbase.jbase.XJCasePart;
import jbase.jbase.XJMethodReference;
import jbase.jbase.XJTryWithResourcesStatement;
import jbase.jbase.XJVariableDeclaration;

/**
 * Detects references to variables which might not be initialized, according to Java semantics.
 * Lambda bodies use a snapshot of the current initialized locals: their reads must be valid
 * when the lambda is created, and their deferred writes cannot initialize an enclosing local.
 * Method reference receivers instead execute immediately and use the current state.
 *
 * <p>This analysis complements the effectively-final capture check in JbaseValidator;
 * absence of a later reassignment does not prove that a local was initialized before capture.
 *
 * @author Lorenzo Bettini
 */
public class JbaseInitializedVariableFinder {

	@Inject
	private JbaseBranchingStatementDetector branchingStatementDetector;

	@FunctionalInterface
	public interface NotInitializedAcceptor {
		void accept(XAbstractFeatureCall call);
	}

	public static class InitializedVariables extends HashSet<XVariableDeclaration> {
		private static final long serialVersionUID = 1L;
	}

	/**
	 * Calls the acceptor's accept method with all the references to
	 * variables which are not considered initialized.
	 *
	 * Method to be called from clients.
	 */
	public void detectNotInitialized(final XBlockExpression e,
			final NotInitializedAcceptor acceptor) {
		detectNotInitializedDispatch(e, new InitializedVariables(), acceptor);
	}

	/**
	 * We handle null case gracefully
	 */
	public void detectNotInitializedDispatch(final XExpression e,
			final InitializedVariables initialized, final NotInitializedAcceptor acceptor) {
		if (e != null) {
			detectNotInitialized(e, initialized, acceptor);
		}
	}

	/**
	 * Dispatches reference receivers before the XClosure supertype case. For example,
	 * {@code (text = "hello")::trim} initializes text immediately, whereas an assignment
	 * in a lambda body must not update the caller's initialized-variable set.
	 */
	public void detectNotInitialized(final XExpression e, final InitializedVariables initialized,
			final NotInitializedAcceptor acceptor) {
		switch (e) {
			case null -> throw new IllegalArgumentException("Unhandled parameter types: "
				+ Arrays.asList(e, initialized, acceptor));
			case XAssignment assignment -> inspectNotInitialized(assignment, initialized, acceptor);
			case XJAdditionalXVariableDeclaration declaration ->
				inspectNotInitialized(declaration, initialized, acceptor);
			case XJVariableDeclaration declaration ->
				inspectNotInitialized(declaration, initialized, acceptor);
			case XBasicForLoopExpression loop -> inspectNotInitialized(loop, initialized, acceptor);
			case XForLoopExpression loop -> inspectNotInitialized(loop, initialized, acceptor);
			case XWhileExpression loop -> inspectNotInitialized(loop, initialized, acceptor);
			case XDoWhileExpression loop -> inspectNotInitialized(loop, initialized, acceptor);
			case XSwitchExpression statement ->
				inspectNotInitialized(statement, initialized, acceptor);
			case XBlockExpression block -> inspectNotInitialized(block, initialized, acceptor);
			case XJMethodReference reference -> detectNotInitializedDispatch(reference.getReceiver(),
				initialized, acceptor);
			case XClosure closure -> detectNotInitializedDispatch(closure.getExpression(),
				createCopy(initialized), acceptor);
			case XAbstractFeatureCall call -> inspectNotInitialized(call, initialized, acceptor);
			case XIfExpression conditional ->
				inspectNotInitialized(conditional, initialized, acceptor);
			case XJTryWithResourcesStatement statement ->
				inspectNotInitialized(statement, initialized, acceptor);
			case XTryCatchFinallyExpression statement ->
				inspectNotInitialized(statement, initialized, acceptor);
			case XSynchronizedExpression statement ->
				inspectNotInitialized(statement, initialized, acceptor);
			default -> inspectNotInitialized(e, initialized, acceptor);
		}
	}

	private void inspectNotInitialized(final XExpression e,
			final InitializedVariables initialized, final NotInitializedAcceptor acceptor) {
		inspectContents(e, initialized, acceptor);
	}

	private void inspectNotInitialized(final XAssignment e,
			final InitializedVariables initialized, final NotInitializedAcceptor acceptor) {
		checkArrayAccess(e, initialized, acceptor);
		if (e.getFeature() instanceof XVariableDeclaration variable) {
			detectNotInitializedDispatch(e.getValue(), initialized, acceptor);
			initialized.add(variable);
		} else {
			inspectContents(e, initialized, acceptor);
		}
	}

	private void inspectNotInitialized(final XJVariableDeclaration e,
			final InitializedVariables initialized, final NotInitializedAcceptor acceptor) {
		inspectVariableDeclaration(e, initialized, acceptor);
		loopOverExpressions(e.getAdditionalVariables(), initialized, acceptor);
	}

	private void inspectNotInitialized(final XJAdditionalXVariableDeclaration e,
			final InitializedVariables initialized, final NotInitializedAcceptor acceptor) {
		inspectVariableDeclaration(e, initialized, acceptor);
	}

	private void inspectVariableDeclaration(final XVariableDeclaration e,
			final InitializedVariables initialized, final NotInitializedAcceptor acceptor) {
		if (e.getRight() != null) {
			detectNotInitializedDispatch(e.getRight(), initialized, acceptor);
			initialized.add(e);
		}
	}

	private void inspectNotInitialized(final XBasicForLoopExpression e,
			final InitializedVariables initialized, final NotInitializedAcceptor acceptor) {
		loopOverExpressions(e.getInitExpressions(), initialized, acceptor);

		// discard information collected
		var expressions = new ArrayList<XExpression>();
		expressions.add(e.getExpression());
		expressions.addAll(e.getUpdateExpressions());
		expressions.add(e.getEachExpression());
		loopOverExpressions(expressions, createCopy(initialized), acceptor);
	}

	private void inspectNotInitialized(final XForLoopExpression e,
			final InitializedVariables initialized, final NotInitializedAcceptor acceptor) {
		detectNotInitializedDispatch(e.getForExpression(), initialized, acceptor);
		// discard information collected
		detectNotInitializedDispatch(e.getEachExpression(), createCopy(initialized), acceptor);
	}

	private void inspectNotInitialized(final XWhileExpression e,
			final InitializedVariables initialized, final NotInitializedAcceptor acceptor) {
		detectNotInitializedDispatch(e.getPredicate(), initialized, acceptor);

		// discard information collected
		detectNotInitializedDispatch(e.getBody(), createCopy(initialized), acceptor);
	}

	private void inspectNotInitialized(final XDoWhileExpression e,
			final InitializedVariables initialized, final NotInitializedAcceptor acceptor) {
		// use information collected, since the body is surely executed
		detectNotInitializedDispatch(e.getBody(), initialized, acceptor);
		detectNotInitializedDispatch(e.getPredicate(), initialized, acceptor);
	}

	private void inspectNotInitialized(final XSwitchExpression e,
			final InitializedVariables initialized, final NotInitializedAcceptor acceptor) {
		detectNotInitializedDispatch(e.getSwitch(), initialized, acceptor);

		// we consider effective branches the cases that end with a break;
		// cases without a break will not be considered as branches, as in Java
		var effectiveBranches = new ArrayList<XExpression>();
		var otherBranches = new ArrayList<XExpression>();
		for (var casePart : e.getCases()) {
			if (casePart.getTypeGuard() != null) {
				detectNotInitializedDispatch(casePart.getCase(), createCopy(initialized), acceptor);
			}
			var then = casePart.getThen();
			if (casePart instanceof XJCasePart rule && rule.isArrow()
					|| branchingStatementDetector.isSureBranchStatement(then)) {
				effectiveBranches.add(then);
			} else {
				otherBranches.add(then);
			}
		}

		// adding the default case even if not specified, i.e., null
		// ensures that the intersection will be empty in case there is
		// a single branch (as expected).
		effectiveBranches.add(e.getDefault());

		// the cases not considered as effective branches are simply inspected
		loopOverExpressions(otherBranches, createCopy(initialized), acceptor);
		inspectBranchesAndIntersect(effectiveBranches, initialized, acceptor);
	}

	private void inspectNotInitialized(final XBlockExpression b,
			final InitializedVariables initialized, final NotInitializedAcceptor acceptor) {
		loopOverExpressions(b.getExpressions(), initialized, acceptor);
	}

	private void inspectNotInitialized(final XAbstractFeatureCall o,
			final InitializedVariables initialized, final NotInitializedAcceptor acceptor) {
		var actualArguments = o.getActualArguments();
		if (actualArguments.isEmpty()) {
			if (o.getFeature() instanceof XVariableDeclaration variable
					&& !initialized.contains(variable)) {
				acceptor.accept(o);
			}
		} else {
			loopOverExpressions(actualArguments, initialized, acceptor);
		}
	}

	private void inspectNotInitialized(final XIfExpression e,
			final InitializedVariables initialized, final NotInitializedAcceptor acceptor) {
		detectNotInitializedDispatch(e.getIf(), initialized, acceptor);
		inspectBranchesAndIntersect(Arrays.asList(e.getThen(), e.getElse()), initialized, acceptor);
	}

	private void inspectNotInitialized(final XTryCatchFinallyExpression e,
			final InitializedVariables initialized, final NotInitializedAcceptor acceptor) {
		detectNotInitializedTryCatchCommon(e, initialized, acceptor,
			variables -> inspectTryCatchFinallyBranchesAndIntersect(e, variables, acceptor));
	}

	private void inspectNotInitialized(final XJTryWithResourcesStatement e,
			final InitializedVariables initialized, final NotInitializedAcceptor acceptor) {
		detectNotInitializedTryCatchCommon(e, initialized, acceptor, variables -> {
			loopOverExpressions(e.getResources(), variables, acceptor);
			inspectTryCatchFinallyBranchesAndIntersect(e, variables, acceptor);
		});
	}

	private void inspectTryCatchFinallyBranchesAndIntersect(final XTryCatchFinallyExpression e,
			final InitializedVariables initializedVariables, final NotInitializedAcceptor acceptor) {
		var branches = new ArrayList<XExpression>();
		branches.add(e.getExpression());
		for (var catchClause : e.getCatchClauses()) {
			branches.add(catchClause.getExpression());
		}
		inspectBranchesAndIntersect(branches, initializedVariables, acceptor);
	}

	private void detectNotInitializedTryCatchCommon(final XTryCatchFinallyExpression e,
			final InitializedVariables initialized, final NotInitializedAcceptor acceptor,
			final Procedure1<? super InitializedVariables> branchInspector) {
		var finallyExpression = e.getFinallyExpression();
		if (finallyExpression != null) {
			// when inspecting the finally block we can't assume anything about
			// what's initialized in try and catch blocks
			/*
			 * Example:
			 *
			 * int i;
			 * int j;
			 * try {
			 * 	j = 0;
			 * } catch (NullPointerException e) {
			 * 	j = 0;
			 * } finally {
			 * 	i = 0;
			 * 	i = j; // ERROR
			 * }
			 * System.out.println(i); // OK
			 * System.out.println(j); // OK
			 */
			var temporaryCopy = createCopy(initialized);
			branchInspector.apply(temporaryCopy);
			// if present, the final block is always executed so we treat it as a block expression
			// without any information about try and catch
			detectNotInitializedDispatch(finallyExpression, initialized, acceptor);
			// now we can update the information for the following expressions
			// with what we had collected in try and catch
			initialized.addAll(temporaryCopy);
		} else {
			branchInspector.apply(initialized);
		}
	}

	/**
	 * This is just the same as the generic case for XExpression, but we put it here
	 * all the same for documentation.
	 *
	 * A synchronized block is always considered executed from the variable initialization point
	 * of view.
	 */
	private void inspectNotInitialized(final XSynchronizedExpression e,
			final InitializedVariables initialized, final NotInitializedAcceptor acceptor) {
		inspectContents(e, initialized, acceptor);
	}

	private void inspectContents(final XExpression e, final InitializedVariables initialized,
			final NotInitializedAcceptor acceptor) {
		var contents = e.eContents().stream()
			.filter(XExpression.class::isInstance).map(XExpression.class::cast).toList();
		loopOverExpressions(contents, initialized, acceptor);
	}

	private void checkArrayAccess(final XExpression e, final InitializedVariables initialized,
			final NotInitializedAcceptor acceptor) {
		if (e instanceof XJArrayAccess arrayAccess) {
			loopOverExpressions(arrayAccess.getIndexes(), initialized, acceptor);
		}
	}

	/**
	 * Inspects all the branches and computes the initialized variables
	 * as the intersection of the results for all inspected branches.
	 *
	 * IMPORTANT: the intersection assumes that branches contains at least two branches;
	 * callers must ensure this.  If there's only one branch, then the intersection
	 * will simply be the result for the single branch.  This works when
	 * the single branch is the 'default' branch of a switch.
	 */
	private void inspectBranchesAndIntersect(final Iterable<? extends XExpression> branches,
			final InitializedVariables initialized, final NotInitializedAcceptor acceptor) {
		InitializedVariables intersection = null;
		for (var branch : branches) {
			var branchVariables = inspectBranch(branch, initialized, acceptor);
			if (intersection == null) {
				intersection = branchVariables;
			} else {
				intersection.retainAll(branchVariables);
			}
		}
		initialized.addAll(intersection);
	}

	private InitializedVariables inspectBranch(final XExpression b,
			final InitializedVariables initialized, final NotInitializedAcceptor acceptor) {
		var copy = createCopy(initialized);
		detectNotInitializedDispatch(b, copy, acceptor);
		return copy;
	}

	private void loopOverExpressions(final Iterable<? extends XExpression> expressions,
			final InitializedVariables initialized, final NotInitializedAcceptor acceptor) {
		for (var e : expressions) {
			detectNotInitializedDispatch(e, initialized, acceptor);
		}
	}

	private InitializedVariables createCopy(final InitializedVariables initialized) {
		var copy = new InitializedVariables();
		copy.addAll(initialized);
		return copy;
	}
}
