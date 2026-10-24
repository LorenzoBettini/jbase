package jbase.controlflow;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import org.eclipse.xtext.xbase.XExpression;
import org.eclipse.xtext.xbase.XSwitchExpression;
import org.eclipse.xtext.xbase.typesystem.IBatchTypeResolver;

import com.google.inject.Inject;

import jbase.jbase.XJCasePart;
import jbase.typesystem.JbaseSealedTypeResolver;
import jbase.util.JbaseSwitches;

/**
 * Customization to take into account that in Java, switch's cases automatically fall through
 * without an explicit break; it also takes into consideration break statements in loops.
 *
 * @author Lorenzo Bettini
 */
public class JbaseEarlyExitComputer extends JbaseSemicolonStatementAwareEarlyExitComputer {

	@Inject
	private IBatchTypeResolver typeResolver;
	@Inject
	private JbaseSealedTypeResolver sealedTypeResolver;

	@Inject
	private JbaseBranchingStatementDetector branchingStatementDetector;
	@Inject
	private JbaseBreakStatementDetector breakStatementDetector;

	/**
	 * Computes exit points using Java fall-through and loop-break handling.
	 * A possible break prevents a loop from being treated as an unconditional exit.
	 * Switch-expression yields are local results rather than exits from the enclosing method.
	 *
	 * @param expression the expression whose exit points are required
	 * @return the exit points established by the analysis, possibly empty
	 */
	@Override
	public Collection<ExitPoint> getExitPoints(final XExpression expression) {
		var exitPoints = super.getExitPoints(expression);
		var head = exitPoints.isEmpty() ? null : exitPoints.iterator().next();
		if (head != null && head.getExpression() == expression
				&& breakStatementDetector.containsPossibleBreakStatement(expression)) {
			return Collections.emptyList();
		}
		return exitPoints;
	}

	@Override
	protected Collection<ExitPoint> _exitPoints(final XSwitchExpression expression) {
		if (JbaseSwitches.isExpression(expression)) {
			return Collections.emptyList();
		}
		var result = new ArrayList<ExitPoint>();
		for (var casePart : expression.getCases()) {
			var then = casePart.getThen();
			var caseExit = getExitPoints(then);
			// if there is not a break then in Java it is an automatic fall through
			// so we must not consider this case
			if ((((XJCasePart) casePart).isArrow()
					|| branchingStatementDetector.isSureBranchStatement(then))
					&& !isNotEmpty(caseExit)) {
				return Collections.emptyList();
			}
			result.addAll(caseExit);
		}
		if (expression.getDefault() == null && JbaseSwitches.isEnhanced(expression)
				&& JbaseSwitches.isExhaustive(expression, typeResolver.resolveTypes(expression),
					sealedTypeResolver)) {
			return result;
		}
		var defaultExit = getExitPoints(expression.getDefault());
		if (!isNotEmpty(defaultExit)) {
			return Collections.emptyList();
		}
		result.addAll(defaultExit);
		return result;
	}
}
