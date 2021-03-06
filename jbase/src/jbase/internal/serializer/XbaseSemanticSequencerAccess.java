package jbase.internal.serializer;

import java.util.List;
import java.util.Set;

import org.eclipse.xtext.common.types.JvmIdentifiableElement;
import org.eclipse.xtext.naming.IQualifiedNameConverter;
import org.eclipse.xtext.nodemodel.ICompositeNode;
import org.eclipse.xtext.nodemodel.INode;
import org.eclipse.xtext.nodemodel.util.NodeModelUtils;
import org.eclipse.xtext.resource.IEObjectDescription;
import org.eclipse.xtext.scoping.IScope;
import org.eclipse.xtext.scoping.IScopeProvider;
import org.eclipse.xtext.serializer.ISerializationContext;
import org.eclipse.xtext.serializer.acceptor.SequenceFeeder;
import org.eclipse.xtext.serializer.diagnostic.SerializationDiagnostic;
import org.eclipse.xtext.serializer.sequencer.ISemanticNodeProvider.INodesForEObjectProvider;
import org.eclipse.xtext.serializer.tokens.SerializerScopeProviderBinding;
import org.eclipse.xtext.xbase.XBinaryOperation;
import org.eclipse.xtext.xbase.XbasePackage;
import org.eclipse.xtext.xbase.services.XbaseGrammarAccess;
import org.eclipse.xtext.xbase.services.XbaseGrammarAccess.XAdditiveExpressionElements;
import org.eclipse.xtext.xbase.services.XbaseGrammarAccess.XAndExpressionElements;
import org.eclipse.xtext.xbase.services.XbaseGrammarAccess.XEqualityExpressionElements;
import org.eclipse.xtext.xbase.services.XbaseGrammarAccess.XMultiplicativeExpressionElements;
import org.eclipse.xtext.xbase.services.XbaseGrammarAccess.XOrExpressionElements;
import org.eclipse.xtext.xbase.services.XbaseGrammarAccess.XOtherOperatorExpressionElements;
import org.eclipse.xtext.xbase.services.XbaseGrammarAccess.XRelationalExpressionElements;

import com.google.common.collect.Sets;
import com.google.inject.Inject;

import jbase.serializer.AbstractJbaseSemanticSequencer;
import jbase.services.JbaseGrammarAccess;
import jbase.services.JbaseGrammarAccess.XBitwiseInclusiveOrExpressionElements;

/**
 * Customizations needed since in the grammar we have
 * 
 * <pre>=>({XBinaryOperation.leftOperand=current} feature=[types::JvmIdentifiableElement|OpMultiAssign]) rightOperand=XAssignment</pre>
 * 
 * @author Lorenzo Bettini
 *
 */
public class XbaseSemanticSequencerAccess extends AbstractJbaseSemanticSequencer {

	@Inject
	private XbaseGrammarAccess grammarAccess;

	@Inject
	private IQualifiedNameConverter qualifiedNameConverter;

	@Inject
	@SerializerScopeProviderBinding
	private IScopeProvider scopeProvider;

	@Inject
	private JbaseGrammarAccess access;

	@Override
	protected void sequence_XAdditiveExpression_XAndExpression_XAssignment_XEqualityExpression_XMultiplicativeExpression_XOrExpression_XOtherOperatorExpression_XRelationalExpression(ISerializationContext context, XBinaryOperation operation) {
		handleCustomBinaryOperation(context, operation);
	}

	@Override
	protected void sequence_XAdditiveExpression_XAndExpression_XAssignment_XBitwiseAndExpression_XBitwiseExclusiveOrExpression_XBitwiseInclusiveOrExpression_XEqualityExpression_XMultiplicativeExpression_XOrExpression_XOtherOperatorExpression_XRelationalExpression(ISerializationContext context, XBinaryOperation operation) {
		handleCustomBinaryOperation(context, operation);
	}

