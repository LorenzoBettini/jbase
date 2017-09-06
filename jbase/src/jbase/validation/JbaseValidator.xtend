/*
 * generated by Xtext
 */
package jbase.validation

import com.google.inject.Inject
import java.util.List
import jbase.controlflow.JbaseSureReturnComputer
import jbase.jbase.JbasePackage
import jbase.jbase.XJAdditionalXVariableDeclaration
import jbase.jbase.XJArrayAccess
import jbase.jbase.XJArrayConstructorCall
import jbase.jbase.XJBranchingStatement
import jbase.jbase.XJBreakStatement
import jbase.jbase.XJCharLiteral
import jbase.jbase.XJConstructorCall
import jbase.jbase.XJContinueStatement
import jbase.jbase.XJJvmFormalParameter
import jbase.jbase.XJSemicolonStatement
import jbase.jbase.XJTryWithResourcesStatement
import jbase.jbase.XJTryWithResourcesVariableDeclaration
import jbase.scoping.featurecalls.JbaseOperatorMapping
import jbase.util.JbaseModelUtil
import jbase.util.JbaseNodeModelUtil
import org.eclipse.emf.ecore.EObject
import org.eclipse.emf.ecore.EStructuralFeature
import org.eclipse.xtext.common.types.JvmFormalParameter
import org.eclipse.xtext.common.types.JvmOperation
import org.eclipse.xtext.common.types.JvmType
import org.eclipse.xtext.util.Wrapper
import org.eclipse.xtext.validation.Check
import org.eclipse.xtext.xbase.XAbstractFeatureCall
import org.eclipse.xtext.xbase.XAbstractWhileExpression
import org.eclipse.xtext.xbase.XBasicForLoopExpression
import org.eclipse.xtext.xbase.XBlockExpression
import org.eclipse.xtext.xbase.XConstructorCall
import org.eclipse.xtext.xbase.XExpression
import org.eclipse.xtext.xbase.XFeatureCall
import org.eclipse.xtext.xbase.XMemberFeatureCall
import org.eclipse.xtext.xbase.XSwitchExpression
import org.eclipse.xtext.xbase.XVariableDeclaration
import org.eclipse.xtext.xbase.XbasePackage
import org.eclipse.xtext.xbase.jvmmodel.ILogicalContainerProvider
import org.eclipse.xtext.xbase.typesystem.IBatchTypeResolver
import org.eclipse.xtext.xbase.typesystem.references.StandardTypeReferenceOwner
import org.eclipse.xtext.xbase.validation.IssueCodes
import org.eclipse.xtext.xbase.validation.ProxyAwareUIStrings
import org.eclipse.xtext.xtype.XImportDeclaration

import static jbase.validation.JbaseIssueCodes.*

/**
 * @author Lorenzo Bettini
 */
class JbaseValidator extends AbstractJbaseValidator {

	static val xbasePackage = XbasePackage.eINSTANCE;
	static val jbasePackage = JbasePackage.eINSTANCE;

	@Inject extension JbaseNodeModelUtil
	@Inject extension JbaseModelUtil
	@Inject ILogicalContainerProvider logicalContainerProvider
	@Inject IBatchTypeResolver batchTypeResolver
	@Inject JbaseSureReturnComputer sureReturnComputer
	@Inject JbaseInitializedVariableFinder initializedVariableFinder
	@Inject ProxyAwareUIStrings proxyAwareUIStrings

	override protected checkAssignment(XExpression expression, EStructuralFeature feature, boolean simpleAssignment) {
		if (expression instanceof XJArrayAccess) {
			// it means that we're accessing an array element, thus the
			// typical checks on an assignment (i.e., whether the variable is final,
			// or whether it's an abstract feature call) should not be performed
			return;
		}
		if (expression instanceof XAbstractFeatureCall) {
			val assignmentFeature = expression.feature
			if (assignmentFeature instanceof JvmFormalParameter) {
				// all parameters are considered NOT final by default
				val originalParam = assignmentFeature.originalParam
				if (originalParam === null || !originalParam.isFinal()) {
					return;
				}
			}
		}

		super.checkAssignment(expression, feature, simpleAssignment)
	}

	/**
	 * In case of an additional variable declaration we must use the container of
	 * the containing variable declaration, otherwise additional variables will always be
	 * detected as unused; similarly if the container is a semicolon statement which
	 * contains a variable declaration
	 */
	override protected isLocallyUsed(EObject target, EObject containerToFindUsage) {
		if (target instanceof XJAdditionalXVariableDeclaration &&
				containerToFindUsage instanceof XVariableDeclaration) {
			return isLocallyUsed(target, containerToFindUsage.eContainer)
		}
		if (containerToFindUsage instanceof XJSemicolonStatement) {
			return isLocallyUsed(target, containerToFindUsage.eContainer)
		}
		return super.isLocallyUsed(target, containerToFindUsage)
	}

