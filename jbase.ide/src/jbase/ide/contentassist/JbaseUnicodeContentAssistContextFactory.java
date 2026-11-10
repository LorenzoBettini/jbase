package jbase.ide.contentassist;

import org.eclipse.xtext.ide.editor.contentassist.ContentAssistContext;
import org.eclipse.xtext.ide.editor.contentassist.antlr.ContentAssistContextFactory;
import org.eclipse.xtext.resource.XtextResource;
import org.eclipse.xtext.util.ITextRegion;
import org.eclipse.xtext.util.TextRegion;

import jbase.conversion.JavaUnicodeEscapeTranslator;

/**
 * Relates editor offsets in the original source to the translated node model used by Jbase.
 */
public class JbaseUnicodeContentAssistContextFactory extends ContentAssistContextFactory {

	/** Computes completion in translated source, then restores editor offsets in the result. */
	@Override
	public ContentAssistContext[] create(final String document, final ITextRegion selection,
			final int offset, final XtextResource resource) {
		final var translation = JavaUnicodeEscapeTranslator.translateWithOffsets(document);
		final var translatedSelection = new TextRegion(
				translation.translatedOffset(selection.getOffset()),
				translation.translatedOffset(selection.getEndOffset())
						- translation.translatedOffset(selection.getOffset()));
		final var contexts = super.create(translation.text(), translatedSelection,
				translation.translatedOffset(offset), resource);
		for (var i = 0; i < contexts.length; i++) {
			final var context = contexts[i];
			final var region = context.getReplaceRegion();
			final var start = translation.originalOffset(region.getOffset());
			final var end = translation.originalOffset(region.getEndOffset());
			contexts[i] = context.copy()
					.setOffset(translation.originalOffset(context.getOffset()))
					.setReplaceRegion(new TextRegion(start, end - start))
					.toContext();
		}
		return contexts;
	}
}
