/**
 * 
 */
package jbase.compiler;

import java.util.Set;

import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.xtext.EcoreUtil2;
import org.eclipse.xtext.common.types.JvmField;
import org.eclipse.xtext.common.types.JvmFormalParameter;
import org.eclipse.xtext.common.types.JvmIdentifiableElement;
import org.eclipse.xtext.common.types.JvmOperation;
import org.eclipse.xtext.common.types.JvmType;
import org.eclipse.xtext.common.types.JvmTypeReference;
import org.eclipse.xtext.common.types.TypesPackage;
import org.eclipse.xtext.generator.trace.ILocationData;
import org.eclipse.xtext.nodemodel.util.NodeModelUtils;
import org.eclipse.xtext.util.Strings;
import org.eclipse.xtext.util.Tuples;
import org.eclipse.xtext.xbase.XAbstractFeatureCall;
import org.eclipse.xtext.xbase.XAbstractWhileExpression;
import org.eclipse.xtext.xbase.XAssignment;
import org.eclipse.xtext.xbase.XBasicForLoopExpression;
import org.eclipse.xtext.xbase.XBinaryOperation;
import org.eclipse.xtext.xbase.XBlockExpression;
import org.eclipse.xtext.xbase.XCasePart;
import org.eclipse.xtext.xbase.XClosure;
import org.eclipse.xtext.xbase.XConstructorCall;
import org.eclipse.xtext.xbase.XExpression;
import org.eclipse.xtext.xbase.XFeatureCall;
import org.eclipse.xtext.xbase.XForLoopExpression;
import org.eclipse.xtext.xbase.XIfExpression;
import org.eclipse.xtext.xbase.XNumberLiteral;
import org.eclipse.xtext.xbase.XStringLiteral;
import org.eclipse.xtext.xbase.XSwitchExpression;
import org.eclipse.xtext.xbase.XThrowExpression;
import org.eclipse.xtext.xbase.XTryCatchFinallyExpression;
import org.eclipse.xtext.xbase.XUnaryOperation;
import org.eclipse.xtext.xbase.XVariableDeclaration;
import org.eclipse.xtext.xbase.XbasePackage;
import org.eclipse.xtext.xbase.compiler.FeatureCallCompiler;
import org.eclipse.xtext.xbase.compiler.IGeneratorConfigProvider;
import org.eclipse.xtext.xbase.compiler.JvmModelGenerator;
import org.eclipse.xtext.xbase.compiler.XbaseCompiler;
import org.eclipse.xtext.xbase.compiler.output.ITreeAppendable;
import org.eclipse.xtext.xbase.lib.IterableExtensions;
import org.eclipse.xtext.xbase.typesystem.references.LightweightTypeReference;
import org.eclipse.xtext.xbase.util.XbaseUsageCrossReferencer;

import com.google.common.base.Joiner;
import com.google.inject.Inject;

import jbase.controlflow.JbaseBranchingStatementDetector;
import jbase.conversion.JbaseTextBlockValueConverter;
import jbase.jbase.XJAnonymousClass;
import jbase.jbase.XJArrayAccess;
import jbase.jbase.XJArrayAccessExpression;
import jbase.jbase.XJArrayConstructorCall;
import jbase.jbase.XJArrayLiteral;
import jbase.jbase.XJAssertStatement;
import jbase.jbase.XJBreakStatement;
import jbase.jbase.XJCasePart;
import jbase.jbase.XJCharLiteral;
import jbase.jbase.XJClassObject;
import jbase.jbase.XJComponentPattern;
import jbase.jbase.XJConditionalExpression;
import jbase.jbase.XJContinueStatement;
import jbase.jbase.XJInstanceOfExpression;
import jbase.jbase.XJJvmFormalParameter;
import jbase.jbase.XJMethodReference;
import jbase.jbase.XJPrefixOperation;
import jbase.jbase.XJRecordPattern;
import jbase.jbase.XJSemicolonStatement;
import jbase.jbase.XJSwitchExpression;
import jbase.jbase.XJTryWithResourcesExistingResource;
import jbase.jbase.XJTryWithResourcesStatement;
import jbase.jbase.XJTryWithResourcesVariableDeclaration;
import jbase.jbase.XJVariableDeclaration;
import jbase.jbase.XJYieldStatement;
import jbase.jvmmodel.JbaseAnonymousClassUtil;
import jbase.scoping.featurecalls.JbaseOperatorMapping;
import jbase.util.JbaseExpressionHelper;
import jbase.util.JbaseExpressionHelper.BaseCase;
import jbase.util.JbaseExpressionHelper.StepCase;
import jbase.util.JbaseModelUtil;
import jbase.util.JbaseSwitches;

/**
 * Generates Java expressions and statements while retaining Xbase's ordinary lambda compiler.
 * Method references are intercepted before inherited XClosure dispatch and emitted with
 * native Java reference syntax. Lambda-related customizations preserve blank local
 * declarations and the control-flow boundaries of loops inside closures.
 * Type patterns retain native instanceof syntax and their boolean flow scopes.
 *
 * @author Lorenzo Bettini
 */
public class JbaseXbaseCompiler extends PatchedXbaseCompiler {

	@Inject
	private JbaseAnonymousClassUtil anonymousTypes;
	@Inject
	private JvmModelGenerator modelGenerator;
	@Inject
	private IGeneratorConfigProvider generatorConfigs;
	
	private static final String FINAL_MODIFIER = "final ";
	private static final String ASSIGNED_TRUE = " = true;";

	@Inject
	private JbaseModelUtil modelUtil;

	@Inject
	private JbaseBranchingStatementDetector branchingStatementDetector;

	@Inject
	private JbaseExpressionHelper expressionHelper;

	@Inject
	private JbaseTextBlockValueConverter textBlockValueConverter;

