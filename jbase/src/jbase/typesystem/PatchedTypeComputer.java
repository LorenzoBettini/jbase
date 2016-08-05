/**
 * 
 */
package jbase.typesystem;

import java.util.List;

import org.eclipse.xtext.common.types.JvmFormalParameter;
import org.eclipse.xtext.common.types.JvmPrimitiveType;
import org.eclipse.xtext.common.types.JvmTypeReference;
import org.eclipse.xtext.common.types.util.Primitives;
import org.eclipse.xtext.common.types.util.Primitives.Primitive;
import org.eclipse.xtext.xbase.XBinaryOperation;
import org.eclipse.xtext.xbase.XCatchClause;
import org.eclipse.xtext.xbase.XExpression;
import org.eclipse.xtext.xbase.XNumberLiteral;
import org.eclipse.xtext.xbase.XTryCatchFinallyExpression;
import org.eclipse.xtext.xbase.XUnaryOperation;
import org.eclipse.xtext.xbase.annotations.typesystem.XbaseWithAnnotationsTypeComputer;
import org.eclipse.xtext.xbase.typesystem.computation.ITypeComputationState;
import org.eclipse.xtext.xbase.typesystem.computation.ITypeExpectation;
import org.eclipse.xtext.xbase.typesystem.references.ITypeReferenceOwner;
import org.eclipse.xtext.xbase.typesystem.references.LightweightTypeReference;

import com.google.common.collect.Lists;
import com.google.inject.Inject;

import jbase.util.JbaseExpressionHelper;
import jbase.util.JbaseExpressionHelper.BaseCase;
import jbase.util.JbaseExpressionHelper.StepCase;

/**
 * Type computation for number literals takes expectations into account.
 * 
 * @author Lorenzo Bettini
 *
 */
public class PatchedTypeComputer extends XbaseWithAnnotationsTypeComputer {

	@Inject
	private Primitives primitives;

	@Inject
	private JbaseExpressionHelper expressionHelper;

	@Override
	public void computeTypes(XExpression expression, ITypeComputationState state) {
		if (expression instanceof XUnaryOperation) {
			_computeTypes((XUnaryOperation) expression, state);
		} else {
			super.computeTypes(expression, state);
		}
	}

	/**
	 * The original implementation in Xbase does not consider possible type
	 * expectations, failing to correctly type these cases, which are valid in
	 * Java:
	 * 
	 * <pre>
	 * byte b = -100;
	 * short s = -1000;
	 * </pre>
	 */
	protected void _computeTypes(XUnaryOperation unaryOperation, final ITypeComputationState state) {
		boolean specialHanlding = expressionHelper.specialHandling(
			unaryOperation, 
			new BaseCase() {
				@Override
				public Boolean apply(XUnaryOperation op, XNumberLiteral lit) {
					if (op.eContainer() instanceof XBinaryOperation) {
						// temporary fix for https://github.com/LorenzoBettini/javamm/issues/34
						return false;
					}
					List<? extends ITypeExpectation> expectations = state.getExpectations();
					for (ITypeExpectation typeExpectation : expectations) {
						LightweightTypeReference expectedType = typeExpectation.getExpectedType();
						if (expectedType != null && expectedType.getType() instanceof JvmPrimitiveType) {
							Primitive kind = primitives.primitiveKind((JvmPrimitiveType) expectedType.getType());
							String unaryOp = op.getConcreteSyntaxFeatureName();
							if (checkConversionToPrimitive(unaryOp + lit.getValue(), kind)) {
								state.withExpectation(expectedType).computeTypes(op.getOperand());
								state.acceptActualType(expectedType);
								return true;
							}
						}
					}
					return false;
				}
			},
			new StepCase() {
				@Override
				public void accept(XUnaryOperation t) {
				}
			}
		);

		if (!specialHanlding) {
			super._computeTypes(unaryOperation, state);
		}
	}

	/**
	 * The original implementation in Xbase does not consider possible type
	 * expectations, failing to correctly type these cases, which are valid in
	 * Java:
	 * 
	 * <pre>
	 * byte b = 100;
	 * short s = 1000;
	 * char c = 1000;
	 * </pre>
	 */
	@Override
	protected void _computeTypes(XNumberLiteral object, ITypeComputationState state) {
		if (object.eContainer() instanceof XBinaryOperation) {
			// temporary fix for https://github.com/LorenzoBettini/javamm/issues/34
			super._computeTypes(object, state);
			return;
		}

		List<? extends ITypeExpectation> expectations = state.getExpectations();
		for (ITypeExpectation typeExpectation : expectations) {
			LightweightTypeReference expectedType = typeExpectation.getExpectedType();
			if (expectedType != null && expectedType.getType() instanceof JvmPrimitiveType) {
				Primitive kind = primitives.primitiveKind((JvmPrimitiveType) expectedType.getType());
				if (checkConversionToPrimitive(object, kind)) {
					state.acceptActualType(expectedType);
					return;
				}
			}
		}

		super._computeTypes(object, state);
	}

	private boolean checkConversionToPrimitive(XNumberLiteral object, Primitive kind) {
		return checkConversionToPrimitive(object.getValue(), kind);
	}

	private boolean checkConversionToPrimitive(String value, Primitive kind) {
		boolean success = true;
		try {
			switch (kind) {
			case Byte:
				Byte.parseByte(value);
				break;
			case Short:
				Short.parseShort(value);
				break;
			case Char:
				int parsed = Integer.parseInt(value);
				success = parsed >= 0 && parsed <= Character.MAX_VALUE;
				break;
			default:
				success = false;
				break;
			}
		} catch (NumberFormatException e) {
			success = false;
		}
		return success;
	}

	@Override
	protected void _computeTypes(XTryCatchFinallyExpression object, ITypeComputationState state) {
		List<LightweightTypeReference> caughtExceptions = Lists.newArrayList();
		ITypeReferenceOwner referenceOwner = state.getReferenceOwner();
		for (XCatchClause catchClause : object.getCatchClauses())
			if (catchClause.getDeclaredParam() != null && catchClause.getDeclaredParam().getParameterType() != null)
				caughtExceptions.add(referenceOwner.toLightweightTypeReference(catchClause.getDeclaredParam().getParameterType()));
		state.withExpectedExceptions(caughtExceptions).computeTypes(object.getExpression());
		computeTypesForCatchFinally(object, state);
	}

	/**
	 * Common part for catch and finally, used both by try-catch-finally and
	 * try-with-resource
	 * 
	 * @param object
	 * @param state
	 */
	protected void computeTypesForCatchFinally(XTryCatchFinallyExpression object, ITypeComputationState state) {
		ITypeReferenceOwner referenceOwner = state.getReferenceOwner();
		for (XCatchClause catchClause : object.getCatchClauses()) {
			JvmFormalParameter catchClauseParam = catchClause.getDeclaredParam();
			JvmTypeReference parameterType = catchClauseParam.getParameterType();
			LightweightTypeReference lightweightReference = referenceOwner.toLightweightTypeReference(parameterType);
			ITypeComputationState catchClauseState = assignType(catchClauseParam, lightweightReference, state);
			catchClauseState.withinScope(catchClause);
			catchClauseState.computeTypes(catchClause.getExpression());
		}
		// TODO validate / handle return / throw in finally block
		state.withoutExpectation().computeTypes(object.getFinallyExpression());
	}

}
