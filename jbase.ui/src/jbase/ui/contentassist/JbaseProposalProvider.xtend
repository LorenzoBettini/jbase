/*
 * generated by Xtext
 */
package jbase.ui.contentassist

import org.eclipse.emf.ecore.EObject
import org.eclipse.xtext.Assignment
import org.eclipse.xtext.common.types.TypesPackage
import org.eclipse.xtext.ui.editor.contentassist.ContentAssistContext
import org.eclipse.xtext.ui.editor.contentassist.ICompletionProposalAcceptor
import org.eclipse.xtext.xbase.XFeatureCall
import org.eclipse.xtext.xbase.typesystem.IExpressionScope

/** 
 * We redefine a few methods from the XbaseProposalProvider.
 * @author Lorenzo Bettini
 */
class JbaseProposalProvider extends jbase.ui.contentassist.AbstractJbaseProposalProvider {
	override void completeXVariableDeclaration_Right(EObject model, Assignment assignment, ContentAssistContext context,
		ICompletionProposalAcceptor acceptor) {
		// in our case the model can be the whole main expression, probably due to our parsing strategy...
		createLocalVariableAndImplicitProposals(model, IExpressionScope.Anchor.BEFORE, context, acceptor)
		// ... so we must manually force the content assist using the feature call as the model
		var EObject previousModel = context.getPreviousModel()
		if (previousModel instanceof XFeatureCall) {
			createLocalVariableAndImplicitProposals(previousModel, IExpressionScope.Anchor.BEFORE, context, acceptor)
		}
	}

	override void completeXForLoopExpression_ForExpression(EObject model, Assignment assignment,
		ContentAssistContext context, ICompletionProposalAcceptor acceptor) {
		// when we started to write something, like the name of the feature
		// the model is still the containing block expression, so we must retrieve the
		// feature call from the context
		var EObject previous = context.getPreviousModel()
		if (previous instanceof XFeatureCall) {
			createLocalVariableAndImplicitProposals(previous, IExpressionScope.Anchor.BEFORE, context, acceptor)
		}
		createLocalVariableAndImplicitProposals(model, IExpressionScope.Anchor.BEFORE, context, acceptor)
	}

	override void completeXCastedExpression_Type(EObject model, Assignment assignment, ContentAssistContext context,
		ICompletionProposalAcceptor acceptor) {
		completeJavaTypes(context, TypesPackage.Literals.JVM_PARAMETERIZED_TYPE_REFERENCE__TYPE, acceptor)
	}
}