	@Override
	public ITreeAppendable compile(final XExpression expression, final ITreeAppendable appendable,
			final LightweightTypeReference returnType, final Set<JvmTypeReference> exceptions) {
		// Reserve binding names before any synthetic locals are allocated. Java permits
		// disjoint patterns with the same spelling, so emit each binding's original name.
		for (final var pattern : EcoreUtil2.getAllContentsOfType(
				expression, XJInstanceOfExpression.class)) {
			final var variable = pattern.getPatternVariable();
			if (variable != null && !appendable.hasObject(variable.getName())) {
				appendable.declareVariable(variable, variable.getName());
			}
		}
		for (final var part : EcoreUtil2.getAllContentsOfType(expression, XJCasePart.class)) {
			final var variable = part.getPatternVariable();
			if (variable != null && !appendable.hasObject(variable.getName())) {
				appendable.declareVariable(variable, variable.getName());
			}
		}
		for (final var component : EcoreUtil2.getAllContentsOfType(expression, XJComponentPattern.class)) {
			if (component.getRecordPattern() == null && !appendable.hasObject(component.getName())) {
				appendable.declareVariable(component, component.getName());
			}
		}
		return super.compile(expression, appendable, returnType, exceptions);
	}

	@Override
	protected String getReferenceName(final XExpression expression,
			final ITreeAppendable appendable) {
		if (expression instanceof XFeatureCall call
				&& (call.getFeature().eContainer() instanceof XJCasePart
					|| call.getFeature() instanceof XJComponentPattern)) {
			return call.getFeature().getSimpleName();
		}
		return super.getReferenceName(expression, appendable);
	}

	/**
	 * Prepares reference receivers as ordinary expressions instead of compiling a closure body.
	 * Typed qualifiers have no receiver expression to prepare. The corresponding expression
	 * hook emits the reference itself, including its creation-time receiver evaluation.
	 */
	@Override
	protected void doInternalToJavaStatement(final XExpression obj,
			final ITreeAppendable appendable, final boolean isReferenced) {
		switch (obj) {
			case XJAnonymousClass anonymous -> {
				internalToJavaStatement(anonymous.getConstructorCall(), appendable, isReferenced);
			}
			case XJArrayConstructorCall call -> { /* compile only as expression */ }
			case XJArrayAccessExpression access -> { /* compile only as expression */ }
			case XJClassObject classObject -> { /* compile only as expression */ }
			case XBinaryOperation operation when isNativeCompoundAssignment(operation) -> {
				// Keep the element read/checks ahead of the RHS, with no operand hoisting.
				if (!isReferenced) {
					appendable.newLine();
					featureCalltoJavaExpression(operation, appendable, false);
					appendable.append(";");
				}
			}
			case XJMethodReference reference -> {
				if (reference.getReceiver() != null) {
					internalToJavaStatement(reference.getReceiver(), appendable, true);
				}
			}
			case XJYieldStatement statement -> {
				if (!canCompileToJavaExpression(statement.getExpression(), appendable)) {
					internalToJavaStatement(statement.getExpression(), appendable, true);
				}
				appendable.newLine().append("yield ");
				internalToJavaExpression(statement.getExpression(), appendable);
				appendable.append(";");
			}
			case XJContinueStatement statement -> compileContinueStatement(statement, appendable);
			case XJBreakStatement statement -> compileBranchingStatement(appendable, "break");
			case XJAssertStatement statement -> compileAssertStatement(statement, appendable);
			case XJSemicolonStatement statement ->
				compileSemicolonStatement(statement, appendable, isReferenced);
			case null, default -> super.doInternalToJavaStatement(obj, appendable, isReferenced);
		}
	}

	/**
	 * Emits updates needed when a basic for has been transformed to a while loop.
	 * A continue in a lambda's own loop must not update an enclosing basic for: the nearest
	 * while/enhanced-for containment checks discard that unrelated outer loop first.
	 */
	private void compileContinueStatement(XJContinueStatement st, ITreeAppendable b) {
		XBasicForLoopExpression basicForLoop = EcoreUtil2.getContainerOfType(st, XBasicForLoopExpression.class);
		if (basicForLoop != null
				&& (EcoreUtil.isAncestor(basicForLoop,
					EcoreUtil2.getContainerOfType(st, XAbstractWhileExpression.class))
				|| EcoreUtil.isAncestor(basicForLoop,
					EcoreUtil2.getContainerOfType(st, XForLoopExpression.class)))) {
			// Only the loop targeted by continue may contribute update expressions.
			// In particular, a lambda's own loop must not update an enclosing for.
			basicForLoop = null;
		}
		
		if (basicForLoop != null && !canCompileToJavaBasicForStatement(basicForLoop, b)) {
			// the for loop is translated into a while statement, so, before
			// the continue; we must perform the update expressions and then
			// check the while condition.
			
			EList<XExpression> updateExpressions = basicForLoop.getUpdateExpressions();
			for (XExpression updateExpression : updateExpressions) {
				internalToJavaStatement(updateExpression, b, false);
			}
			
			final String varName = b.getName(basicForLoop);
			
			XExpression expression = basicForLoop.getExpression();
			if (expression != null) {
				internalToJavaStatement(expression, b, true);
				b.newLine().append(varName).append(" = ");
				internalToJavaExpression(expression, b);
				b.append(";");
			} else {
				b.newLine().append(varName).append(ASSIGNED_TRUE);
			}
		}
		compileBranchingStatement(b, "continue");
	}

	private void compileClassObject(XJClassObject e, ITreeAppendable b) {
		XAbstractFeatureCall featureCall = (XAbstractFeatureCall) e.getTypeExpression();
		// we must append it as a JvmType so that the corresponding import statement
		// is generated in the Java code
		b.append((JvmType) featureCall.getFeature()).
			append(Joiner.on("").join(e.getArrayDimensions())).append(".class");
	}

	private void compileBranchingStatement(ITreeAppendable b, String instruction) {
		b.newLine().append(instruction).append(";");
	}

	private void compileAssertStatement(final XJAssertStatement statement,
			final ITreeAppendable appendable) {
		appendable.newLine().append("assert ");
		internalToJavaExpression(statement.getCondition(), appendable);
		if (statement.getMessage() != null) {
			appendable.append(" : ");
			internalToJavaExpression(statement.getMessage(), appendable);
		}
		appendable.append(";");
	}

