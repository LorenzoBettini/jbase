package jbase.typesystem;

import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.xtext.common.types.JvmIdentifiableElement;
import org.eclipse.xtext.common.types.util.Primitives;
import org.eclipse.xtext.diagnostics.Severity;
import org.eclipse.xtext.validation.EObjectDiagnosticImpl;
import org.eclipse.xtext.xbase.XAbstractFeatureCall;
import org.eclipse.xtext.xbase.XExpression;
import org.eclipse.xtext.xbase.XFeatureCall;
import org.eclipse.xtext.xbase.XInstanceOfExpression;
import org.eclipse.xtext.xbase.XStringLiteral;
import org.eclipse.xtext.xbase.XSwitchExpression;
import org.eclipse.xtext.xbase.XVariableDeclaration;
import org.eclipse.xtext.xbase.XbasePackage;
import org.eclipse.xtext.xbase.typesystem.computation.IFeatureLinkingCandidate;
import org.eclipse.xtext.xbase.typesystem.computation.ILinkingCandidate;
import org.eclipse.xtext.xbase.typesystem.computation.ITypeComputationResult;
import org.eclipse.xtext.xbase.typesystem.computation.ITypeComputationState;
import org.eclipse.xtext.xbase.typesystem.internal.AbstractTypeComputationState;
import org.eclipse.xtext.xbase.typesystem.internal.ExpressionTypeComputationState;
import org.eclipse.xtext.xbase.typesystem.references.ArrayTypeReference;
import org.eclipse.xtext.xbase.typesystem.references.ITypeReferenceOwner;
import org.eclipse.xtext.xbase.typesystem.references.LightweightTypeReference;
import org.eclipse.xtext.xbase.typesystem.util.CommonTypeComputationServices;

import com.google.inject.Inject;

import jbase.controlflow.JbaseBranchingStatementDetector;
import jbase.jbase.JbasePackage;
import jbase.jbase.XJArrayAccess;
import jbase.jbase.XJArrayAccessExpression;
import jbase.jbase.XJArrayConstructorCall;
import jbase.jbase.XJAssignment;
import jbase.jbase.XJBranchingStatement;
import jbase.jbase.XJCharLiteral;
import jbase.jbase.XJClassObject;
import jbase.jbase.XJSemicolonStatement;
import jbase.jbase.XJVariableDeclaration;
import jbase.validation.JbaseIssueCodes;

/**
 * @author Lorenzo Bettini
 */
public class JbaseTypeComputer extends PatchedTypeComputer {

	@Inject
	private CommonTypeComputationServices services;
	@Inject
	private JbaseBranchingStatementDetector branchingStatementDetector;

	@Override
	public void computeTypes(final XExpression expression, final ITypeComputationState state) {
		switch (expression) {
			case XJAssignment assignment -> computeJbaseTypes(assignment, state);
			case XJArrayConstructorCall call -> computeJbaseTypes(call, state);
			case XJArrayAccessExpression access -> computeJbaseTypes(access, state);
			case XJBranchingStatement statement -> state.acceptActualType(getPrimitiveVoid(state));
			case XJCharLiteral literal -> computeCharLiteralTypes(state);
			case XJVariableDeclaration declaration -> computeJbaseTypes(declaration, state);
			case XJClassObject classObject -> computeJbaseTypes(classObject, state);
			case XJSemicolonStatement statement -> computeJbaseTypes(statement, state);
			case null, default -> super.computeTypes(expression, state);
		}
	}

