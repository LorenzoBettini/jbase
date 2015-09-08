package jbase.typesystem

import com.google.inject.Inject
import jbase.controlflow.JbaseBranchingStatementDetector
import jbase.jbase.XJArrayAccess
import jbase.jbase.XJArrayAccessExpression
import jbase.jbase.XJArrayConstructorCall
import jbase.jbase.XJAssignment
import jbase.jbase.XJBranchingStatement
import jbase.jbase.XJCharLiteral
import jbase.jbase.XJMemberFeatureCall
import jbase.jbase.XJVariableDeclaration
import jbase.validation.JbaseIssueCodes
import org.eclipse.emf.ecore.EStructuralFeature
import org.eclipse.xtext.common.types.JvmIdentifiableElement
import org.eclipse.xtext.common.types.util.Primitives
import org.eclipse.xtext.diagnostics.Severity
import org.eclipse.xtext.validation.EObjectDiagnosticImpl
import org.eclipse.xtext.xbase.XExpression
import org.eclipse.xtext.xbase.XInstanceOfExpression
import org.eclipse.xtext.xbase.XStringLiteral
import org.eclipse.xtext.xbase.XSwitchExpression
import org.eclipse.xtext.xbase.XVariableDeclaration
import org.eclipse.xtext.xbase.XbasePackage
import org.eclipse.xtext.xbase.typesystem.computation.ILinkingCandidate
import org.eclipse.xtext.xbase.typesystem.computation.ITypeComputationState
import org.eclipse.xtext.xbase.typesystem.internal.AbstractTypeComputationState
import org.eclipse.xtext.xbase.typesystem.internal.ExpressionTypeComputationState
import org.eclipse.xtext.xbase.typesystem.references.ArrayTypeReference
import org.eclipse.xtext.xbase.typesystem.references.LightweightTypeReference
import org.eclipse.xtext.xbase.typesystem.util.CommonTypeComputationServices

/**
 * @author Lorenzo Bettini
 */
class JbaseTypeComputer extends PatchedTypeComputer {
	
	@Inject 
	private CommonTypeComputationServices services;

	@Inject extension JbaseBranchingStatementDetector
	
	override computeTypes(XExpression expression, ITypeComputationState state) {
		if (expression instanceof XJAssignment) {
			_computeTypes(expression, state)
		} else if (expression instanceof XJArrayConstructorCall) {
			_computeTypes(expression, state)
		} else if (expression instanceof XJArrayAccessExpression) {
			_computeTypes(expression, state)
		} else if (expression instanceof XJBranchingStatement) {
			_computeTypes(expression, state)
		} else if (expression instanceof XJCharLiteral) {
			_computeTypes(expression, state)
		} else if (expression instanceof XJVariableDeclaration) {
			_computeTypes(expression, state)
		} else if (expression instanceof XJMemberFeatureCall) {
			_computeTypes(expression, state)
		} else {
			super.computeTypes(expression, state)
		}
	}

	/**
	 * In our case an XStringLiteral is always a String
	 */
	override protected _computeTypes(XStringLiteral object, ITypeComputationState state) {
		val result = getTypeForName(String, state);
		state.acceptActualType(result);
	}

	override protected addLocalToCurrentScope(XVariableDeclaration localVariable, ITypeComputationState state) {
		super.addLocalToCurrentScope(localVariable, state)
		if (localVariable instanceof XJVariableDeclaration) {
			for (additional : localVariable.additionalVariables) {
				addLocalToCurrentScope(additional, state)
			}
		}
	}

	/**
	 * In Jbase the switch statement is simpler, and it must type check the
	 * case expressions 
	 */
	override protected _computeTypes(XSwitchExpression object, ITypeComputationState state) {
		val switchExpressionState = state.withNonVoidExpectation
		val computedType = switchExpressionState.computeTypes(object.getSwitch());
		
		val allCasePartsState = state;
		val expressionType = computedType.getActualExpressionType();
		
		allCasePartsState.withinScope(object);

//		var BranchExpressionProcessor branchExpressionProcessor = null
//		if (object.getDefault() == null) {
//			branchExpressionProcessor = new BranchExpressionProcessor(state, object) {
//				override protected String getMessage() {
//					return "Missing default branch for switch expression with primitive type";
//				}
//			}			
//		}
		
		val cases = getCases(object);
		for(var i = 0; i < cases.size(); i++) {
			val casePart = cases.get(i);
			// assign the type for the switch expression if possible and use that one for the remaining things
			val casePartState = allCasePartsState.withTypeCheckpoint(casePart);

			// Xbase: val caseState = casePartState.withNonVoidExpectation();
			// we must use the type of the switch's expression to type the case part
			val caseState = casePartState.withExpectation(expressionType)
			caseState.withinScope(casePart);
			if (casePart.getCase() != null) {
				caseState.computeTypes(casePart.getCase());
			}
			val then = casePart.getThen();
			// then is never null
			if (then.isSureBranchStatement) {
				val thenState = allCasePartsState.withTypeCheckpoint(casePart);
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
				allCasePartsState.withoutExpectation.computeTypes(then)
			}
//				if (branchExpressionProcessor != null) {
//					branchExpressionProcessor.process(thenResult);
//				}
		}
		val defaultCase = object.getDefault();
		if (defaultCase != null) {
			allCasePartsState.computeTypes(object.getDefault());
		} else { // if (branchExpressionProcessor != null) {
			// branchExpressionProcessor.commit();
			val typeExpectation = state.expectations.findFirst[expectedType != null]
			if (typeExpectation != null) {
				val diagnostic = new EObjectDiagnosticImpl(
					Severity.ERROR,
					JbaseIssueCodes.MISSING_DEFAULT, 
					"Missing default branch in the presence of expected type " + expressionType.simpleName,
					object,
					null,
					-1,
					null);
				state.addDiagnostic(diagnostic);
			}
		}
	}