	private void compileSemicolonStatement(XJSemicolonStatement st, ITreeAppendable b,
			boolean isReferenced) {
		XExpression expression = st.getExpression();
		if (expression != null) {
			doInternalToJavaStatement(expression, b, isReferenced);
		} else {
			b.append(";");
		}
	}

	@Override
	protected void internalToConvertedExpression(final XExpression expression,
			final ITreeAppendable appendable, final LightweightTypeReference expected) {
		if (expression instanceof XSwitchExpression) {
			// Java switch expressions are poly expressions. Their yields already carry
			// the target conversion; converting the switch again can unbox twice.
			internalToConvertedExpression(expression, appendable.trace(expression, true));
		} else {
			super.internalToConvertedExpression(expression, appendable, expected);
		}
	}

	@Override
	protected void internalToConvertedExpression(final XExpression obj,
			final ITreeAppendable appendable) {
		switch (obj) {
			case XJAnonymousClass anonymous ->
				internalToJavaExpression(anonymous.getConstructorCall(), appendable);
			case XJArrayConstructorCall call -> compileJavaExpression(call, appendable);
			case XJArrayAccessExpression access -> compileJavaExpression(access, appendable);
			case XJCharLiteral literal -> compileJavaExpression(literal, appendable);
			case XStringLiteral literal -> compileStringLiteral(literal, appendable);
			case XJClassObject classObject -> compileClassObject(classObject, appendable);
			case XSwitchExpression expression ->
				compileEnhancedSwitch(expression, appendable);
			case XJMethodReference reference -> compileReference(reference, appendable);
			case XJInstanceOfExpression pattern -> compilePattern(pattern, appendable);
			case XJConditionalExpression conditional when needsNativeConditional(conditional) ->
				compileNativeConditional(conditional, appendable);
			case null, default -> super.internalToConvertedExpression(obj, appendable);
		}
	}

	@Override
	protected void constructorCallToJavaExpression(final XConstructorCall call,
			final ITreeAppendable appendable) {
		if (!(call.eContainer() instanceof XJAnonymousClass anonymous)) {
			super.constructorCallToJavaExpression(call, appendable);
			return;
		}
		compileAnonymousConstructorCall(call, anonymous, appendable);
	}

	private void compileAnonymousConstructorCall(final XConstructorCall call,
			final XJAnonymousClass anonymous, final ITreeAppendable appendable) {
		appendable.append("new ").append(anonymousTypes.getSuperType(anonymous));
		appendExplicitTypeArguments(anonymous, appendable);
		appendConstructorArguments(call, appendable);
		final var type = call.getConstructor().getDeclaringType();
		appendable.append(" ").openScope();
		final var config = generatorConfigs.get(anonymous);
		modelGenerator.assignThisAndSuper(appendable, type, config);
		modelGenerator.generateMembersInBody(type, appendable, config);
		appendable.closeScope();
	}

	private void appendExplicitTypeArguments(final XJAnonymousClass anonymous,
			final ITreeAppendable appendable) {
		final var creation = anonymous.getConstructorCall();
		if (!creation.isExplicitTypeArguments()) {
			return;
		}
		appendable.append("<");
		for (var i = 0; i < creation.getTypeArguments().size(); i++) {
			if (i != 0) {
				appendable.append(", ");
			}
			getTypeReferenceSerializer().serialize(creation.getTypeArguments().get(i),
				creation, appendable);
		}
		appendable.append(">");
	}

	private void appendConstructorArguments(final XConstructorCall call,
			final ITreeAppendable appendable) {
		appendable.append("(");
		for (var i = 0; i < call.getArguments().size(); i++) {
			if (i != 0) {
				appendable.append(", ");
			}
			internalToJavaExpression(call.getArguments().get(i), appendable);
		}
		appendable.append(")");
	}

	private void compileStringLiteral(final XStringLiteral literal,
			final ITreeAppendable appendable) {
		final var nodes = NodeModelUtils.findNodesForFeature(literal,
				XbasePackage.Literals.XSTRING_LITERAL__VALUE);
		if (!nodes.isEmpty() && nodes.getFirst().getText().startsWith("\"\"\"")) {
			var content = textBlockValueConverter.normalizedContent(nodes.getFirst().getText());
			if (!content.translateEscapes().equals(literal.getValue())) {
				// A model transformation may have changed the value without changing its node.
				content = textBlockValueConverter.normalizedContent(
						textBlockValueConverter.toString(literal.getValue()));
			}
			// TreeAppendable adds the current Java indentation after every newline.
			appendable.append("\"\"\"\n").append(content).append("\"\"\"");
		} else {
			super.internalToConvertedExpression(literal, appendable);
		}
	}

	/**
	 * Emits native Java reference syntax without lowering it to a lambda invocation.
	 * Lowering {@code receiver()::method} could defer evaluation or a null check, and
	 * lowering {@code super::method} could change dispatch. Type literal qualifiers are
	 * appended as JvmType values (not as Class<T> expressions) to retain import management;
	 * parameterized/array qualifiers and explicit executable arguments use the type serializer.
	 */
	private void compileReference(final XJMethodReference reference,
			final ITreeAppendable appendable) {
		if (reference.getType() != null) {
			getTypeReferenceSerializer().serialize(reference.getType(), reference, appendable);
		} else if (reference.getReceiver() instanceof XAbstractFeatureCall call
				&& call.isTypeLiteral()) {
			appendable.append((JvmType) call.getFeature());
		} else {
			internalToJavaExpression(reference.getReceiver(), appendable);
		}
		appendable.append("::");
		if (!reference.getTypeArguments().isEmpty()) {
			appendable.append("<");
			for (var i = 0; i < reference.getTypeArguments().size(); i++) {
				if (i != 0) {
					appendable.append(", ");
				}
				getTypeReferenceSerializer().serialize(
					reference.getTypeArguments().get(i), reference, appendable);
			}
			appendable.append(">");
		}
		appendable.append(reference.getMember());
	}