	@Override
	protected void _computeTypes(final XAbstractFeatureCall featureCall,
			final ITypeComputationState state) {
		var candidates = state.getLinkingCandidates(featureCall);
		var best = (IFeatureLinkingCandidate) getBestCandidate(candidates);
		if (best.isTypeLiteral()) {
			// in Xbase when a type name can be used as a type literal
			// in Java type literals, a.k.a. class object, must have .class
			var containingFeature = featureCall.eContainingFeature();
			if (containingFeature.equals(XbasePackage.Literals.XVARIABLE_DECLARATION__RIGHT)
					|| containingFeature.equals(
						XbasePackage.Literals.XFEATURE_CALL__FEATURE_CALL_ARGUMENTS)
					|| containingFeature.equals(
						XbasePackage.Literals.XMEMBER_FEATURE_CALL__MEMBER_CALL_ARGUMENTS)) {
				var diagnostic = new EObjectDiagnosticImpl(Severity.ERROR,
					JbaseIssueCodes.INCOMPLETE_CLASS_OBJECT,
					"Syntax error, insert \".class\" to complete Expression",
					featureCall, null, -1, null);
				state.addDiagnostic(diagnostic);
			}
		}
		best.applyToComputationState();
	}

	/**
	 * In our case an XStringLiteral is always a String
	 */
	@Override
	protected void _computeTypes(final XStringLiteral object, final ITypeComputationState state) {
		var result = getTypeForName(String.class, state);
		state.acceptActualType(result);
	}

	@Override
	protected void addLocalToCurrentScope(final XVariableDeclaration localVariable,
			final ITypeComputationState state) {
		super.addLocalToCurrentScope(localVariable, state);
		if (localVariable instanceof XJVariableDeclaration declaration) {
			for (var additional : declaration.getAdditionalVariables()) {
				addLocalToCurrentScope(additional, state);
			}
		}
	}

	@Override
	protected void addLocalToCurrentScope(final XExpression e, final ITypeComputationState state) {
		if (e instanceof XJSemicolonStatement statement) {
			addLocalToCurrentScope(statement.getExpression(), state);
		} else {
			super.addLocalToCurrentScope(e, state);
		}
	}

	/**
	 * In Jbase the switch statement is simpler, and it must type check the
	 * case expressions
	 */
	@Override
	protected void _computeTypes(final XSwitchExpression object, final ITypeComputationState state) {
		var switchExpressionState = state.withNonVoidExpectation();
		var computedType = switchExpressionState.computeTypes(object.getSwitch());
		var allCasePartsState = state;
		var expressionType = computedType.getActualExpressionType();
		allCasePartsState.withinScope(object);

//		var BranchExpressionProcessor branchExpressionProcessor = null
//		if (object.getDefault() == null) {
//			branchExpressionProcessor = new BranchExpressionProcessor(state, object) {
//				override protected String getMessage() {
//					return "Missing default branch for switch expression with primitive type";
//				}
//			}
//		}

		for (var casePart : getCases(object)) {
			// assign the type for the switch expression if possible and use that one
			// for the remaining things
			var casePartState = allCasePartsState.withTypeCheckpoint(casePart);

			// Xbase: val caseState = casePartState.withNonVoidExpectation();
			// we must use the type of the switch's expression to type the case part
			var caseState = casePartState.withExpectation(expressionType);
			caseState.withinScope(casePart);
			if (casePart.getCase() != null) {
				caseState.computeTypes(casePart.getCase());
			}
			var then = casePart.getThen();
			// then is never null
			if (branchingStatementDetector.isSureBranchStatement(then)) {
				var thenState = allCasePartsState.withTypeCheckpoint(casePart);
				thenState.afterScope(casePart);
				thenState.computeTypes(then);
			} else {
				// Since in Java without a break we fall through on the next case or default
				// then we must influence the typing of the all switch expression,
				// e.g., this code must be valid if an int is expected
				// switch (p) {
				// 		case 0: System.out.println("0"); // the default is executed anyway
				//		default: return -1;
				// }
				allCasePartsState.withoutExpectation().computeTypes(then);
			}
//				if (branchExpressionProcessor != null) {
//					branchExpressionProcessor.process(thenResult);
//				}
		}
		var defaultCase = object.getDefault();
		if (defaultCase != null) {
			allCasePartsState.computeTypes(defaultCase);
		} else { // if (branchExpressionProcessor != null) {
			// branchExpressionProcessor.commit();
			for (var expectation : state.getExpectations()) {
				if (expectation.getExpectedType() != null) {
					var diagnostic = new EObjectDiagnosticImpl(Severity.ERROR,
						JbaseIssueCodes.MISSING_DEFAULT,
						"Missing default branch in the presence of expected type "
							+ expressionType.getSimpleName(),
						object, null, -1, null);
					state.addDiagnostic(diagnostic);
					break;
				}
			}
		}
	}

