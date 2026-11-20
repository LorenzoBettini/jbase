package jbase.ui.refactoring.internal;

import jbase.ui.refactoring.JbaseExtractVariableRefactoring;

import org.apache.log4j.Logger;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.swt.widgets.Display;
import org.eclipse.xtext.resource.ILocationInFileProvider;
import org.eclipse.xtext.resource.XtextResource;
import org.eclipse.xtext.ui.editor.model.IXtextDocument;
import org.eclipse.xtext.ui.editor.utils.EditorUtils;
import org.eclipse.xtext.ui.refactoring.ui.RefactoringWizardOpenOperation_NonForking;
import org.eclipse.xtext.ui.refactoring.ui.SyncUtil;
import org.eclipse.xtext.xbase.ui.refactoring.ExpressionUtil;
import org.eclipse.xtext.xbase.ui.refactoring.ExtractVariableHandler;
import org.eclipse.xtext.xbase.ui.refactoring.ExtractVariableRefactoring;
import org.eclipse.xtext.xbase.ui.refactoring.RefactoredResourceCopier;

import com.google.inject.Inject;
import com.google.inject.Provider;

/**
 * Opens the Java-syntax extract-local wizard for editors using Jbase.
 *
 * @author Lorenzo Bettini
 */
public class JbaseExtractVariableHandler extends ExtractVariableHandler {
	private static final Logger LOG = Logger.getLogger(JbaseExtractVariableHandler.class);

	@Inject private SyncUtil syncUtil;
	@Inject private ExpressionUtil expressionUtil;
	@Inject private Provider<ExtractVariableRefactoring> refactoringProvider;
	@Inject private ILocationInFileProvider locationInFileProvider;
	@Inject private RefactoredResourceCopier resourceCopier;

	/** Selects the complete expression before opening the refactoring dialog. */
	@Override
	public Object execute(final ExecutionEvent event) throws ExecutionException {
		try {
			syncUtil.totalSync(false);
			final var editor = EditorUtils.getActiveXtextEditor(event);
			if (editor == null) {
				return null;
			}
			final var selection = (ITextSelection) editor.getSelectionProvider().getSelection();
			final IXtextDocument document = editor.getDocument();
			final XtextResource resource = document.priorityReadOnly(
				state -> resourceCopier.loadIntoNewResourceSet(state));
			final var expression = expressionUtil.findSelectedExpression(resource, selection);
			if (expression == null) {
				return null;
			}
			final var refactoring = refactoringProvider.get();
			if (!refactoring.initialize(editor, expression)) {
				return null;
			}
			final var region = locationInFileProvider.getFullTextRegion(expression);
			editor.selectAndReveal(region.getOffset(), region.getLength());
			final var wizard = new JbaseExtractVariableWizard(
				(JbaseExtractVariableRefactoring) refactoring);
			new RefactoringWizardOpenOperation_NonForking(wizard)
				.run(editor.getSite().getShell(), "Extract Local Variable");
		} catch (InterruptedException e) {
			return null;
		} catch (Exception e) {
			LOG.error("Error during refactoring", e);
			MessageDialog.openError(Display.getCurrent().getActiveShell(),
				"Error during refactoring", e.getMessage() + "\nSee log for details");
		}
		return null;
	}
}