	private void compilePattern(final XJInstanceOfExpression pattern,
			final ITreeAppendable appendable) {
		if (pattern.getPatternVariable() != null || pattern.getRecordPattern() != null) {
			appendable.append("(");
			internalToJavaExpression(pattern.getExpression(), appendable);
			appendable.append(" instanceof ");
			if (pattern.isFinal()) {
				appendable.append(FINAL_MODIFIER);
			}
			getTypeReferenceSerializer().serialize(pattern.getType(), pattern, appendable);
			if (pattern.getRecordPattern() != null) {
				compileRecordPattern(pattern.getRecordPattern(), appendable);
			} else {
				// Java permits the same name in disjoint flow scopes; do not rename it.
				appendable.append(" ").append(pattern.getPatternVariable().getName());
			}
			appendable.append(")");
		} else {
			super._toJavaExpression(pattern, appendable);
		}
	}

	private void compileRecordPattern(final XJRecordPattern pattern, final ITreeAppendable appendable) {
		appendable.append("(");
		var separator = "";
		for (final var component : pattern.getComponents()) {
			appendable.append(separator);
			separator = ", ";
			if (component.isFinal()) {
				appendable.append(FINAL_MODIFIER);
			}
			if (component.isInferred()) {
				appendable.append("var");
			} else {
				getTypeReferenceSerializer().serialize(component.getParameterType(), component, appendable);
			}
			if (component.getRecordPattern() != null) {
				compileRecordPattern(component.getRecordPattern(), appendable);
			} else {
				appendable.append(" ").append(component.getName());
			}
		}
		appendable.append(")");
	}

	private boolean containsPattern(final XExpression expression) {
		return expression != null && (expression instanceof XJInstanceOfExpression pattern
				&& (pattern.getPatternVariable() != null || pattern.getRecordPattern() != null)
			|| EcoreUtil2.getAllContentsOfType(expression, XJInstanceOfExpression.class)
				.stream().anyMatch(pattern -> pattern.getPatternVariable() != null
					|| pattern.getRecordPattern() != null));
	}

	@Override
	protected LightweightTypeReference appendVariableTypeAndName(
			final XVariableDeclaration varDeclaration, final ITreeAppendable appendable) {
		if (varDeclaration.getType() == null) {
			final var type = getLightweightType(varDeclaration.getRight());
			final var tracedAppendable = appendable.trace(varDeclaration);
			final var customDeclaration = (XJVariableDeclaration) varDeclaration;
			if (customDeclaration.isFinal()) {
				tracedAppendable.append(FINAL_MODIFIER);
			}
			tracedAppendable.append("var ");
			final var name = tracedAppendable.declareVariable(varDeclaration,
				makeJavaIdentifier(varDeclaration.getName()));
			tracedAppendable.append(name);
			return type;
		}
		return super.appendVariableTypeAndName(varDeclaration, appendable);
	}

	@Override
	protected void _toJavaStatement(final XIfExpression expression,
			final ITreeAppendable appendable, final boolean isReferenced) {
		if (!(expression instanceof XJConditionalExpression conditional)
				|| !needsNativeConditional(conditional)) {
			super._toJavaStatement(expression, appendable, isReferenced);
		}
	}

	private boolean needsNativeConditional(final XJConditionalExpression expression) {
		final var part = EcoreUtil2.getContainerOfType(expression, XJCasePart.class);
		return containsPattern(expression)
			|| containsNativeCompoundAssignment(expressionRoot(expression))
			|| part != null && EcoreUtil.isAncestor(part.getCase(), expression);
	}

	private boolean isNativeCompoundAssignment(final XExpression expression) {
		return expression instanceof XBinaryOperation operation
			&& operation.isReassignFirstArgument()
			&& (operation.getLeftOperand() instanceof XJArrayAccessExpression
				|| JbaseOperatorMapping.SHIFT_ASSIGNMENTS.contains(
					operation.getConcreteSyntaxFeatureName()));
	}

	private boolean containsNativeCompoundAssignment(final XExpression expression) {
		return isNativeCompoundAssignment(expression)
			|| EcoreUtil2.getAllContentsOfType(expression, XBinaryOperation.class)
				.stream().anyMatch(this::isNativeCompoundAssignment);
	}

	private void compileNativeConditional(final XJConditionalExpression expression,
			final ITreeAppendable appendable) {
		appendable.append("(");
		internalToJavaExpression(expression.getIf(), appendable);
		appendable.append(" ? ");
		internalToJavaExpression(expression.getThen(), appendable);
		appendable.append(" : ");
		internalToJavaExpression(expression.getElse(), appendable);
		appendable.append(")");
	}

	@Override
	protected boolean internalCanCompileToJavaExpression(final XExpression expression,
			final ITreeAppendable appendable) {
		if (isNativeCompoundAssignment(expression)) {
			return true;
		}
		if (expression instanceof XSwitchExpression) {
			return true;
		}
		if (expression instanceof XJConditionalExpression conditional
				&& needsNativeConditional(conditional)) {
			return true;
		}
		return super.internalCanCompileToJavaExpression(expression, appendable);
	}

	private void compileJavaExpression(XJArrayConstructorCall call, ITreeAppendable b) {
		if (call.getArrayLiteral() == null) {
			// otherwise we simply compile the array literal
			// assuming that no dimension expression has been specified
			// (checked by the validator)
			b.append("new ");
			b.append(call.getType());
		}
		compileArrayAccess(call, b);
	}

	private void compileJavaExpression(XJArrayAccessExpression arrayAccess, ITreeAppendable b) {
		internalToConvertedExpression(arrayAccess.getArray(), b);
		compileArrayAccess(arrayAccess, b);
	}

	/**
	 * Always compile into a char literal (we've already type checked that, and we
	 * can assign it also to numeric variables as in Java).
	 * 
	 * @param literal
	 * @param appendable
	 */
	private void compileJavaExpression(XJCharLiteral literal, ITreeAppendable appendable) {
		String javaString = Strings.convertToJavaString(literal.getValue(), true);
		appendable.append("'").append(javaString).append("'");
	}

