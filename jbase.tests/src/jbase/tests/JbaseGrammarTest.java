package jbase.tests;

import static org.junit.Assert.fail;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.xtext.IGrammarAccess;
import org.eclipse.xtext.ParserRule;
import org.eclipse.xtext.testing.InjectWith;
import org.eclipse.xtext.testing.XtextRunner;
import org.eclipse.xtext.validation.AbstractValidationMessageAcceptor;
import org.eclipse.xtext.xtext.OverriddenValueInspector;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.google.inject.Inject;

/**
 * Verifies that the grammar does not present any warning in the editor, e.g.,
 * for single-valued features that are overridden by multi-valued features.
 */
@RunWith(XtextRunner.class)
@InjectWith(JbaseInjectorProvider.class)
public class JbaseGrammarTest {

	@Inject
	private IGrammarAccess grammarAccess;

	@Test
	public void singleValuedFeaturesAreNotOverridden() {
		final var inspector = new OverriddenValueInspector(
				new AbstractValidationMessageAcceptor() {
			@Override
			public void acceptWarning(final String message, final EObject object,
					final EStructuralFeature feature, final int index, final String code,
					final String... issueData) {
				fail(message);
			}
		});
		for (final var rule : grammarAccess.getGrammar().getRules()) {
			if (rule instanceof ParserRule parserRule) {
				inspector.inspect(parserRule);
			}
		}
	}
}
