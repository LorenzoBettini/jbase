/**
 * 
 */
package jbase.compiler;

import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.xtext.EcoreUtil2;
import org.eclipse.xtext.common.types.JvmField;
import org.eclipse.xtext.common.types.JvmFormalParameter;
import org.eclipse.xtext.common.types.JvmIdentifiableElement;
import org.eclipse.xtext.common.types.JvmOperation;
import org.eclipse.xtext.common.types.JvmType;
import org.eclipse.xtext.common.types.JvmTypeReference;
import org.eclipse.xtext.common.types.TypesPackage;
import org.eclipse.xtext.generator.trace.ILocationData;
import org.eclipse.xtext.util.Strings;
import org.eclipse.xtext.xbase.XAbstractFeatureCall;
import org.eclipse.xtext.xbase.XAssignment;
import org.eclipse.xtext.xbase.XBasicForLoopExpression;
import org.eclipse.xtext.xbase.XCasePart;
import org.eclipse.xtext.xbase.XExpression;
import org.eclipse.xtext.xbase.XForLoopExpression;
import org.eclipse.xtext.xbase.XNumberLiteral;
import org.eclipse.xtext.xbase.XSwitchExpression;
import org.eclipse.xtext.xbase.XUnaryOperation;
import org.eclipse.xtext.xbase.XVariableDeclaration;
import org.eclipse.xtext.xbase.XbasePackage;
import org.eclipse.xtext.xbase.compiler.output.ITreeAppendable;
import org.eclipse.xtext.xbase.lib.IterableExtensions;
import org.eclipse.xtext.xbase.typesystem.references.LightweightTypeReference;

import com.google.common.base.Joiner;
import com.google.inject.Inject;

import jbase.controlflow.JbaseBranchingStatementDetector;
import jbase.jbase.XJArrayAccess;
import jbase.jbase.XJArrayAccessExpression;
import jbase.jbase.XJArrayConstructorCall;
import jbase.jbase.XJArrayLiteral;
import jbase.jbase.XJBranchingStatement;
import jbase.jbase.XJBreakStatement;
import jbase.jbase.XJCharLiteral;
import jbase.jbase.XJClassObject;
import jbase.jbase.XJContinueStatement;
import jbase.jbase.XJJvmFormalParameter;
import jbase.jbase.XJPrefixOperation;
import jbase.jbase.XJSemicolonStatement;
import jbase.jbase.XJTryWithResourcesStatement;
import jbase.jbase.XJTryWithResourcesVariableDeclaration;
import jbase.jbase.XJVariableDeclaration;
import jbase.util.JbaseExpressionHelper;
import jbase.util.JbaseExpressionHelper.BaseCase;
import jbase.util.JbaseExpressionHelper.StepCase;
import jbase.util.JbaseModelUtil;

/**
 * @author Lorenzo Bettini
 *
 */
public class JbaseXbaseCompiler extends PatchedXbaseCompiler {
	
	private static final String ASSIGNED_TRUE = " = true;";

	@Inject
	private JbaseModelUtil modelUtil;

	@Inject
	private JbaseBranchingStatementDetector branchingStatementDetector;

	@Inject
	private JbaseExpressionHelper expressionHelper;

	@Override
	protected void doInternalToJavaStatement(XExpression obj,
			ITreeAppendable appendable, boolean isReferenced) {
		if (obj instanceof XJArrayConstructorCall) {
			_toJavaStatement((XJArrayConstructorCall) obj, appendable, isReferenced);
		} else if (obj instanceof XJArrayAccessExpression) {
			_toJavaStatement((XJArrayAccessExpression) obj, appendable, isReferenced);
		} else if (obj instanceof XJContinueStatement) {
			_toJavaStatement((XJContinueStatement) obj, appendable, isReferenced);
		} else if (obj instanceof XJBreakStatement) {
			_toJavaStatement((XJBreakStatement) obj, appendable, isReferenced);
		} else if (obj instanceof XJClassObject) {
			_toJavaStatement((XJClassObject) obj, appendable, isReferenced);
		} else if (obj instanceof XJTryWithResourcesStatement) {
			_toJavaStatement((XJTryWithResourcesStatement) obj, appendable, isReferenced);
		} else if (obj instanceof XJSemicolonStatement) {
			_toJavaStatement((XJSemicolonStatement) obj, appendable, isReferenced);
		} else {
			super.doInternalToJavaStatement(obj, appendable, isReferenced);
		}
	}
	
	public void _toJavaStatement(XJArrayConstructorCall call, ITreeAppendable b,
			boolean isReferenced) {
		// compile it only as expression
	}

	public void _toJavaStatement(XJArrayAccessExpression access, ITreeAppendable b,
			boolean isReferenced) {
		// compile it only as expression
	}

	public void _toJavaStatement(XJContinueStatement st, ITreeAppendable b,
			boolean isReferenced) {
		XBasicForLoopExpression basicForLoop = EcoreUtil2.getContainerOfType(st, XBasicForLoopExpression.class);
		
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
		compileBranchingStatement(st, b, "continue");
	}

