package jbase.typesystem;

import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.xtext.common.types.JvmEnumerationType;
import org.eclipse.xtext.common.types.JvmGenericType;
import org.eclipse.xtext.common.types.JvmIdentifiableElement;
import org.eclipse.xtext.common.types.JvmPrimitiveType;
import org.eclipse.xtext.common.types.util.Primitives;
import org.eclipse.xtext.diagnostics.Severity;
import org.eclipse.xtext.naming.QualifiedName;
import org.eclipse.xtext.validation.EObjectDiagnosticImpl;
import org.eclipse.xtext.xbase.XAbstractFeatureCall;
import org.eclipse.xtext.xbase.XBlockExpression;
import org.eclipse.xtext.xbase.XBinaryOperation;
import org.eclipse.xtext.xbase.XCastedExpression;
import org.eclipse.xtext.xbase.XClosure;
import org.eclipse.xtext.xbase.XExpression;
import org.eclipse.xtext.xbase.XFeatureCall;
import org.eclipse.xtext.xbase.XForLoopExpression;
import org.eclipse.xtext.xbase.XNullLiteral;
import org.eclipse.xtext.xbase.XStringLiteral;
import org.eclipse.xtext.xbase.XSwitchExpression;
import org.eclipse.xtext.xbase.XVariableDeclaration;
import org.eclipse.xtext.xbase.XbasePackage;
import org.eclipse.xtext.xbase.typesystem.computation.ClosureTypeComputer;
import org.eclipse.xtext.xbase.typesystem.conformance.ConformanceFlags;
import org.eclipse.xtext.xbase.typesystem.computation.EnumLiteralImporter;
import org.eclipse.xtext.xbase.typesystem.computation.IFeatureLinkingCandidate;
import org.eclipse.xtext.xbase.typesystem.computation.ILinkingCandidate;
import org.eclipse.xtext.xbase.typesystem.computation.ITypeComputationResult;
import org.eclipse.xtext.xbase.typesystem.computation.ITypeComputationState;
import org.eclipse.xtext.xbase.typesystem.internal.AbstractTypeComputationState;
import org.eclipse.xtext.xbase.typesystem.internal.ExpressionTypeComputationState;
import org.eclipse.xtext.xbase.typesystem.references.ArrayTypeReference;
import org.eclipse.xtext.xbase.typesystem.references.ITypeReferenceOwner;
import org.eclipse.xtext.xbase.typesystem.references.LightweightTypeReference;
import org.eclipse.xtext.xbase.typesystem.references.UnboundTypeReference;
import org.eclipse.xtext.xbase.typesystem.util.CommonTypeComputationServices;
import org.eclipse.xtext.xbase.validation.IssueCodes;

import com.google.inject.Inject;

import jbase.controlflow.JbaseBranchingStatementDetector;
import jbase.jbase.JbasePackage;
import jbase.jbase.XJArrayAccess;
import jbase.jbase.XJArrayAccessExpression;
import jbase.jbase.XJArrayConstructorCall;
import jbase.jbase.XJAssertStatement;
import jbase.jbase.XJAssignment;
import jbase.jbase.XJBranchingStatement;
import jbase.jbase.XJCasePart;
import jbase.jbase.XJCharLiteral;
import jbase.jbase.XJClassObject;
import jbase.jbase.XJConstructorCall;
import jbase.jbase.XJInstanceOfExpression;
import jbase.jbase.XJMethodReference;
import jbase.jbase.XJSemicolonStatement;
import jbase.jbase.XJVariableDeclaration;
import jbase.jbase.XJTryWithResourcesExistingResource;
import jbase.scoping.featurecalls.JbaseOperatorMapping;
import jbase.util.JbaseSwitches;
import jbase.validation.JbaseIssueCodes;

/**
 * Adapts Xbase computation to Java expressions and Jbase statement wrappers.
 * Ordinary lambdas retain Xbase's closure inference; method references are dispatched
 * to {@link JbaseMethodReferences} before that inference attempts to visit a lambda body.
 *
 * @author Lorenzo Bettini
 */
public class JbaseTypeComputer extends PatchedTypeComputer {