	@Override
	protected void assignmentToJavaExpression(XAssignment expr, ITreeAppendable b, boolean isExpressionContext) {
		final JvmIdentifiableElement feature = expr.getFeature();
		if (!(feature instanceof JvmOperation)) {
			boolean isArgument = expr.eContainer() instanceof XAbstractFeatureCall;
			if (isArgument) {
				EStructuralFeature containingFeature = expr.eContainingFeature();
				if (containingFeature == XbasePackage.Literals.XFEATURE_CALL__FEATURE_CALL_ARGUMENTS 
						|| containingFeature == XbasePackage.Literals.XMEMBER_FEATURE_CALL__MEMBER_CALL_ARGUMENTS) {
					isArgument = false;
				} else {
					b.append("(");
				}
			}
			if (feature instanceof JvmField) {
				appendReceiver(expr, b, isExpressionContext);
				b.append(".");
				appendFeatureCall(expr, b);
			} else {
				b.append(getAssignmentVariableName(feature, b));
			}
			
			// custom implementation starts here
			compileArrayAccess(expr, b);
			// custom implementation ends here
			
			b.append(" = ");
			internalToJavaExpression(expr.getValue(), b);
			if (isArgument) {
				b.append(")");
			}

			return;
		}

		super.assignmentToJavaExpression(expr, b, isExpressionContext);
	}

	private String getAssignmentVariableName(final JvmIdentifiableElement feature,
			final ITreeAppendable appendable) {
		return (feature.eContainer() instanceof XJInstanceOfExpression
			|| feature.eContainer() instanceof XJCasePart || feature instanceof XJComponentPattern)
			? feature.getSimpleName() : appendable.getName(feature);
	}

	@Override
	protected boolean canCompileToJavaBasicForStatement(final XBasicForLoopExpression expression,
			final ITreeAppendable appendable) {
		// A saved boolean condition would put the pattern outside the loop body scope.
		return containsPattern(expression.getExpression())
			|| super.canCompileToJavaBasicForStatement(expression, appendable);
	}

	/** Preserves the additional declarations parsed as part of the first for initializer. */
	@Override
	protected void toJavaBasicForStatement(XBasicForLoopExpression expr,
			ITreeAppendable b, boolean isReferenced) {
		ITreeAppendable loopAppendable = b.trace(expr);
		loopAppendable.openPseudoScope();
		loopAppendable.newLine().append("for (");

		EList<XExpression> initExpressions = expr.getInitExpressions();
		XExpression firstInitExpression = IterableExtensions.head(initExpressions);
		if (firstInitExpression instanceof XJVariableDeclaration variableDeclaration) {
			LightweightTypeReference type = appendVariableTypeAndName(variableDeclaration, loopAppendable);
			loopAppendable.append(" = ");
			if (variableDeclaration.getRight() != null) {
				compileAsJavaExpression(variableDeclaration.getRight(), loopAppendable, type);
			} else {
				appendDefaultLiteral(loopAppendable, type);
			}
			// custom implementation since possible additional declarations are contained (i.e., parsed)
			// in JavamXVariableDeclaration
			handleForStatementAdditionalVariables(variableDeclaration, type, loopAppendable);
		} else {
			for (int i = 0; i < initExpressions.size(); i++) {
				if (i != 0) {
					loopAppendable.append(", ");
				}
				XExpression initExpression = initExpressions.get(i);
				if (isNativeCompoundAssignment(initExpression)) {
					featureCalltoJavaExpression((XAbstractFeatureCall) initExpression,
						loopAppendable, false);
				} else {
					compileAsJavaExpression(initExpression, loopAppendable,
						getLightweightType(initExpression));
				}
			}
		}

		loopAppendable.append(";");

		XExpression expression = expr.getExpression();
		if (expression != null) {
			loopAppendable.append(" ");
			internalToJavaExpression(expression, loopAppendable);
		}
		loopAppendable.append(";");

		compileForUpdateExpressions(expr.getUpdateExpressions(), loopAppendable);
		loopAppendable.append(") {").increaseIndentation();

		XExpression eachExpression = expr.getEachExpression();
		internalToJavaStatement(eachExpression, loopAppendable, false);

		loopAppendable.decreaseIndentation().newLine().append("}");
		loopAppendable.closeScope();
	}

	private void compileForUpdateExpressions(final EList<XExpression> updateExpressions,
			final ITreeAppendable loopAppendable) {
		for (int i = 0; i < updateExpressions.size(); i++) {
			if (i != 0) {
				loopAppendable.append(",");
			}
			loopAppendable.append(" ");
			XExpression updateExpression = updateExpressions.get(i);
			if (updateExpression instanceof XAbstractFeatureCall call
					&& (getLightweightType(call).isPrimitiveVoid()
						|| isNativeCompoundAssignment(call))) {
				featureCalltoJavaExpression(call, loopAppendable, false);
			} else {
				internalToJavaExpression(updateExpression, loopAppendable);
			}
		}
	}

	private void handleForStatementAdditionalVariables(XJVariableDeclaration variableDeclaration,
			LightweightTypeReference type, ITreeAppendable loopAppendable) {
		// custom implementation since possible additional declarations are contained (i.e., parsed)
		// in JavamXVariableDeclaration
		EList<XVariableDeclaration> additionalVariables = variableDeclaration.getAdditionalVariables();
		for (int i = 0; i < additionalVariables.size(); i++) {
			loopAppendable.append(", ");
			XVariableDeclaration initExpression = additionalVariables.get(i);
			loopAppendable.append(loopAppendable.declareVariable(initExpression, makeJavaIdentifier(initExpression.getName())));
			loopAppendable.append(" = ");
			if (initExpression.getRight() != null) {
				compileAsJavaExpression(initExpression.getRight(), loopAppendable, type);
			} else {
				appendDefaultLiteral(loopAppendable, type);
			}
		}
	}