	public void _toJavaStatement(XJBreakStatement st, ITreeAppendable b,
			boolean isReferenced) {
		compileBranchingStatement(st, b, "break");
	}

	public void _toJavaStatement(XJClassObject e, ITreeAppendable b,
			boolean isReferenced) {
		// compile it only as expression
	}

	private void compileClassObject(XJClassObject e, ITreeAppendable b) {
		XAbstractFeatureCall featureCall = (XAbstractFeatureCall) e.getTypeExpression();
		// we must append it as a JvmType so that the corresponding import statement
		// is generated in the Java code
		b.append((JvmType) featureCall.getFeature()).
			append(Joiner.on("").join(e.getArrayDimensions())).append(".class");
	}

	private void compileBranchingStatement(XJBranchingStatement st,
			ITreeAppendable b, String instruction) {
		b.newLine().append(instruction).append(";");
	}

	public void _toJavaStatement(XJSemicolonStatement st, ITreeAppendable b,
			boolean isReferenced) {
		XExpression expression = st.getExpression();
		if (expression != null) {
			doInternalToJavaStatement(expression, b, isReferenced);
		} else {
			b.append(";");
		}
	}

	public void _toJavaStatement(XJTryWithResourcesStatement expr, ITreeAppendable outerAppendable, boolean isReferenced) {
		ITreeAppendable b = outerAppendable.trace(expr, false);
		b.newLine().append("try (").increaseIndentation();
		for (XJTryWithResourcesVariableDeclaration r : expr.getResourceDeclarations()) {
			internalToJavaStatement(r, b.trace(r, true), false);
		}
		b.decreaseIndentation();
		b.newLine().append(") {").increaseIndentation();
		internalToJavaStatement(expr.getExpression(), b, false);
		b.decreaseIndentation().newLine().append("}");
		appendCatchAndFinally(expr, b, isReferenced);
	}

	@Override
	protected void internalToConvertedExpression(XExpression obj, ITreeAppendable appendable) {
		if (obj instanceof XJArrayConstructorCall) {
			_toJavaExpression((XJArrayConstructorCall) obj, appendable);
		} else if (obj instanceof XJArrayAccessExpression) {
			_toJavaExpression((XJArrayAccessExpression) obj, appendable);
		} else if (obj instanceof XJCharLiteral) {
			_toJavaExpression((XJCharLiteral) obj, appendable);
		} else if (obj instanceof XJClassObject) {
			_toJavaExpression((XJClassObject) obj, appendable);
		} else {
			super.internalToConvertedExpression(obj, appendable);
		}
	}

	public void _toJavaExpression(XJArrayConstructorCall call, ITreeAppendable b) {
		if (call.getArrayLiteral() == null) {
			// otherwise we simply compile the array literal
			// assuming that no dimension expression has been specified
			// (checked by the validator)
			b.append("new ");
			b.append(call.getType());
		}
		compileArrayAccess(call, b);
	}

	public void _toJavaExpression(XJArrayAccessExpression arrayAccess, ITreeAppendable b) {
		internalToConvertedExpression(arrayAccess.getArray(), b);
		compileArrayAccess(arrayAccess, b);
	}

	public void _toJavaExpression(XJClassObject e, ITreeAppendable b) {
		compileClassObject(e, b);
	}

