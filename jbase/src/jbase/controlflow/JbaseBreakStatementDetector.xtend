package jbase.controlflow

import jbase.jbase.XJBreakStatement
import jbase.jbase.XJSemicolonStatement
import org.eclipse.xtext.xbase.XAbstractWhileExpression
import org.eclipse.xtext.xbase.XBasicForLoopExpression
import org.eclipse.xtext.xbase.XBlockExpression
import org.eclipse.xtext.xbase.XExpression
import org.eclipse.xtext.xbase.XIfExpression

/**
 * Whether in the passed expression a break is possibly executed.
 * 
 * @author Lorenzo Bettini
 */
class JbaseBreakStatementDetector {

	def boolean containsPossibleBreakStatement(XExpression e) {
		if (e == null)
			return false;
		return containsPossibleBreak(e)
	}

	def protected dispatch containsPossibleBreak(XExpression e) {
		return false
	}

	def protected dispatch containsPossibleBreak(XJSemicolonStatement e) {
		e.expression.containsPossibleBreakStatement
	}

	def protected dispatch containsPossibleBreak(XAbstractWhileExpression e) {
		return e.body.isPossibleBreakStatement
	}

	def protected dispatch containsPossibleBreak(XBasicForLoopExpression e) {
		return e.eachExpression.isPossibleBreakStatement
	}

	def boolean isPossibleBreakStatement(XExpression e) {
		if (e == null)
			return false;
		return possibleBreak(e)
	}

	def protected dispatch possibleBreak(XExpression e) {
		return false
	}

	def protected dispatch possibleBreak(XJBreakStatement e) {
		return true
	}

	def protected dispatch possibleBreak(XIfExpression e) {
		return isPossibleBreakStatement(e.then)
			|| (e.^else != null && isPossibleBreakStatement(e.^else))
	}

	def protected dispatch possibleBreak(XBlockExpression e) {
		e.expressions.exists[isPossibleBreakStatement]
	}

	def protected dispatch possibleBreak(XJSemicolonStatement e) {
		e.expression.isPossibleBreakStatement
	}

}