	/**
	 * Overridden to deal with branching instructions.
	 * 
	 * @see XbaseCompiler#toJavaWhileStatement(XBasicForLoopExpression, ITreeAppendable, boolean)
	 */
	@Override
	protected void toJavaWhileStatement(XBasicForLoopExpression expr,
			ITreeAppendable b, boolean isReferenced) {
		ITreeAppendable loopAppendable = b.trace(expr);
		
		boolean needBraces = !bracesAreAddedByOuterStructure(expr);
		if (needBraces) {
			loopAppendable.newLine().increaseIndentation().append("{");
			loopAppendable.openPseudoScope();
		}
		
		EList<XExpression> initExpressions = expr.getInitExpressions();
		for (int i = 0; i < initExpressions.size(); i++) {
			XExpression initExpression = initExpressions.get(i);
			
			// custom implementation
			// since a for statement cannot be used as expression we don't need
			// any special treatment for the last expression
			
			internalToJavaStatement(initExpression, loopAppendable, false);
			
		}

		final String varName = loopAppendable.declareSyntheticVariable(expr, "_while");
		
		XExpression expression = expr.getExpression();
		if (expression != null) {
			internalToJavaStatement(expression, loopAppendable, true);
			loopAppendable.newLine().append("boolean ").append(varName).append(" = ");
			internalToJavaExpression(expression, loopAppendable);
			loopAppendable.append(";");
		} else {
			loopAppendable.newLine().append("boolean ").append(varName).append(ASSIGNED_TRUE);
		}
		loopAppendable.newLine();
		loopAppendable.append("while (");
		loopAppendable.append(varName);
		loopAppendable.append(") {").increaseIndentation();
		loopAppendable.openPseudoScope();
		
		XExpression eachExpression = expr.getEachExpression();
		internalToJavaStatement(eachExpression, loopAppendable, false);
		
		// custom implementation:
		// if the each expression contains sure branching statements then
		// we must not generate the update expression and the check expression
		// not that we don't check if eachExpression is early exit: if it was,
		// then we'd get a validation error for unreachable statement,
		// see jbase.tests.JbaseValidatorTest.testDeadCodeInForLoopTranslatedToJavaWhileEarlyExit
		if (!branchingStatementDetector.isSureBranchStatement(eachExpression)) {
			EList<XExpression> updateExpressions = expr.getUpdateExpressions();
			
			for (XExpression updateExpression : updateExpressions) {
				internalToJavaStatement(updateExpression, loopAppendable, false);
			}

			if (expression != null) {
				internalToJavaStatement(expression, loopAppendable, true);
				loopAppendable.newLine().append(varName).append(" = ");
				internalToJavaExpression(expression, loopAppendable);
				loopAppendable.append(";");
			} else {
				loopAppendable.newLine().append(varName).append(ASSIGNED_TRUE);
			}
		}
		
		loopAppendable.closeScope();
		loopAppendable.decreaseIndentation().newLine().append("}");
		
		if (needBraces) {
			loopAppendable.closeScope();
			loopAppendable.decreaseIndentation().newLine().append("}");
		}
	}

	/**
	 * In our Java-like switch statement we can always compile into Java switch.
	 * 
	 * @see XbaseCompiler#_toJavaStatement(XSwitchExpression, ITreeAppendable, boolean)
	 */
	@Override
	protected void _toJavaStatement(XSwitchExpression expr, ITreeAppendable b, boolean isReferenced) {
		if (JbaseSwitches.isExpression(expr)) {
			internalToJavaStatement(expr.getSwitch(), b, true);
		} else if (JbaseSwitches.isEnhanced(expr)) {
			internalToJavaStatement(expr.getSwitch(), b, true);
			b.newLine();
			compileEnhancedSwitch(expr, b);
		} else {
			_toJavaSwitchStatement(expr, b, isReferenced);
		}
	}

	/** Preserve Java rules, patterns and yield instead of lowering to if/else chains. */
	private void compileEnhancedSwitch(final XSwitchExpression expression,
			final ITreeAppendable appendable) {
		appendable.append("switch (");
		internalToJavaExpression(expression.getSwitch(), appendable);
		appendable.append(") {").increaseIndentation();
		final var defaultBody = expression.getDefault();
		var defaultPending = defaultBody != null;
		final var defaultNode = NodeModelUtils.getNode(defaultBody);
		for (final var original : expression.getCases()) {
			final var part = (XJCasePart) original;
			// Default may occur between cases; the inherited Xbase model stores it separately.
			final var node = NodeModelUtils.getNode(part);
			if (defaultPending && node.getOffset() > defaultNode.getOffset()) {
				compileSwitchDefault((XJSwitchExpression) expression, appendable);
				defaultPending = false;
			}
			appendable.openPseudoScope();
			appendable.newLine().append("case ");
			compileCaseLabels(part, appendable);
			compileSwitchBody(expression, part.getThen(), part.isArrow(), appendable);
			appendable.closeScope();
		}
		if (defaultPending) {
			compileSwitchDefault((XJSwitchExpression) expression, appendable);
		}
		appendable.decreaseIndentation().newLine().append("}");
	}

	private void compileCaseLabels(final XJCasePart part, final ITreeAppendable appendable) {
		if (part.getTypeGuard() != null) {
			if (part.isFinal()) {
				appendable.append(FINAL_MODIFIER);
			}
			getTypeReferenceSerializer().serialize(part.getTypeGuard(), part, appendable);
			if (part.getRecordPattern() != null) {
				compileRecordPattern(part.getRecordPattern(), appendable);
			} else {
				appendable.append(" ").append(part.getPatternVariable().getName());
			}
			if (part.getCase() != null) {
				appendable.append(" when ");
				internalToJavaExpression(part.getCase(), appendable);
			}
		} else {
			internalToJavaExpression(part.getCase(), appendable);
			for (final var label : part.getAdditionalLabels()) {
				appendable.append(", ");
				internalToJavaExpression(label, appendable);
			}
		}
	}

	private void compileSwitchDefault(final XJSwitchExpression expression,
			final ITreeAppendable appendable) {
		appendable.openPseudoScope();
		appendable.newLine().append(expression.isNullDefault() ? "case null, default" : "default");
		compileSwitchBody(expression, expression.getDefault(),
			expression.isDefaultArrow(), appendable);
		appendable.closeScope();
	}