	private void handleCustomBinaryOperation(ISerializationContext context, XBinaryOperation operation) {
		INodesForEObjectProvider nodes = createNodeProvider(operation);
		SequenceFeeder acceptor = createSequencerFeeder(context, operation, nodes);
		XAdditiveExpressionElements opAdd = grammarAccess.getXAdditiveExpressionAccess();
		XMultiplicativeExpressionElements opMulti = grammarAccess.getXMultiplicativeExpressionAccess();
		XOtherOperatorExpressionElements opOther = grammarAccess.getXOtherOperatorExpressionAccess();
		XRelationalExpressionElements opCompare = grammarAccess.getXRelationalExpressionAccess();
		XEqualityExpressionElements opEquality = grammarAccess.getXEqualityExpressionAccess();
		XAndExpressionElements opAnd = grammarAccess.getXAndExpressionAccess();
		XOrExpressionElements opOr = grammarAccess.getXOrExpressionAccess();
		// difference with respect to Xbase
		jbase.services.JbaseGrammarAccess.XAssignmentElements opMultiAssign = access.getXAssignmentAccess();
		
		// bitwise operators
		XBitwiseInclusiveOrExpressionElements opBitwiseIncOr = access.getXBitwiseInclusiveOrExpressionAccess();
//		XBitwiseExclusiveOrExpressionElements opBitwiseExclOr = access.getXBitwiseExclusiveOrExpressionAccess();
//		XBitwiseAndExpressionElements opBitwiseAnd = access.getXBitwiseAndExpressionAccess();
		
		JvmIdentifiableElement feature = operation.getFeature();
		Set<String> operatorNames = Sets.newHashSet();
		if (feature.eIsProxy()) {
			List<INode> ops = NodeModelUtils.findNodesForFeature(operation, XbasePackage.Literals.XABSTRACT_FEATURE_CALL__FEATURE);
			for (INode o : ops)
				operatorNames.add(NodeModelUtils.getTokenText(o));
		} else {
			IScope scope = scopeProvider.getScope(operation, XbasePackage.Literals.XABSTRACT_FEATURE_CALL__FEATURE);
			for (IEObjectDescription desc : scope.getElements(feature))
				operatorNames.add(qualifiedNameConverter.toString(desc.getName()));
		}
		ICompositeNode featureNode = (ICompositeNode) nodes.getNodeForSingelValue(XbasePackage.Literals.XABSTRACT_FEATURE_CALL__FEATURE, operation.getFeature());
		String featureToken;
		
		if((featureToken = getValidOperator(operation, opAdd.getFeatureJvmIdentifiableElementOpAddParserRuleCall_1_0_0_1_0_1(), operatorNames, featureNode)) != null) {
			acceptor.accept(opAdd.getXBinaryOperationLeftOperandAction_1_0_0_0(), operation.getLeftOperand());
			acceptor.accept(opAdd.getFeatureJvmIdentifiableElementOpAddParserRuleCall_1_0_0_1_0_1(), operation.getFeature(), featureToken, featureNode);
			acceptor.accept(opAdd.getRightOperandXMultiplicativeExpressionParserRuleCall_1_1_0(), operation.getRightOperand());
		} else if((featureToken = getValidOperator(operation, opMulti.getFeatureJvmIdentifiableElementOpMultiParserRuleCall_1_0_0_1_0_1(), operatorNames, featureNode)) != null) {
			acceptor.accept(opMulti.getXBinaryOperationLeftOperandAction_1_0_0_0(), operation.getLeftOperand());
			acceptor.accept(opMulti.getFeatureJvmIdentifiableElementOpMultiParserRuleCall_1_0_0_1_0_1(), operation.getFeature(), featureToken, featureNode);
			acceptor.accept(opMulti.getRightOperandXUnaryOperationParserRuleCall_1_1_0(), operation.getRightOperand());
		} else if((featureToken = getValidOperator(operation, opOther.getFeatureJvmIdentifiableElementOpOtherParserRuleCall_1_0_0_1_0_1(), operatorNames, featureNode)) != null) {
			acceptor.accept(opOther.getXBinaryOperationLeftOperandAction_1_0_0_0(), operation.getLeftOperand());
			acceptor.accept(opOther.getFeatureJvmIdentifiableElementOpOtherParserRuleCall_1_0_0_1_0_1(), operation.getFeature(), featureToken, featureNode);
			acceptor.accept(opOther.getRightOperandXAdditiveExpressionParserRuleCall_1_1_0(), operation.getRightOperand());
		} else if((featureToken = getValidOperator(operation, opCompare.getFeatureJvmIdentifiableElementOpCompareParserRuleCall_1_1_0_0_1_0_1(), operatorNames, featureNode)) != null) {
			acceptor.accept(opCompare.getXBinaryOperationLeftOperandAction_1_1_0_0_0(), operation.getLeftOperand());
			acceptor.accept(opCompare.getFeatureJvmIdentifiableElementOpCompareParserRuleCall_1_1_0_0_1_0_1(), operation.getFeature(), featureToken, featureNode);
			acceptor.accept(opCompare.getRightOperandXOtherOperatorExpressionParserRuleCall_1_1_1_0(), operation.getRightOperand());
		} else if((featureToken = getValidOperator(operation, opEquality.getFeatureJvmIdentifiableElementOpEqualityParserRuleCall_1_0_0_1_0_1(), operatorNames, featureNode)) != null) {
			acceptor.accept(opEquality.getXBinaryOperationLeftOperandAction_1_0_0_0(), operation.getLeftOperand());
			acceptor.accept(opEquality.getFeatureJvmIdentifiableElementOpEqualityParserRuleCall_1_0_0_1_0_1(), operation.getFeature(), featureToken, featureNode);
			acceptor.accept(opEquality.getRightOperandXRelationalExpressionParserRuleCall_1_1_0(), operation.getRightOperand());
		} else if((featureToken = getValidOperator(operation, opAnd.getFeatureJvmIdentifiableElementOpAndParserRuleCall_1_0_0_1_0_1(), operatorNames, featureNode)) != null) {
			acceptor.accept(opAnd.getXBinaryOperationLeftOperandAction_1_0_0_0(), operation.getLeftOperand());
			acceptor.accept(opAnd.getFeatureJvmIdentifiableElementOpAndParserRuleCall_1_0_0_1_0_1(), operation.getFeature(), featureToken, featureNode);
			acceptor.accept(opAnd.getRightOperandXEqualityExpressionParserRuleCall_1_1_0(), operation.getRightOperand());
		} else if((featureToken = getValidOperator(operation, opOr.getFeatureJvmIdentifiableElementOpOrParserRuleCall_1_0_0_1_0_1(), operatorNames, featureNode)) != null) {
			acceptor.accept(opOr.getXBinaryOperationLeftOperandAction_1_0_0_0(), operation.getLeftOperand());
			acceptor.accept(opOr.getFeatureJvmIdentifiableElementOpOrParserRuleCall_1_0_0_1_0_1(), operation.getFeature(), featureToken, featureNode);
			acceptor.accept(opOr.getRightOperandXAndExpressionParserRuleCall_1_1_0(), operation.getRightOperand());
		} // difference with respect to Xbase
		else if((featureToken = getValidOperator(operation, opMultiAssign.getFeatureJvmIdentifiableElementOpMultiAssignParserRuleCall_2_1_1_0_0_1_0_1(), operatorNames, featureNode)) != null) {
			acceptor.accept(opMultiAssign.getXBinaryOperationLeftOperandAction_2_1_1_0_0_0(), operation.getLeftOperand());
			acceptor.accept(opMultiAssign.getFeatureJvmIdentifiableElementOpMultiAssignParserRuleCall_2_1_1_0_0_1_0_1(), operation.getFeature(), featureToken, featureNode);
			acceptor.accept(opMultiAssign.getRightOperandXAssignmentParserRuleCall_2_1_1_1_0(), operation.getRightOperand());
		} else if((featureToken = getValidOperator(operation, opBitwiseIncOr.getFeatureJvmIdentifiableElementOpInclusiveOrParserRuleCall_1_0_0_1_0_1(), operatorNames, featureNode)) != null) {
			acceptor.accept(opOr.getXBinaryOperationLeftOperandAction_1_0_0_0(), operation.getLeftOperand());
			acceptor.accept(opOr.getFeatureJvmIdentifiableElementOpOrParserRuleCall_1_0_0_1_0_1(), operation.getFeature(), featureToken, featureNode);
			acceptor.accept(opOr.getRightOperandXAndExpressionParserRuleCall_1_1_0(), operation.getRightOperand());
		} else if (errorAcceptor != null) {
			errorAcceptor.accept(new SerializationDiagnostic(OPERATOR_NOT_SUPPORTED, operation, context, grammarAccess.getGrammar(), "Operator "+operatorNames+" is not supported."));
		} 
		acceptor.finish();
	}
}
