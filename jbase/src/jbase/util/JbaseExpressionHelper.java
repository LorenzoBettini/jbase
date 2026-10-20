package jbase.util;

import org.eclipse.xtext.util.IAcceptor;
import org.eclipse.xtext.xbase.XNumberLiteral;
import org.eclipse.xtext.xbase.XUnaryOperation;
import org.eclipse.xtext.xbase.lib.Functions.Function2;

/**
 * Utility methods for expressions used both for typing and for code generation.
 *
 * @author Lorenzo Bettini
 *
 */
public class JbaseExpressionHelper {

	public abstract static class BaseCase
			implements Function2<XUnaryOperation, XNumberLiteral, Boolean> {
	}

	public abstract static class StepCase implements IAcceptor<XUnaryOperation> {
	}

	/**
	 * Whether this unary operation requires special handling
	 */
	public boolean specialHandling(final XUnaryOperation unaryOperation) {
		return specialHandling(unaryOperation, new BaseCase() {
			@Override
			public Boolean apply(final XUnaryOperation operation, final XNumberLiteral literal) {
				return true;
			}
		}, new StepCase() {
			@Override
			public void accept(final XUnaryOperation operation) {
				// Only checking whether special handling is needed; no step action is required.
			}
		});
	}

	/**
	 * Whether this unary operation requires special handling; while checking
	 * it also executes the passed lambdas.
	 *
	 * The base case is responsible for specifying whether in this particular execution the
	 * base case has performed custom operations.
	 *
	 * The step case is executed after the base case executed and returned true, in
	 * case of a successful recursive invocation.
	 *
	 * For example, given this unary operation <pre>-(+1)</pre>
	 * and this (Xtend) code
	 *
	 * <pre>
	 * val buffer = new StringBuilder
	 * helper.specialHandling(it as XUnaryOperation,
	 *   [ unaryOperation, numLiteral |
	 *     buffer.append(unaryOperation.getConcreteSyntaxFeatureName + numLiteral.value); return true
	 *   ],
	 *   [ unaryOperation |
	 *     buffer.insert(0, unaryOperation.getConcreteSyntaxFeatureName + "(").
	 *            append(")")
	 *   ]
	 * )
	 * </pre>
	 *
	 * Then the buffer will contain <pre>-(+1)</pre>.
	 *
	 * Given <pre>-1</pre> the buffer will contain <pre>-1</pre> and the step case will not
	 * be executed.
	 */
	public boolean specialHandling(final XUnaryOperation unaryOperation,
			final BaseCase baseCase, final StepCase stepCase) {
		// don't get the feature since that would require linking resolution
		// get the original program text instead
		var unaryOp = unaryOperation.getConcreteSyntaxFeatureName();
		var operand = unaryOperation.getOperand();

		if ("!".equals(unaryOp)) {
			return false;
		}
		if (operand instanceof XNumberLiteral literal) {
			return baseCase.apply(unaryOperation, literal);
		} else if (operand instanceof XUnaryOperation operation && specialHandling(operation, baseCase, stepCase)) {
			stepCase.accept(unaryOperation);
			return true;
		}
		return false;
	}
}