	private void compileSwitchBody(final XSwitchExpression expression, final XExpression body,
			final boolean arrow, final ITreeAppendable appendable) {
		appendable.append(arrow ? " -> {" : ":").increaseIndentation();
		if (JbaseSwitches.isExpression(expression) && arrow
				&& body instanceof XJSemicolonStatement statement
				&& !(statement.getExpression() instanceof XThrowExpression)) {
			if (!canCompileToJavaExpression(statement.getExpression(), appendable)) {
				internalToJavaStatement(statement.getExpression(), appendable, true);
			}
			appendable.newLine().append("yield ");
			internalToJavaExpression(statement.getExpression(), appendable);
			appendable.append(";");
		} else {
			// Blocks are already delimited by the rule. Compile their contents in this scope.
			if (body instanceof XBlockExpression block) {
				for (final var child : block.getExpressions()) {
					internalToJavaStatement(child, appendable, false);
				}
			} else {
				internalToJavaStatement(body, appendable, false);
			}
		}
		appendable.decreaseIndentation();
		if (arrow) {
			appendable.newLine().append("}");
		}
	}

	/**
	 * Since we want Java switch statement, the compilation is simpler and does
	 * not append break automatically.
	 * 
	 * @see XbaseCompiler#_toJavaSwitchStatement(XSwitchExpression, ITreeAppendable, boolean)
	 */
	@Override
	protected void _toJavaSwitchStatement(XSwitchExpression expr, ITreeAppendable b, boolean isReferenced) {
		final String switchResultName = declareSwitchResultVariable(expr, b, isReferenced);
		internalToJavaStatement(expr.getSwitch(), b, true);
		final String variableName = declareLocalVariable(expr, b);
		
		b.newLine().append("switch (").append(variableName).append(") {").increaseIndentation();
		for (XCasePart casePart : expr.getCases()) {
			ITreeAppendable caseAppendable = b.trace(casePart, true);
			caseAppendable.newLine().increaseIndentation().append("case ");
			
			ITreeAppendable conditionAppendable = caseAppendable.trace(casePart.getCase(), true);
			internalToJavaExpression(casePart.getCase(), conditionAppendable);
			
			caseAppendable.append(":");
			XExpression then = casePart.getThen();
			executeThenPart(expr, switchResultName, then, caseAppendable, isReferenced);
			caseAppendable.decreaseIndentation();
		}
		if (expr.getDefault() != null) {
			ILocationData location = getLocationOfDefault(expr);
			ITreeAppendable defaultAppendable = b.trace(location);
			
			defaultAppendable.newLine().increaseIndentation().append("default:");

			defaultAppendable.openPseudoScope();
			executeThenPart(expr, switchResultName, expr.getDefault(), defaultAppendable, isReferenced);
			defaultAppendable.closeScope();

			defaultAppendable.decreaseIndentation();
		}
		b.decreaseIndentation().newLine().append("}");
	}

	/**
	 * Preserves blank locals used under closure nodes before delegating normal generation.
	 * For {@code int n; n = 2; IntSupplier f = () -> n;}, a synthesized {@code n = 0}
	 * initializer would make the real assignment a reassignment and invalidate the capture
	 * in generated Java. The validator independently checks initialization and capture legality.
	 */
	@Override
	protected void _toJavaStatement(XVariableDeclaration varDeclaration,
			ITreeAppendable b, boolean isReferenced) {
		if (varDeclaration.getRight() == null && isCapturedLocal(varDeclaration)) {
			// A synthetic initializer would make the subsequent Java assignment
			// a reassignment, preventing capture of an effectively final local.
			b.newLine();
			appendVariableTypeAndName(varDeclaration, b);
			b.append(";");
		} else {
			super._toJavaStatement(varDeclaration, b, isReferenced);
		}
		
		if (varDeclaration instanceof XJVariableDeclaration customVar) {
			for (XVariableDeclaration additional : customVar.getAdditionalVariables()) {
				_toJavaStatement(additional, b, isReferenced);
			}
		}
	}

	/**
	 * Conservatively detects an external local used below any XClosure node, including the
	 * reference subtype. This syntactic query controls preservation of a blank declaration;
	 * JbaseValidator decides whether a use is an actual capture and is effectively final.
	 */
	private boolean isCapturedLocal(final XVariableDeclaration variable) {
		return XbaseUsageCrossReferencer.find(variable, EcoreUtil.getRootContainer(variable))
			.stream().anyMatch(reference -> {
				var closure = EcoreUtil2.getContainerOfType(reference.getEObject(), XClosure.class);
				return closure != null && !EcoreUtil.isAncestor(closure, variable);
			});
	}

	/**
	 * Specialized for prefix operator and unary expression
	 * 
	 * @see FeatureCallCompiler#featureCalltoJavaExpression(XAbstractFeatureCall, ITreeAppendable, boolean)
	 */
	@Override
	protected void featureCalltoJavaExpression(XAbstractFeatureCall call,
			ITreeAppendable b, boolean isExpressionContext) {
		if (call instanceof XBinaryOperation operation && isNativeCompoundAssignment(operation)) {
			compileNativeCompoundAssignment(operation, b, isExpressionContext);
			return;
		} else if (call instanceof XJPrefixOperation prefixOperation) {
			// we can't simply retrieve the inline annotations as it is done
			// for postfix operations, since postfix operations are already mapped to
			// postfix methods operator_plusPlus and operator_minusMinus
			JvmIdentifiableElement feature = call.getFeature();
			if (feature.getSimpleName().endsWith("plusPlus")) {
				b.append("++");
			} else {
				// the only other possibility is minus minus
				b.append("--");
			}
			appendArgument(prefixOperation.getOperand(), b);
			return;
		} else if (call instanceof XUnaryOperation unaryOperation) {
			final StringBuilder builder = new StringBuilder();
			boolean specialHandling = expressionHelper.specialHandling(unaryOperation,
				new BaseCase() {
					@Override
					public Boolean apply(XUnaryOperation op, XNumberLiteral lit) {
						builder.append(op.getConcreteSyntaxFeatureName() + lit.getValue());
						return true;
					}
				},
				new StepCase() {
					@Override
					public void accept(XUnaryOperation op) {
						builder.insert(0, op.getConcreteSyntaxFeatureName() + "(").append(')');
					}
				}
			);
			if (specialHandling) {
				b.append(builder);
				return;
			}
		}

		super.featureCalltoJavaExpression(call, b, isExpressionContext);
	}

