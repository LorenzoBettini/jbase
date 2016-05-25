/*
 * generated by Xtext
 */
package jbase.serializer;

import java.util.List;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.serializer.acceptor.SequenceFeeder;
import org.eclipse.xtext.serializer.sequencer.ISemanticNodeProvider.INodesForEObjectProvider;
import org.eclipse.xtext.xbase.XExpression;

import com.google.inject.Inject;

import jbase.internal.serializer.XbaseSemanticSequencerAccess;
import jbase.jbase.XJArrayConstructorCall;
import jbase.jbase.XJArrayDimension;
import jbase.services.JbaseGrammarAccess;
import jbase.services.JbaseGrammarAccess.XJArrayConstructorCallElements;
import jbase.util.JbaseModelUtil;

/**
 * Customized to deal with custom rules with special treatments.
 * 
 * @author Lorenzo Bettini
 *
 */
public class JbaseSemanticSequencer extends XbaseSemanticSequencerAccess {
	@Inject
	private JbaseGrammarAccess access;

	@Inject
	private JbaseModelUtil modelUtil;

//	@Override
//	protected void sequence_XJArrayConstructorCall(EObject context, XJArrayConstructorCall semanticObject) {
//		INodesForEObjectProvider nodes = createNodeProvider(semanticObject);
//		SequenceFeeder acceptor = createSequencerFeeder(semanticObject, nodes);
//		XJArrayConstructorCallElements elements = access.getXJArrayConstructorCallAccess();
//
//		acceptor.accept(elements.getTypeJvmTypeQualifiedNameParserRuleCall_0_0_2_0_1(), semanticObject.getType());
//
//		List<XExpression> associations = modelUtil.arrayDimensionIndexAssociations(semanticObject);
//		List<XJArrayDimension> dimensions = semanticObject.getDimensions();
//		acceptor.accept(elements.getDimensionsXJArrayDimensionParserRuleCall_0_0_3_0(), dimensions.get(0), 0);
//		if (associations.get(0) != null) {
//			acceptor.accept(elements.getIndexesXExpressionParserRuleCall_1_0(), semanticObject.getIndexes().get(0), 0);
//		}
//		for (int i = 1; i < associations.size(); i++) {
//			acceptor.accept(elements.getDimensionsXJArrayDimensionParserRuleCall_3_0_0(), dimensions.get(i), i);
//			if (associations.get(i) != null) {
//				acceptor.accept(elements.getIndexesXExpressionParserRuleCall_3_1_0(), associations.get(i), i);
//			}
//		}
//
//		if (semanticObject.getArrayLiteral() != null) {
//			acceptor.accept(elements.getArrayLiteralXJArrayLiteralParserRuleCall_4_0(),
//					semanticObject.getArrayLiteral());
//		}
//
//		acceptor.finish();
//	}
}