	@Check
	def void checkContinue(XJContinueStatement st) {
		checkBranchingStatementInternal(
			st,
			"a loop",
			"continue",
			XAbstractWhileExpression,
			XBasicForLoopExpression
		)
	}

	@Check
	def void checkBreak(XJBreakStatement st) {
		checkBranchingStatementInternal(
			st,
			"a loop or a switch",
			"break",
			XAbstractWhileExpression,
			XBasicForLoopExpression,
			XSwitchExpression
		)
	}

	def private checkBranchingStatementInternal(XJBranchingStatement st, String errorDetails,
		String instruction,
		Class<? extends EObject>... validContainers) {
		val container = Wrapper.wrap(st.eContainer)
		while (container.get !== null) {
			if (validContainers.exists[c|c.isInstance(container.get)]) {
				return;
			}
			container.set(container.get.eContainer)
		}
		error(
			instruction + " cannot be used outside of " + errorDetails,
			st,
			null,
			INVALID_BRANCHING_STATEMENT
		)
	}

	@Check
	def checkMissingSemicolon(XJSemicolonStatement e) {
		if (e.semicolon === null) {
			errorMissingSemicolon(e.expression)
		}
	}

	@Check
	def checkMissingSemicolon(XImportDeclaration e) {
		checkMissingSemicolonInternal(e)
	}

	@Check
	def checkTryWithResources(XJTryWithResourcesStatement e) {
		val resourceDeclarations = e.resourceDeclarations
		val numOfResources = resourceDeclarations.size
		if (numOfResources == 0) {
			error(
				'Syntax error on token "(", Resources expected after this token',
				e, jbasePackage.XJTryWithResourcesStatement_OpenParenthesis,
				MISSING_RESOURCES
			)
		} else {
			// in the last declaration the ';' is optional
			for (r : resourceDeclarations.take(numOfResources-1)) {
				if (r.semicolon === null) {
					errorMissingSemicolon(r)
				}
			}
		}
	}

	@Check
	def void checkAutoCloseableResource(XJTryWithResourcesVariableDeclaration e) {
		val declaredType = getActualType(e.type.type)
		val autoCloseable = new StandardTypeReferenceOwner(services, e).
			toLightweightTypeReference
			(services.typeReferences.getTypeForName(AutoCloseable, e))
		if (!autoCloseable.isAssignableFrom(declaredType)) {
			error(
				'The resource type ' + declaredType + ' does not implement java.lang.AutoCloseable',
				e, xbasePackage.XVariableDeclaration_Type,
				NOT_AUTO_CLOSEABLE
			)
		}
	}

	def private checkMissingSemicolonInternal(EObject e) {
		if (!e.hasSemicolon) {
			errorMissingSemicolon(e)
		}
	}
	
	private def errorMissingSemicolon(EObject e) {
		error(
			'Syntax error, insert ";" to complete Statement',
			e, null, MISSING_SEMICOLON
		)
	}

	@Check
	def checkMissingParentheses(XFeatureCall call) {
		checkMissingParenthesesInternal(call, call.isExplicitOperationCall)
	}

	@Check
	def checkMissingParentheses(XMemberFeatureCall call) {
		checkMissingParenthesesInternal(call, call.isExplicitOperationCall)
	}

	@Check
	def checkMissingParentheses(XConstructorCall call) {
		if (!call.explicitConstructorCall) {
			error(
				'Syntax error, insert "()" to complete Expression',
				call,
				xbasePackage.XConstructorCall_Constructor,
				MISSING_PARENTHESES
			)
		}
	}

	@Check
	def checkConstructorCallRawType(XJConstructorCall call) {
		if (call.isRawType) {
			val constructor = call.constructor
			val constructorType = constructor.eContainer as JvmType
			var StringBuilder message=new StringBuilder(64) 
			message.append(constructorType.getSimpleName()) 
			message.append(" is a raw type. References to generic type ") 
			message=proxyAwareUIStrings.appendTypeSignature(constructorType, message) 
			message.append(" should be parameterized")
			warning(
				message.toString(),
				xbasePackage.XConstructorCall_Constructor,
				IssueCodes.RAW_TYPE
			);
		}
	}