	/**
	 * In Javamm we must keep the semantics and typing of Java for instanceof expressions,
	 * which do not imply a subsequent implicit cast like in Xbase.
	 */
	override protected reassignCheckedType(XExpression condition, XExpression guardedExpression, ITypeComputationState state) {
		if (condition instanceof XInstanceOfExpression) {
			return state
		}
		return super.reassignCheckedType(condition, guardedExpression, state)
	}

	def protected _computeTypes(XJVariableDeclaration object, ITypeComputationState state) {
		super._computeTypes(object, state)
		// and also compute types for possible additional declarations
		for (additional : object.additionalVariables) {
			state.withoutExpectation.computeTypes(additional)
		}
	}

	/**
	 * We must consider possible type expectations since a char literal can be
	 * assigned also to a primitive numeric type.
	 */
	def protected _computeTypes(XJCharLiteral object, ITypeComputationState state) {
		val expectations = state.expectations
		for (typeExpectation : expectations.map[expectedType].filterNull) {
			val primitive = typeExpectation.primitiveKind
			if (primitive != null && primitive != Primitives.Primitive.Void &&
				primitive != Primitives.Primitive.Boolean
			) {
				state.acceptActualType(typeExpectation)
				return;
			}
		}
		
		val result = getTypeForName(Character.TYPE, state);
		state.acceptActualType(result);
	}
	
	def protected _computeTypes(XJAssignment assignment, ITypeComputationState state) {
		val candidates = state.getLinkingCandidates(assignment);
		val best = getBestCandidate(candidates);
		best.applyToComputationState();
		computeTypesOfArrayAccess(assignment, best, state, XbasePackage.Literals.XASSIGNMENT__ASSIGNABLE)
	}

	def protected _computeTypes(XJMemberFeatureCall call, ITypeComputationState state) {
		super._computeTypes(call, state)
		checkArrayIndexHasTypeInt(call, state)
	}
	
	def protected _computeTypes(XJArrayAccessExpression arrayAccess, ITypeComputationState state) {
		val actualType = state.withNonVoidExpectation.computeTypes(arrayAccess.array).actualExpressionType
		val type = componentTypeOfArrayAccess(arrayAccess, actualType, state, XbasePackage.Literals.XABSTRACT_FEATURE_CALL__FEATURE)
		state.acceptActualType(type)

		checkArrayIndexHasTypeInt(arrayAccess, state);
	}

	def protected _computeTypes(XJArrayConstructorCall call, ITypeComputationState state) {
		checkArrayIndexHasTypeInt(call, state)
		val typeReference = services.typeReferences.createTypeRef(call.type)
		val lightweight = getReferenceOwner(state).toLightweightTypeReference(typeReference)
		var arrayTypeRef = lightweight
		for (i : 0..<call.dimensions.size) {
			arrayTypeRef = getReferenceOwner(state).newArrayTypeReference(arrayTypeRef)
		}
		if (call.arrayLiteral != null) {
			state.withExpectation(arrayTypeRef).computeTypes(call.arrayLiteral)
		}
		state.acceptActualType(arrayTypeRef)
	}
	
	private def getReferenceOwner(ITypeComputationState state) {
		state.referenceOwner
	}

	def protected _computeTypes(XJBranchingStatement st, ITypeComputationState state) {
		state.acceptActualType(state.primitiveVoid)
	}
	
	private def computeTypesOfArrayAccess(XJArrayAccess arrayAccess, 
		ILinkingCandidate best, ITypeComputationState state, EStructuralFeature featureForError
	) {
		checkArrayIndexHasTypeInt(arrayAccess, state);
		val expressionState = state as ExpressionTypeComputationState
		val featureType = getDeclaredType(best.feature, expressionState)
		componentTypeOfArrayAccess(arrayAccess, featureType, state, featureForError)
	}
	
	private def componentTypeOfArrayAccess(XJArrayAccess arrayAccess, LightweightTypeReference type, ITypeComputationState state, EStructuralFeature featureForError) {
		var currentType = type
		for (index : arrayAccess.indexes) {
			if (currentType instanceof ArrayTypeReference) {
				currentType = currentType.componentType
			} else {
				val diagnostic = new EObjectDiagnosticImpl(
					Severity.ERROR,
					JbaseIssueCodes.NOT_ARRAY_TYPE, 
					"The type of the expression must be an array type but it resolved to " + currentType.simpleName,
					arrayAccess,
					featureForError,
					-1,
					null);
				state.addDiagnostic(diagnostic);
				return currentType
			}
		}
		return currentType
	}
	
	private def checkArrayIndexHasTypeInt(XJArrayAccess arrayAccess, ITypeComputationState state) {
		for (index : arrayAccess.indexes) {
			val conditionExpectation = state.withExpectation(getTypeForName(Integer.TYPE, state))
			conditionExpectation.computeTypes(index)
		}
	}

	def private getDeclaredType(JvmIdentifiableElement feature, AbstractTypeComputationState state) {
		val result = state.getResolvedTypes().getActualType(feature);
		if (result == null) {
			return state.getReferenceOwner().newAnyTypeReference();
		}
		return result;
	}
	
}