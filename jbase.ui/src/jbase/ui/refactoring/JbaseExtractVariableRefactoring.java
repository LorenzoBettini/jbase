/**
 * 
 */
package jbase.ui.refactoring;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.xtext.ui.editor.XtextEditor;
import org.eclipse.xtext.ui.editor.model.IXtextDocument;
import org.eclipse.xtext.util.ITextRegion;
import org.eclipse.xtext.xbase.XExpression;
import org.eclipse.xtext.xbase.compiler.ISourceAppender;
import org.eclipse.xtext.xbase.typesystem.IBatchTypeResolver;
import org.eclipse.xtext.xbase.typesystem.IResolvedTypes;
import org.eclipse.xtext.xbase.typesystem.references.LightweightTypeReference;
import org.eclipse.xtext.xbase.ui.refactoring.ExpressionUtil;
import org.eclipse.xtext.xbase.ui.refactoring.ExtractVariableRefactoring;
import org.eclipse.xtext.xbase.ui.refactoring.NewFeatureNameUtil;

import com.google.inject.Inject;

/**
 * Redefined since Java variable syntax is different from Xbase variable syntax.
 * 
 * @author Lorenzo Bettini
 *
 */
public class JbaseExtractVariableRefactoring extends ExtractVariableRefactoring {

	@Inject 
	private IBatchTypeResolver typeResolver;

	@Inject 
	private NewFeatureNameUtil nameUtil;

	@Inject 
	private ExpressionUtil expressionUtil;

	private IXtextDocument document;

	private XExpression expression;

	private String variableName;

	private boolean isFinal = true;

	@Override
	public boolean initialize(XtextEditor editor, XExpression expression) {
		boolean result = super.initialize(editor, expression);
		this.document = editor.getDocument();
		this.expression = expression;
		XExpression successor = expressionUtil.findSuccessorExpressionForVariableDeclaration(expression);
		nameUtil.setFeatureScopeContext(successor);
		variableName = nameUtil.getDefaultName(expression);
		return result;
	}

	@Override
	public void setFinal(boolean isFinal) {
		super.setFinal(isFinal);
		this.isFinal = isFinal;
	}

	@Override
	protected void appendDeclaration(ISourceAppender section, ITextRegion expressionRegion)
			throws BadLocationException {
		IResolvedTypes types = typeResolver.resolveTypes(expression);
		LightweightTypeReference expressionType = types.getActualType(expression);
		section
		.append(isFinal ? "final " : "")
		.append(expressionType)
		.append(" ")
		.append(variableName)
		.append(" = ")
		.append(document.get(expressionRegion.getOffset(), expressionRegion.getLength()))
		.append(";");
	}
}
