package jbase.validation

import com.google.inject.Inject
import java.util.HashSet
import jbase.controlflow.JbaseBranchingStatementDetector
import jbase.jbase.XJAdditionalXVariableDeclaration
import jbase.jbase.XJArrayAccess
import jbase.jbase.XJTryWithResourcesStatement
import jbase.jbase.XJVariableDeclaration
import jbase.validation.JbaseInitializedVariableFinder.InitializedVariables
import jbase.validation.JbaseInitializedVariableFinder.NotInitializedAcceptor
import org.eclipse.xtext.xbase.XAbstractFeatureCall
import org.eclipse.xtext.xbase.XAssignment
import org.eclipse.xtext.xbase.XBasicForLoopExpression
import org.eclipse.xtext.xbase.XBlockExpression
import org.eclipse.xtext.xbase.XDoWhileExpression
import org.eclipse.xtext.xbase.XExpression
import org.eclipse.xtext.xbase.XForLoopExpression
import org.eclipse.xtext.xbase.XIfExpression
import org.eclipse.xtext.xbase.XSwitchExpression
import org.eclipse.xtext.xbase.XSynchronizedExpression
import org.eclipse.xtext.xbase.XTryCatchFinallyExpression
import org.eclipse.xtext.xbase.XVariableDeclaration
import org.eclipse.xtext.xbase.XWhileExpression

/**
 * Detects references to variables which might not be initialized, according
 * to Java semantics.
 * 
 * @author Lorenzo Bettini
 */
class JbaseInitializedVariableFinder {

	@Inject extension JbaseBranchingStatementDetector

	interface NotInitializedAcceptor {
		def void accept(XAbstractFeatureCall call);
	}

	static class InitializedVariables extends HashSet<XVariableDeclaration> {
	}

	/**
	 * Calls the acceptor's accept method with all the references to
	 * variables which are not considered initialized.
	 * 
	 * Method to be called from clients.
	 */
	def void detectNotInitialized(XBlockExpression e, NotInitializedAcceptor acceptor) {
		detectNotInitializedDispatch(e, new InitializedVariables, acceptor)
	}

	/**
	 * We handle null case gracefully
	 */
	def void detectNotInitializedDispatch(XExpression e,
		InitializedVariables initialized, NotInitializedAcceptor acceptor) {
		if (e != null) {
			detectNotInitialized(e, initialized, acceptor)
		}
	}

	def dispatch void detectNotInitialized(XExpression e,
		InitializedVariables initialized, NotInitializedAcceptor acceptor) {
		inspectContents(e, initialized, acceptor)
	}

	def dispatch void detectNotInitialized(XAssignment e,
		InitializedVariables initialized, NotInitializedAcceptor acceptor) {
		checkArrayAccess(e, initialized, acceptor)
		val feature = e.feature
		if (feature instanceof XVariableDeclaration) {
			detectNotInitializedDispatch(
				e.value, initialized, acceptor
			)
			initialized += feature
		} else {
			inspectContents(e, initialized, acceptor)
		}
	}

	def dispatch void detectNotInitialized(XJVariableDeclaration e,
		InitializedVariables initialized, NotInitializedAcceptor acceptor) {
		inspectVariableDeclaration(e, initialized, acceptor)
		loopOverExpressions(
			e.additionalVariables, initialized, acceptor
		)
	}

	def dispatch void detectNotInitialized(XJAdditionalXVariableDeclaration e,
		InitializedVariables initialized, NotInitializedAcceptor acceptor) {
		inspectVariableDeclaration(e, initialized, acceptor)
	}

	def protected void inspectVariableDeclaration(XVariableDeclaration e,
		InitializedVariables initialized, NotInitializedAcceptor acceptor) {
		if (e.right != null) {
			detectNotInitializedDispatch(
				e.right, initialized, acceptor
			)
			initialized += e
		}
	}

	def dispatch void detectNotInitialized(XBasicForLoopExpression e,
		InitializedVariables initialized, NotInitializedAcceptor acceptor) {
		loopOverExpressions(e.initExpressions, initialized, acceptor)

		// discard information collected
		loopOverExpressions(
			newArrayList(e.expression) +
				e.updateExpressions +
				newArrayList(e.eachExpression),
			initialized.createCopy,
			acceptor
		)
	}

	def dispatch void detectNotInitialized(XForLoopExpression e,
		InitializedVariables initialized, NotInitializedAcceptor acceptor) {
		// discard information collected
		detectNotInitializedDispatch(e.eachExpression, initialized.createCopy, acceptor)
	}

	def dispatch void detectNotInitialized(XWhileExpression e,
		InitializedVariables initialized, NotInitializedAcceptor acceptor) {
		detectNotInitializedDispatch(e.predicate, initialized, acceptor)

		// discard information collected
		detectNotInitializedDispatch(e.body, initialized.createCopy, acceptor)
	}

	def dispatch void detectNotInitialized(XDoWhileExpression e,
		InitializedVariables initialized, NotInitializedAcceptor acceptor) {
		// use information collected, since the body is surely executed
		detectNotInitializedDispatch(e.body, initialized, acceptor)
		detectNotInitializedDispatch(e.predicate, initialized, acceptor)
	}

