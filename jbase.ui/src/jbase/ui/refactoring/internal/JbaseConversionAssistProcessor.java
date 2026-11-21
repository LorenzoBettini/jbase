package jbase.ui.refactoring.internal;

import java.util.ArrayList;

import org.eclipse.jface.text.contentassist.CompletionProposal;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.quickassist.IQuickAssistInvocationContext;
import org.eclipse.xtext.resource.ILocationInFileProvider;
import org.eclipse.xtext.ui.editor.model.IXtextDocument;
import org.eclipse.xtext.ui.editor.quickfix.XtextQuickAssistProcessor;
import org.eclipse.xtext.xbase.XClosure;
import org.eclipse.xtext.xbase.ui.refactoring.RefactoredResourceCopier;

import com.google.inject.Inject;

import jbase.jbase.XJMethodReference;
import jbase.refactoring.JbaseFunctionalConversion;
import jbase.refactoring.JbaseFunctionalConversionValidator;

/** Adds context-sensitive functional conversions to the existing Xtext fixes. */
public class JbaseConversionAssistProcessor extends XtextQuickAssistProcessor {
	@Inject private JbaseFunctionalConversion conversion;
	@Inject private JbaseFunctionalConversionValidator validator;
	@Inject private ILocationInFileProvider locations;
	@Inject private RefactoredResourceCopier copier;

	@Override
	public boolean canAssist(final IQuickAssistInvocationContext context) {
		return true;
	}

	@Override
	public ICompletionProposal[] computeQuickAssistProposals(
			final IQuickAssistInvocationContext context) {
		final var proposals = new ArrayList<ICompletionProposal>();
		for (final var proposal : super.computeQuickAssistProposals(context)) {
			proposals.add(proposal);
		}
		if (context.getSourceViewer() == null
				|| !(context.getSourceViewer().getDocument() instanceof IXtextDocument document)) {
			return proposals.toArray(ICompletionProposal[]::new);
		}
		final var source = document.get();
		final var offset = context.getOffset();
		document.readOnly(resource -> {
			XClosure selected = null;
			var selectedLength = Integer.MAX_VALUE;
			for (final var iterator = resource.getAllContents(); iterator.hasNext();) {
				if (!(iterator.next() instanceof XClosure closure)) {
					continue;
				}
				final var region = locations.getFullTextRegion(closure);
				if (region.getOffset() <= offset
						&& offset <= region.getOffset() + region.getLength()
						&& region.getLength() < selectedLength) {
					selected = closure;
					selectedLength = region.getLength();
				}
			}
			if (selected != null) {
				final var replacement = conversion.convert(selected, source);
				if (replacement != null && validator.preservesBinding(selected,
						copier.loadIntoNewResourceSet(resource), source, replacement)) {
					final var region = locations.getFullTextRegion(selected);
					final var label = selected instanceof XJMethodReference
						? "Convert method reference to lambda"
						: "Convert lambda to method reference";
					proposals.add(new CompletionProposal(replacement, region.getOffset(),
						region.getLength(), replacement.length(), null, label, null, null));
				}
			}
			return null;
		});
		return proposals.toArray(ICompletionProposal[]::new);
	}
}