	@Inject
	private CommonTypeComputationServices services;
	@Inject
	private JbaseMethodReferences methodReferences;
	@Inject
	private JbaseBranchingStatementDetector branchingStatementDetector;

	@Inject
	private JbasePatternVariables patternVariables;

	@Override
	public void computeTypes(final XExpression expression, final ITypeComputationState originalState) {
		final var state = patternVariables.enterScope(expression, originalState);
		switch (expression) {
			case XJInstanceOfExpression instanceOf -> {
				super._computeTypes(instanceOf, state);
				patternVariables.assignType(instanceOf, state);
			}
			case XJAssignment assignment -> computeJbaseTypes(assignment, state);
			case XJArrayConstructorCall call -> computeJbaseTypes(call, state);
			case XJArrayAccessExpression access -> computeJbaseTypes(access, state);
			case XJBranchingStatement statement -> state.acceptActualType(getPrimitiveVoid(state));
			case XJAssertStatement statement -> computeJbaseTypes(statement, state);
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
		var linkingState = state;
		if (featureCall instanceof XBinaryOperation operation
				&& (operation.getLeftOperand() instanceof XJArrayAccessExpression
					|| JbaseOperatorMapping.SHIFT_ASSIGNMENTS.contains(
						operation.getConcreteSyntaxFeatureName()))
				&& services.getOperatorMapping().getCompoundOperators()
					.contains(QualifiedName.create(operation.getConcreteSyntaxFeatureName()))) {
			// Retain the lvalue's type and index types before numeric operator promotion.
			state.withNonVoidExpectation().computeTypes(operation.getLeftOperand());
			linkingState = new JbaseCompoundAssignmentState(operation,
				(AbstractTypeComputationState) state);
		}
		var candidates = linkingState.getLinkingCandidates(featureCall);
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

	/**
	 * Supplies a functional interface expectation for a cast lambda or method reference.
	 * For example, {@code (IntUnaryOperator) n -> n} must infer n as int before computing
	 * the operand. An ordinary cast's operand computation does not supply this expectation.
	 * The type guard is needed while the editor is recovering an incomplete cast.
	 */
	@Override
	protected void _computeTypes(final XCastedExpression expression,
			final ITypeComputationState state) {
		if (expression.getTarget() instanceof XClosure && expression.getType() != null) {
			// Unlike an ordinary cast, a lambda cast supplies the target interface
			// from which Xbase must infer the lambda's parameter and return types.
			var targetType = state.getReferenceOwner()
				.toLightweightTypeReference(expression.getType());
			state.withExpectation(targetType).computeTypes(expression.getTarget());
			state.acceptActualType(targetType);
		} else {
			super._computeTypes(expression, state);
		}
	}

	/**
	 * Keeps closure inference tied to each enclosing expectation, including overload candidates.
	 * A raw functional interface uses erased bounds (raw Consumer's parameter is Object);
	 * other lambdas delegate to Xbase's ClosureTypeComputer with their original expectations.
	 * The reference subtype must be intercepted before Xbase looks for a lambda body.
	 */
	@Override
	protected void _computeTypes(final XClosure closure, final ITypeComputationState state) {
		if (closure instanceof XJMethodReference reference) {
			methodReferences.computeTypes(reference, state);
			return;
		}
		for (var expectation : state.getExpectations()) {
			var expectedType = expectation.getExpectedType();
			if (expectedType != null && expectedType.isRawType()) {
				// Java infers erased parameter types for a raw functional interface.
				// Xbase would otherwise leave these parameters as unbound types.
				var owner = state.getReferenceOwner();
				var type = (JvmGenericType) expectedType.getRawTypeReference().getType();
				var parameterized = owner.newParameterizedTypeReference(type);
				for (var parameter : type.getTypeParameters()) {
					parameterized.addTypeArgument(owner.newParameterizedTypeReference(parameter)
						.getRawTypeReference());
				}
				super._computeTypes(closure, state.withExpectation(parameterized));
				state.acceptActualType(parameterized);
			} else {
				new ClosureTypeComputer(closure, expectation, state).computeTypes();
			}
		}
	}

	@Override
	protected void addLocalToCurrentScope(final XVariableDeclaration localVariable,
			final ITypeComputationState state) {
		if (localVariable instanceof XJTryWithResourcesExistingResource) {
			return;
		}
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
			patternVariables.addIntroducedByStatement(e, state);
		}
	}

	/**
	 * In Jbase the switch statement is simpler, and it must type check the
	 * case expressions
	 */
	@Override
	protected void _computeTypes(final XSwitchExpression object, final ITypeComputationState state) {
		if (JbaseSwitches.isEnhanced(object)) {
			computeEnhancedSwitch(object, state);
			return;
		}
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
		computeClassicSwitchDefault(object, expressionType, allCasePartsState, state);
	}

	private void computeClassicSwitchDefault(final XSwitchExpression object,
			final LightweightTypeReference expressionType,
			final ITypeComputationState allCasePartsState, final ITypeComputationState state) {
		var defaultCase = object.getDefault();
		if (defaultCase != null) {
			if (object.getCases().stream().anyMatch(part -> part.getCase() == null)) {
				// Keep useful scopes while an unfinished label has no result type yet.
				allCasePartsState.withoutExpectation().computeTypes(defaultCase);
			} else {
				allCasePartsState.computeTypes(defaultCase);
			}
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

	/** Java patterns bind a new local; they never refine the selector variable. */
	private void computeEnhancedSwitch(final XSwitchExpression expression,
			final ITypeComputationState state) {
		final var selectorType = state.withNonVoidExpectation()
			.computeTypes(expression.getSwitch()).getActualExpressionType();
		state.withinScope(expression);
		for (final var original : expression.getCases()) {
			final var part = (XJCasePart) original;
			var branch = state.withTypeCheckpoint(part);
			branch.withinScope(part);
			if (selectorType.getType() instanceof JvmEnumerationType enumeration) {
				branch.addImports(new EnumLiteralImporter(enumeration));
			}
			branch = computeEnhancedLabels(part, branch, selectorType, state);
			branch.afterScope(part);
			computeSwitchBody(expression, part.getThen(), branch);
		}
		if (expression.getDefault() != null) {
			computeSwitchBody(expression, expression.getDefault(), state);
		}
		if (!JbaseSwitches.isExpression(expression)) {
			state.acceptActualType(getPrimitiveVoid(state), ConformanceFlags.NO_IMPLICIT_RETURN);
		}
	}

	private ITypeComputationState computeEnhancedLabels(final XJCasePart part,
			final ITypeComputationState branch, final LightweightTypeReference selectorType,
			final ITypeComputationState state) {
		if (part.getTypeGuard() != null) {
			final var type = state.getReferenceOwner().toLightweightTypeReference(part.getTypeGuard());
			final ITypeComputationState typed;
			if (part.getRecordPattern() == null) {
				typed = branch.assignType(part.getPatternVariable(), type);
			} else {
				new JbaseRecordPatterns().assignTypes(part.getRecordPattern(), type, selectorType, branch);
				typed = branch.withTypeCheckpoint(part);
				JbaseRecordPatterns.bindings(part.getRecordPattern()).forEach(typed::addLocalToCurrentScope);
			}
			if (part.getCase() != null) {
				typed.withExpectation(getTypeForName(boolean.class, state))
					.computeTypes(part.getCase());
			}
			return typed;
		}
		// Labels remain constants: boxing would emit Integer.valueOf(1) as a case.
		final var labelType = selectorType.getPrimitiveIfWrapperType();
		branch.withExpectation(part.getCase() instanceof XNullLiteral ? selectorType : labelType)
			.computeTypes(part.getCase());
		for (final var label : part.getAdditionalLabels()) {
			branch.withExpectation(labelType).computeTypes(label);
		}
		return branch;
	}

	private void computeSwitchBody(final XSwitchExpression expression, final XExpression body,
			final ITypeComputationState state) {
		if (!JbaseSwitches.isExpression(expression)) {
			state.withoutExpectation().computeTypes(body);
		} else if (body instanceof XBlockExpression) {
			// A yield uses Xbase's return-type channel, isolated from the enclosing method.
			final var expected = state.getExpectations().getFirst().getExpectedType();
			final var local = expected == null ? state.withoutRootExpectation()
				: state.withRootExpectation(expected);
			final var result = local.withoutExpectation().computeTypes(body);
			state.acceptActualType(result.getReturnType());
		} else {
			state.computeTypes(body);
		}
	}

	/** Java instanceof never refines the checked operand's declared type. */
	@Override
	protected ITypeComputationState reassignCheckedType(final XExpression condition,
			final XExpression guardedExpression, final ITypeComputationState state) {
		return state;
	}

	@Override
	protected LightweightTypeReference computeForLoopParameterType(final XForLoopExpression loop,
			final ITypeComputationState state) {
		if (loop.getDeclaredParam().getParameterType() != null) {
			return super.computeForLoopParameterType(loop, state);
		}
		// The iterable has no target type in Java. In particular, a raw Iterable must
		// not acquire an unconstrained element type from an Iterable<?> expectation.
		final var type = inferLocalType(loop.getForExpression(), state);
		if (type == null) {
			return null;
		}
		if (!type.isArray() && !type.isSubtypeOf(Iterable.class)) {
			state.addDiagnostic(new EObjectDiagnosticImpl(Severity.ERROR,
				IssueCodes.INCOMPATIBLE_TYPES, "Can only iterate over an array or an Iterable",
				loop, XbasePackage.Literals.XFOR_LOOP_EXPRESSION__FOR_EXPRESSION, -1, null));
		}
		if (type.isRawType()) {
			return state.getReferenceOwner().newReferenceToObject();
		}
		return getElementOrComponentType(type, state.getReferenceOwner());
	}

	private void computeJbaseTypes(final XJVariableDeclaration object,
			final ITypeComputationState state) {
		if (object.getType() == null) {
			// Java var initializers have no target type, even in a resource declaration.
			state.withinScope(object);
			final var type = inferLocalType(object.getRight(), state);
			state.assignType(object, type, false);
			state.acceptActualType(getPrimitiveVoid(state));
		} else {
			super._computeTypes(object, state);
		}
		// and also compute types for possible additional declarations
		for (var additional : object.getAdditionalVariables()) {
			state.withoutExpectation().computeTypes(additional);
		}
	}

	private LightweightTypeReference inferLocalType(final XExpression initializer,
			final ITypeComputationState state) {
		final var type = state.withNonVoidExpectation().computeTypes(initializer)
			.getActualExpressionType();
		// Xbase treats omitted type arguments like a diamond. Java keeps them raw.
		if (initializer instanceof XJConstructorCall call && !call.isExplicitTypeArguments()) {
			return type.getRawTypeReference();
		}
		// Complete inference here so later uses cannot supply a target type for var.
		if (type != null) {
			resolveTypeArguments(type);
		}
		return type;
	}

	private void resolveTypeArguments(final LightweightTypeReference type) {
		if (type instanceof UnboundTypeReference unbound) {
			unbound.resolve();
		}
		for (final var argument : type.getTypeArguments()) {
			resolveTypeArguments(argument);
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
		final var primitiveType = typeExpression instanceof XFeatureCall featureCall
			&& ((AbstractTypeComputationState) state).getResolvedTypes()
				.getLinkedFeature(featureCall) instanceof JvmPrimitiveType;
		// given Class<T>...
		var typeArguments = typeExpressionType.getTypeArguments();
		var typeArgument = typeArguments.isEmpty() ? null : typeArguments.getFirst();
		if (typeArgument != null) {
			// ...we possibly create as result Class<T[]...>
			var arrayTypeRef = typeArgument;
			if (primitiveType && !e.getArrayDimensions().isEmpty()) {
				// int.class has type Class<Integer>, but int[].class has type Class<int[]>.
				arrayTypeRef = typeArgument.getPrimitiveIfWrapperType();
			}
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

	private void computeJbaseTypes(final XJAssertStatement statement,
			final ITypeComputationState state) {
		state.withExpectation(getTypeForName(boolean.class, state))
			.computeTypes(statement.getCondition());
		if (statement.getMessage() != null) {
			state.withNonVoidExpectation().computeTypes(statement.getMessage());
		}
		state.acceptActualType(getPrimitiveVoid(state));
	}

	/**
	 * Exposes the statement wrapper to Xbase's scope tracker before typing its expression.
	 * Lambda parameter and local completion depend on these tracked scopes, even though
	 * the semicolon wrapper has no independent expression semantics.
	 */
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