	/**
	 * Always compile into a char literal (we've already type checked that, and we
	 * can assign it also to numeric variables as in Java).
	 * 
	 * @param literal
	 * @param appendable
	 */
	public void _toJavaExpression(XJCharLiteral literal, ITreeAppendable appendable) {
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
				String name = b.getName(expr.getFeature());
				b.append(name);
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

	/**
	 * Overridden to deal with several variable declarations.
	 * 
	 * @see org.eclipse.xtext.xbase.compiler.XbaseCompiler#toJavaBasicForStatement(org.eclipse.xtext.xbase.XBasicForLoopExpression, org.eclipse.xtext.xbase.compiler.output.ITreeAppendable, boolean)
	 */
	@Override
	protected void toJavaBasicForStatement(XBasicForLoopExpression expr,
			ITreeAppendable b, boolean isReferenced) {
		ITreeAppendable loopAppendable = b.trace(expr);
		loopAppendable.openPseudoScope();
		loopAppendable.newLine().append("for (");
		
		EList<XExpression> initExpressions = expr.getInitExpressions();
		XExpression firstInitExpression = IterableExtensions.head(initExpressions);
		if (firstInitExpression instanceof XJVariableDeclaration) {
			XJVariableDeclaration variableDeclaration = (XJVariableDeclaration) firstInitExpression;
			LightweightTypeReference type = appendVariableTypeAndName(variableDeclaration, loopAppendable);
			loopAppendable.append(" = ");
			if (variableDeclaration.getRight() != null) {
				compileAsJavaExpression(variableDeclaration.getRight(), loopAppendable, type);
			} else {
				appendDefaultLiteral(loopAppendable, type);
			}
			
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
		} else {
			for (int i = 0; i < initExpressions.size(); i++) {
				if (i != 0) {
					loopAppendable.append(", ");
				}
				XExpression initExpression = initExpressions.get(i);
				compileAsJavaExpression(initExpression, loopAppendable, getLightweightType(initExpression));
			}
		}
		
		loopAppendable.append(";");
		
		XExpression expression = expr.getExpression();
		if (expression != null) {
			loopAppendable.append(" ");
			internalToJavaExpression(expression, loopAppendable);
		}
		loopAppendable.append(";");
		
		EList<XExpression> updateExpressions = expr.getUpdateExpressions();
		for (int i = 0; i < updateExpressions.size(); i++) {
			if (i != 0) {
				loopAppendable.append(",");
			}
			loopAppendable.append(" ");
			XExpression updateExpression = updateExpressions.get(i);
			internalToJavaExpression(updateExpression, loopAppendable);
		}
		loopAppendable.append(") {").increaseIndentation();
		
		XExpression eachExpression = expr.getEachExpression();
		internalToJavaStatement(eachExpression, loopAppendable, false);
		
		loopAppendable.decreaseIndentation().newLine().append("}");
		loopAppendable.closeScope();
	}

	/**
	 * Overridden to deal with branching instructions.
	 * 
	 * @see org.eclipse.xtext.xbase.compiler.XbaseCompiler#toJavaWhileStatement(org.eclipse.xtext.xbase.XBasicForLoopExpression, org.eclipse.xtext.xbase.compiler.output.ITreeAppendable, boolean)
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
	 * @see org.eclipse.xtext.xbase.compiler.XbaseCompiler#_toJavaStatement(org.eclipse.xtext.xbase.XSwitchExpression, org.eclipse.xtext.xbase.compiler.output.ITreeAppendable, boolean)
	 */
	@Override
	protected void _toJavaStatement(XSwitchExpression expr, ITreeAppendable b, boolean isReferenced) {
		_toJavaSwitchStatement(expr, b, isReferenced);
	}

	/**
	 * Since we want Java switch statement, the compilation is simpler and does
	 * not append break automatically.
	 * 
	 * @see org.eclipse.xtext.xbase.compiler.XbaseCompiler#_toJavaSwitchStatement(org.eclipse.xtext.xbase.XSwitchExpression, org.eclipse.xtext.xbase.compiler.output.ITreeAppendable, boolean)
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

	@Override
	protected void _toJavaStatement(XVariableDeclaration varDeclaration,
			ITreeAppendable b, boolean isReferenced) {
		super._toJavaStatement(varDeclaration, b, isReferenced);
		
		if (varDeclaration instanceof XJVariableDeclaration) {
			XJVariableDeclaration customVar = (XJVariableDeclaration) varDeclaration;
			for (XVariableDeclaration additional : customVar.getAdditionalVariables()) {
				_toJavaStatement(additional, b, isReferenced);
			}
		}
	}

	/**
	 * Specialized for prefix operator and unary expression
	 * 
	 * @see org.eclipse.xtext.xbase.compiler.FeatureCallCompiler#featureCalltoJavaExpression(org.eclipse.xtext.xbase.XAbstractFeatureCall, org.eclipse.xtext.xbase.compiler.output.ITreeAppendable, boolean)
	 */
	@Override
	protected void featureCalltoJavaExpression(XAbstractFeatureCall call,
			ITreeAppendable b, boolean isExpressionContext) {
		if (call instanceof XJPrefixOperation) {
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
			appendArgument(((XJPrefixOperation) call).getOperand(), b);
			return;
		} else if (call instanceof XUnaryOperation) {
			XUnaryOperation unaryOperation = (XUnaryOperation) call;
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
		if (expr instanceof XUnaryOperation) {
			return !expressionHelper.specialHandling((XUnaryOperation) expr);
		} else if (expr instanceof XJClassObject ||
			EcoreUtil2.getContainerOfType(expr, XJTryWithResourcesVariableDeclaration.class) != null) {
			// there must be no intermediate expressions in the context of the
			// try-with-resources' resource declaration
			return false;
		}
		return super.isVariableDeclarationRequired(expr, b, recursive);
	}

	private void compileArrayAccess(XExpression expr, ITreeAppendable b) {
		if (expr instanceof XJArrayAccess) {
			XJArrayAccess access = (XJArrayAccess) expr;
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
			appendable.append("final ");
		}
		// this is the original code
		JvmTypeReference paramType = getForLoopParameterType(expr);
		serialize(paramType, expr, appendable);
		appendable.append(" ");
		final String name = makeJavaIdentifier(declaredParam.getName());
		String varName = appendable.declareVariable(declaredParam, name);
		appendable.trace(declaredParam, TypesPackage.Literals.JVM_FORMAL_PARAMETER__NAME, 0).append(varName);
	}


}