	def dispatch void detectNotInitialized(XSwitchExpression e, InitializedVariables initialized,
		NotInitializedAcceptor acceptor) {
		detectNotInitializedDispatch(e.^switch, initialized, acceptor)

		// we consider effective branches the cases that end with a break;
		// cases without a break will not be considered as branches, as in Java
		val effectiveOrNotEffectiveBranches =
			e.cases.map[then].groupBy[isSureBranchStatement]

		if (effectiveOrNotEffectiveBranches.get(true) == null) {
			effectiveOrNotEffectiveBranches.put(true, newArrayList)
		}
		if (effectiveOrNotEffectiveBranches.get(false) == null) {
			effectiveOrNotEffectiveBranches.put(false, newArrayList)
		}

		// adding the default case even if not specified, i.e., null
		// ensures that the intersection will be empty in case there is
		// a single branch (as expected).
		effectiveOrNotEffectiveBranches.get(true) += e.^default

		// the cases not considered as effective branches are simply inspected
		loopOverExpressions(
			effectiveOrNotEffectiveBranches.get(false), initialized.createCopy, acceptor
		)

		inspectBranchesAndIntersect(
			effectiveOrNotEffectiveBranches.get(true),
			initialized,
			acceptor
		)
	}

	def dispatch void detectNotInitialized(XBlockExpression b,
		InitializedVariables initialized, NotInitializedAcceptor acceptor) {
		loopOverExpressions(b.expressions, initialized, acceptor)
	}

	def dispatch void detectNotInitialized(XAbstractFeatureCall o,
			InitializedVariables initialized, NotInitializedAcceptor acceptor) {

		val actualArguments = o.actualArguments
		if (actualArguments.empty) {
			val feature = o.feature
			if (feature instanceof XVariableDeclaration) {
				if (!initialized.contains(feature)) {
					acceptor.accept(o)
				}
			}
		} else {
			loopOverExpressions(actualArguments, initialized, acceptor)
		}
	}

	def dispatch void detectNotInitialized(XIfExpression e,
			InitializedVariables initialized, NotInitializedAcceptor acceptor) {
		detectNotInitializedDispatch(e.^if, initialized, acceptor)

		inspectBranchesAndIntersect(
			newArrayList(e.then, e.^else), initialized, acceptor
		)
	}

	def dispatch void detectNotInitialized(XTryCatchFinallyExpression e, InitializedVariables initialized,
		NotInitializedAcceptor acceptor) {
		detectNotInitializedTryCatchCommon(e, initialized, acceptor) [
			initializedVariables |
			inspectTryCatchFinallyBranchesAndIntersect(e, initializedVariables, acceptor)
		]
	}

	def dispatch void detectNotInitialized(XJTryWithResourcesStatement e, InitializedVariables initialized,
		NotInitializedAcceptor acceptor) {
		detectNotInitializedTryCatchCommon(e, initialized, acceptor) [
			initializedVariables |
			loopOverExpressions(e.resourceDeclarations, initializedVariables, acceptor)
			inspectTryCatchFinallyBranchesAndIntersect(e, initializedVariables, acceptor)
		]
	}

	protected def void inspectTryCatchFinallyBranchesAndIntersect(XTryCatchFinallyExpression e, InitializedVariables initializedVariables, NotInitializedAcceptor acceptor) {
		inspectBranchesAndIntersect(
			newArrayList(e.expression) + e.catchClauses.map[expression],
			initializedVariables,
			acceptor
		)
	}

	def protected void detectNotInitializedTryCatchCommon(XTryCatchFinallyExpression e, InitializedVariables initialized,
		NotInitializedAcceptor acceptor, (InitializedVariables)=>void branchInspector) {
		val finallyExpression = e.finallyExpression
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
			val temporaryCopy = initialized.createCopy
			branchInspector.apply(temporaryCopy)
			// if present, the final block is always executed so we treat it as a block expression
			// without any information about try and catch
			detectNotInitializedDispatch(e.finallyExpression, initialized, acceptor)
			// now we can update the information for the following expressions
			// with what we had collected in try and catch
			initialized += temporaryCopy
		} else {
			branchInspector.apply(initialized)
		}
	}

	/**
	 * This is just the same as the generic case for XExpression, but we put it here
	 * all the same for documentation.
	 * 
	 * A synchronized block is always considered executed from the variable initialization point
	 * of view.
	 */
	def dispatch void detectNotInitialized(XSynchronizedExpression e,
		InitializedVariables initialized, NotInitializedAcceptor acceptor) {
		inspectContents(e, initialized, acceptor)
	}

	protected def inspectContents(XExpression e, InitializedVariables initialized, NotInitializedAcceptor acceptor) {
		val contents = e.eContents.filter(XExpression)
		loopOverExpressions(contents, initialized, acceptor)
	}

	protected def checkArrayAccess(XExpression e, InitializedVariables initialized, NotInitializedAcceptor acceptor) {
		if (e instanceof XJArrayAccess) {
			loopOverExpressions(e.indexes, initialized, acceptor)
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
	protected def void inspectBranchesAndIntersect(Iterable<? extends XExpression> branches,
		InitializedVariables initialized, NotInitializedAcceptor acceptor) {
		val intersection = 
			branches.
				map[inspectBranch(initialized, acceptor)].
				reduce[
					$0.retainAll($1) 
					$0
				]

		initialized += intersection
	}

	protected def inspectBranch(XExpression b, InitializedVariables initialized, NotInitializedAcceptor acceptor) {
		var copy = initialized.createCopy
		detectNotInitializedDispatch(b, copy, acceptor)
		return copy
	}

	def protected void loopOverExpressions(Iterable<? extends XExpression> expressions, InitializedVariables initialized,
		NotInitializedAcceptor acceptor) {
		for (e : expressions) {
			detectNotInitializedDispatch(e, initialized, acceptor)
		}
	}

	def private createCopy(InitializedVariables initialized) {
		new InitializedVariables() => [
			addAll(initialized)
		]
	}

}