	@Check
	def checkArrayConstructor(XJArrayConstructorCall cons) {
		val arrayLiteral = cons.arrayLiteral
		val dimensionExpressions = cons.indexes

		if (dimensionExpressions.empty && arrayLiteral === null) {
			error(
				"Constructor must provide either dimension expressions or an array initializer",
				cons,
				null,
				ARRAY_CONSTRUCTOR_EITHER_DIMENSION_EXPRESSION_OR_INITIALIZER
			)
		} else if (!dimensionExpressions.empty && arrayLiteral !== null) {
			error(
				"Cannot define dimension expressions when an array initializer is provided",
				cons,
				null,
				ARRAY_CONSTRUCTOR_BOTH_DIMENSION_EXPRESSION_AND_INITIALIZER
			)
		} else {
			val dimensionsAndIndexes = cons.arrayDimensionIndexAssociations
			var foundEmptyDimension = false
			for (d : dimensionsAndIndexes) {
				if (d === null) {
					foundEmptyDimension = true
				} else if (foundEmptyDimension) {
					error(
						"Cannot specify an array dimension after an empty dimension",
						d,
						null,
						ARRAY_CONSTRUCTOR_DIMENSION_EXPRESSION_AFTER_EMPTY_DIMENSION
					)
					return
				}
			}
		}
	}

	@Check
	def void checkCharacterLiteral(XJCharLiteral c) {
		val lenght = c.value.length
		if (lenght > 1) {
			error(
				"Invalid character constant",
				c, null,
				INVALID_CHARACTER_CONSTANT
			)
		}
	}

	def private checkMissingParenthesesInternal(XAbstractFeatureCall call, boolean explicitOpCall) {
		// length for arrays is OK without parentheses
		if (call.feature instanceof JvmOperation && !explicitOpCall &&
			call.feature.simpleName != JbaseOperatorMapping.ARRAY_LENGTH) {
			error(
				'Syntax error, insert "()" to complete method call',
				call,
				xbasePackage.XAbstractFeatureCall_Feature,
				MISSING_PARENTHESES
			)
		}
	}

	@Check
	def void checkVarArgComesLast(XJJvmFormalParameter param) {
		if (param.isVarArgs()) {
			val params = param.eContainer().eGet(param.eContainingFeature()) as List<XJJvmFormalParameter>
			if (param != params.last) {
				error(
					"A vararg must be the last parameter.",
					param,
					jbasePackage.XJJvmFormalParameter_VarArgs,
					JbaseIssueCodes.INVALID_USE_OF_VAR_ARGS
				);
			}
		}
	}

	/**
	 * This can be explicitly called on an XBlockExpression which represents
	 * the body of an inferred JvmOperation; it will check that,
	 * if the corresponding Java method is not void, a return
	 * is specified in all possible paths.
	 */
	def protected void checkMissingReturn(XBlockExpression body) {
		val jvmOperation = logicalContainerProvider.getLogicalContainer(body)
		val types = batchTypeResolver.resolveTypes(body);
		if (types.getActualType(jvmOperation).isPrimitiveVoid()) 
			return;
		val lastExpression = body.expressions.last
		if (lastExpression === null) {
			errorMissingReturnStatement(body)
			return
		}
		if (!sureReturnComputer.isSureReturn(lastExpression)) {
			errorMissingReturnStatement(lastExpression)
		}
	}

	/**
	 * This can be explicitly called on an XBlockExpression which represents
	 * the body of an inferred JvmOperation; it will check that
	 * variable references refer to variables that are surely initialized,
	 * according to the Java semantics.  This should be called only on
	 * top block expressions, as stated above.
	 */
	def protected checkVariableInitialization(XBlockExpression e) {
		initializedVariableFinder.detectNotInitialized(e) [
			ref |
			error(
				"The local variable " +
					ref.toString + " may not have been initialized",
				ref,
				xbasePackage.XAbstractFeatureCall_Feature,
				NOT_INITIALIZED_VARIABLE
			)
		]
	}

	def protected errorMissingReturnStatement(XExpression e) {
		var source = e
		if (e instanceof XJSemicolonStatement) {
			source = e.expression
		}
		error("Missing return", source, null, MISSING_RETURN)
	}

	/**
	 * Avoids issues that are specific to Xbase, like the warning when you
	 * compare to null with '==' instead of '==='
	 * (see https://github.com/LorenzoBettini/jbase/issues/72)
	 */
	override protected addIssue(String message, EObject source, EStructuralFeature feature, int index, String issueCode, String... issueData) {
		if (IssueCodes.EQUALS_WITH_NULL == issueCode)
			return;
		super.addIssue(message, source, feature, index, issueCode, issueData)
	}

}