	private void compileNativeCompoundAssignment(final XBinaryOperation operation,
			final ITreeAppendable b, final boolean isExpressionContext) {
		// Native syntax evaluates receiver/index once and preserves implicit narrowing.
		if (isExpressionContext) {
			b.append("(");
		}
		if (operation.getLeftOperand() instanceof XJArrayAccessExpression access) {
			compileJavaExpression(access, b);
		} else {
			// Emit the lvalue directly, without the operator's unboxing conversion.
			featureCalltoJavaExpression((XAbstractFeatureCall) operation.getLeftOperand(), b, true);
		}
		b.append(" ").append(operation.getConcreteSyntaxFeatureName()).append(" ");
		internalToJavaExpression(operation.getRightOperand(), b);
		if (isExpressionContext) {
			b.append(")");
		}
	}

	/**
	 * Customized for our special treatment of some unary operations
	 * 
	 * @param expr
	 * @param b
	 * @param recursive
	 * @return
	 */
	@Override
	protected boolean isVariableDeclarationRequired(XExpression expr, ITreeAppendable b, boolean recursive) {
		// Hoisting a negated match (or an operand using its binding) into a
		// boolean temporary destroys Java's flow scope. Keep its expression intact.
		// The same applies to evaluation order around native array assignments.
		final var root = expressionRoot(expr);
		if (containsPattern(root) || containsNativeCompoundAssignment(root)) {
			return false;
		}
		if (expr instanceof XUnaryOperation unaryOperation) {
			return !expressionHelper.specialHandling(unaryOperation);
		} else if (expr instanceof XJClassObject ||
			EcoreUtil2.getContainerOfType(expr, XJTryWithResourcesVariableDeclaration.class) != null) {
			// there must be no intermediate expressions in the context of the
			// try-with-resources' resource declaration
			return false;
		}
		return super.isVariableDeclarationRequired(expr, b, recursive);
	}

	@Override
	protected void _toJavaStatement(final XTryCatchFinallyExpression expression,
			final ITreeAppendable appendable, final boolean isReferenced) {
		if (!(expression instanceof XJTryWithResourcesStatement statement)
				|| statement.getResources().stream()
					.noneMatch(XJTryWithResourcesExistingResource.class::isInstance)) {
			super._toJavaStatement(expression, appendable, isReferenced);
			return;
		}
		final var resources = statement.getResources();

		appendable.newLine().trace(expression, false).append("try (");
		for (var i = 0; i < resources.size(); i++) {
			final var resource = resources.get(i);
			if (resource instanceof XJTryWithResourcesExistingResource existingResource) {
				internalToJavaExpression(existingResource.getRight(), appendable);
			} else {
				resource.setWriteable(false);
				final var type = appendVariableTypeAndName(resource, appendable);
				appendable.append(" = ");
				compileAsJavaExpression(resource.getRight(), appendable, type);
			}
			if (i < resources.size() - 1) {
				appendable.append("; ");
			}
		}
		appendable.append(") {").increaseIndentation();
		appendable.declareSyntheticVariable(Tuples.pair(expression, "_caughtThrowables"), "_ts");
		internalToJavaStatement(expression.getExpression(), appendable, false);
		appendable.decreaseIndentation().newLine().append("}");
		appendCatchAndFinally(expression, appendable, isReferenced);
	}

	private XExpression expressionRoot(final XExpression expression) {
		var root = expression;
		while (root.eContainer() instanceof XExpression parent
				&& !(parent instanceof XBlockExpression)
				&& !(parent instanceof XJSemicolonStatement)
				&& (!(parent instanceof XIfExpression)
					|| parent instanceof XJConditionalExpression)
				&& !(parent instanceof XClosure)) {
			root = parent;
		}
		return root;
	}

	private void compileArrayAccess(XExpression expr, ITreeAppendable b) {
		if (expr instanceof XJArrayAccess access) {
			for (XExpression index : access.getIndexes()) {
				b.append("[");
				internalToJavaExpression(index, b);
				b.append("]");
			}
		}
	}

	/**
	 * Specialization for {@link XJArrayConstructorCall} since it can
	 * have dimensions without dimension expression (index).
	 * 
	 * @param cons
	 * @param b
	 */
	private void compileArrayAccess(XJArrayConstructorCall cons, ITreeAppendable b) {
		XJArrayLiteral arrayLiteral = cons.getArrayLiteral();

		if (arrayLiteral != null) {
			internalToJavaExpression(arrayLiteral, b);
		} else {
			Iterable<XExpression> dimensionsAndIndexes = modelUtil.arrayDimensionIndexAssociations(cons);
			
			for (XExpression e : dimensionsAndIndexes) {
				b.append("[");
				if (e != null) {
					internalToJavaExpression(e, b);
				}
				b.append("]");
			}
		}
	}

	/**
	 * Overridden because our parameters, like in Java, can be final or non final by default 
	 */
	@Override
	protected void appendForLoopParameter(XForLoopExpression expr, ITreeAppendable appendable) {
		JvmFormalParameter declaredParam = expr.getDeclaredParam();
		if (((XJJvmFormalParameter) declaredParam).isFinal()) {
			appendable.append(FINAL_MODIFIER);
		}
		// this is the original code
		JvmTypeReference paramType = getForLoopParameterType(expr);
		final var originalParam = (XJJvmFormalParameter) declaredParam;
		if (originalParam.getParameterType() == null) {
			appendable.append("var");
		} else {
			serialize(paramType, expr, appendable);
		}
		appendable.append(" ");
		final String name = makeJavaIdentifier(declaredParam.getName());
		String varName = appendable.declareVariable(declaredParam, name);
		appendable.trace(declaredParam, TypesPackage.Literals.JVM_FORMAL_PARAMETER__NAME, 0).append(varName);
	}

}