	/**
	 * In Javamm we must keep the semantics and typing of Java for instanceof expressions,
	 * which do not imply a subsequent implicit cast like in Xbase.
	 */
	@Override
	protected ITypeComputationState reassignCheckedType(final XExpression condition,
			final XExpression guardedExpression, final ITypeComputationState state) {
		if (condition instanceof XInstanceOfExpression) {
			return state;
		}
		return super.reassignCheckedType(condition, guardedExpression, state);
	}

	private void computeJbaseTypes(final XJVariableDeclaration object,
			final ITypeComputationState state) {
		super._computeTypes(object, state);
		// and also compute types for possible additional declarations
		for (var additional : object.getAdditionalVariables()) {
			state.withoutExpectation().computeTypes(additional);
		}
	}

	/**
	 * We must consider possible type expectations since a char literal can be
	 * assigned also to a primitive numeric type.
	 */
	private void computeCharLiteralTypes(final ITypeComputationState state) {
		for (var expectation : state.getExpectations()) {
			var typeExpectation = expectation.getExpectedType();
			if (typeExpectation == null) {
				continue;
			}
			var primitive = typeExpectation.getPrimitiveKind();
			if (primitive != null && primitive != Primitives.Primitive.Void
					&& primitive != Primitives.Primitive.Boolean) {
				state.acceptActualType(typeExpectation);
				return;
			}
		}
		var result = getTypeForName(Character.TYPE, state);
		state.acceptActualType(result);
	}

	private LightweightTypeReference computeJbaseTypes(final XJAssignment assignment,
			final ITypeComputationState state) {
		var candidates = state.getLinkingCandidates(assignment);
		var best = getBestCandidate(candidates);
		best.applyToComputationState();
		return computeTypesOfArrayAccess(assignment, best, state,
			XbasePackage.Literals.XASSIGNMENT__ASSIGNABLE);
	}

	private void computeJbaseTypes(final XJArrayAccessExpression arrayAccess,
			final ITypeComputationState state) {
		var actualType = state.withNonVoidExpectation().computeTypes(arrayAccess.getArray())
			.getActualExpressionType();
		// arrayAccess.array can be null in expressions of the shape
		// ()[0]
		if (actualType == null) {
			state.acceptActualType(getPrimitiveVoid(state));
		} else {
			var type = componentTypeOfArrayAccess(arrayAccess, actualType, state,
				XbasePackage.Literals.XABSTRACT_FEATURE_CALL__FEATURE);
			state.acceptActualType(type);
		}
		checkArrayIndexHasTypeInt(arrayAccess, state);
	}

	private void computeJbaseTypes(final XJArrayConstructorCall call,
			final ITypeComputationState state) {
		checkArrayIndexHasTypeInt(call, state);
		var typeReference = services.getTypeReferences().createTypeRef(call.getType());
		var lightweight = getReferenceOwner(state).toLightweightTypeReference(typeReference);
		var arrayTypeRef = lightweight;
		for (var i = 0; i < call.getDimensions().size(); i++) {
			arrayTypeRef = getReferenceOwner(state).newArrayTypeReference(arrayTypeRef);
		}
		if (call.getArrayLiteral() != null) {
			state.withExpectation(arrayTypeRef).computeTypes(call.getArrayLiteral());
		}
		state.acceptActualType(arrayTypeRef);
	}

