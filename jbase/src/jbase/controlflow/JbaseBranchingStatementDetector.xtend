package jbase.controlflow

import jbase.jbase.XJBranchingStatement
import jbase.jbase.XJSemicolonStatement
import org.eclipse.xtext.xbase.XBlockExpression
import org.eclipse.xtext.xbase.XExpression
import org.eclipse.xtext.xbase.XIfExpression

/**
 * @author Lorenzo Bettini
 */
class JbaseBranchingStatementDetector {
	
	def boolean isSureBranchStatement(XExpression e) {
		if (e == null)
			return false;
		return sureBranch(e)
	}

	def protected dispatch sureBranch(XExpression e) {
		return false
	}

	def protected dispatch sureBranch(XJBranchingStatement e) {
		return true
	}

	def protected dispatch sureBranch(XIfExpression e) {
		return isSureBranchStatement(e.then)
			&& (e.^else == null || isSureBranchStatement(e.^else))
	}

	def protected dispatch sureBranch(XBlockExpression e) {
		e.expressions.exists[isSureBranchStatement]
	}

	def protected dispatch sureBranch(XJSemicolonStatement e) {
		e.expression.isSureBranchStatement
	}
}