package jbase.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.StreamSupport;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.xtext.IGrammarAccess;
import org.eclipse.xtext.ParserRule;
import org.eclipse.xtext.XtextStandaloneSetup;
import org.eclipse.xtext.parser.IParser;
import org.eclipse.xtext.testing.InjectWith;
import org.eclipse.xtext.testing.XtextRunner;
import org.eclipse.xtext.validation.AbstractValidationMessageAcceptor;
import org.eclipse.xtext.xtext.OverriddenValueInspector;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.google.inject.Inject;

import jbase.JbaseStandaloneSetup;

/**
 * Verifies that the grammar does not present any warning in the editor, e.g.,
 * for single-valued features that are overridden by multi-valued features.
 */
@RunWith(XtextRunner.class)
@InjectWith(JbaseInjectorProvider.class)
public class JbaseGrammarTest {

	@Inject
	private IGrammarAccess grammarAccess;

	/**
	 * Parse the source: the runtime grammar model has already merged cardinalities
	 * and therefore cannot expose warnings about redundant cardinalities.
	 */
	@Test
	public void grammarSourceHasNoParseDiagnostics() throws Exception {
		final var parser = new XtextStandaloneSetup().createInjector().getInstance(IParser.class);
		try (final var source = JbaseStandaloneSetup.class.getResourceAsStream("Jbase.xtext")) {
			assertNotNull("Jbase grammar source", source);
			final var result = parser.parse(new InputStreamReader(source, StandardCharsets.UTF_8));
			final var diagnostics = StreamSupport.stream(result.getSyntaxErrors().spliterator(), false)
					.map(node -> node.getSyntaxErrorMessage().getMessage()).toList();
			assertEquals(List.of(), diagnostics);
		}
	}

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