	private void computeJbaseTypes(final XJClassObject e, final ITypeComputationState state) {
		var typeExpression = e.getTypeExpression();
		var typeExpressionType = state
			.withExpectation(state.getReferenceOwner().newReferenceTo(Class.class))
			.computeTypes(typeExpression).getActualExpressionType();
		// given Class<T>...
		var typeArguments = typeExpressionType.getTypeArguments();
		var typeArgument = typeArguments.isEmpty() ? null : typeArguments.getFirst();
		if (typeArgument != null) {
			// ...we possibly create as result Class<T[]...>
			var arrayTypeRef = typeArgument;
			for (var i = 0; i < e.getArrayDimensions().size(); i++) {
				arrayTypeRef = getReferenceOwner(state).newArrayTypeReference(arrayTypeRef);
			}
			var resultType = getReferenceOwner(state)
				.newParameterizedTypeReference(typeExpressionType.getType());
			resultType.addTypeArgument(arrayTypeRef);
			state.acceptActualType(resultType);
		} else {
			// in this case the typeExpressionType is not of the shape
			// Class<T> and an error has already been stored in the state
			// here we gracefully set the typeExpressionType as a result
			state.acceptActualType(typeExpressionType);
		}
		if (!(typeExpression instanceof XFeatureCall)) {
			var diagnostic = new EObjectDiagnosticImpl(Severity.ERROR,
				JbaseIssueCodes.INVALID_CLASS_OBJECT_EXPRESSION, "Expected type before .class",
				e, JbasePackage.Literals.XJ_CLASS_OBJECT__TYPE_EXPRESSION, -1, null);
			state.addDiagnostic(diagnostic);
		}
	}

	private ITypeReferenceOwner getReferenceOwner(final ITypeComputationState state) {
		return state.getReferenceOwner();
	}

	private ITypeComputationResult computeJbaseTypes(final XJSemicolonStatement st,
			final ITypeComputationState state) {
		var expression = st.getExpression();
		if (expression != null) {
			// it is crucial to specify withinScope, otherwise, FeatureScopeTracker
			// (used for the content assist) won't work
			// replacePreviousExpressionScope would throw an IllegalStateException
			// when the anchor is AFTER
			state.withinScope(st);
			return state.computeTypes(expression);
		} else {
			// empty statement
			state.acceptActualType(getPrimitiveVoid(state));
			return null;
		}
	}

	private LightweightTypeReference computeTypesOfArrayAccess(final XJArrayAccess arrayAccess,
			final ILinkingCandidate best, final ITypeComputationState state,
			final EStructuralFeature featureForError) {
		checkArrayIndexHasTypeInt(arrayAccess, state);
		var expressionState = (ExpressionTypeComputationState) state;
		var featureType = getDeclaredType(best.getFeature(), expressionState);
		return componentTypeOfArrayAccess(arrayAccess, featureType, state, featureForError);
	}

	private LightweightTypeReference componentTypeOfArrayAccess(final XJArrayAccess arrayAccess,
			final LightweightTypeReference type, final ITypeComputationState state,
			final EStructuralFeature featureForError) {
		var currentType = type;
		// Each index removes one array dimension: for int[][] a, a[0] has type int[],
		// while a[0][1] has type int. Only the number of indexes matters here;
		// their expressions are checked separately by checkArrayIndexHasTypeInt.
		var size = arrayAccess.getIndexes().size();
		for (var i = 0; i < size; i++) {
			if (currentType instanceof ArrayTypeReference arrayType) {
				currentType = arrayType.getComponentType();
			} else {
				// An index applied after all array dimensions are consumed is invalid.
				var diagnostic = new EObjectDiagnosticImpl(Severity.ERROR,
					JbaseIssueCodes.NOT_ARRAY_TYPE,
					"The type of the expression must be an array type but it resolved to "
						+ currentType.getSimpleName(),
					arrayAccess, featureForError, -1, null);
				state.addDiagnostic(diagnostic);
				return currentType;
			}
		}
		return currentType;
	}

	private void checkArrayIndexHasTypeInt(final XJArrayAccess arrayAccess,
			final ITypeComputationState state) {
		for (var index : arrayAccess.getIndexes()) {
			var conditionExpectation = state.withExpectation(getTypeForName(Integer.TYPE, state));
			conditionExpectation.computeTypes(index);
		}
	}

	private LightweightTypeReference getDeclaredType(final JvmIdentifiableElement feature,
			final AbstractTypeComputationState state) {
		var result = state.getResolvedTypes().getActualType(feature);
		return result == null ? state.getReferenceOwner().newAnyTypeReference() : result;
	}
}
