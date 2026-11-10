package jbase.mwe2.generator;

import java.io.Reader;

import org.eclipse.xtend2.lib.StringConcatenationClient;
import org.eclipse.xtext.ParserRule;
import org.eclipse.xtext.RuleCall;
import org.eclipse.xtext.parser.IParseResult;
import org.eclipse.xtext.parser.IParser;
import org.eclipse.xtext.util.ReplaceRegion;
import org.eclipse.xtext.xtext.generator.model.FileAccessFactory;
import org.eclipse.xtext.xtext.generator.model.GuiceModuleAccess;
import org.eclipse.xtext.xtext.generator.model.ManifestAccess;
import org.eclipse.xtext.xtext.generator.model.TypeReference;
import org.eclipse.xtext.xtext.generator.parser.antlr.ContentAssistGrammarNaming;
import org.eclipse.xtext.xtext.generator.parser.antlr.GrammarNaming;
import org.eclipse.xtext.xtext.generator.xbase.XbaseGeneratorFragment2;
import org.eclipse.xtext.xtext.generator.xbase.XbaseUsageDetector;

import com.google.inject.Inject;

/**
 * A customization of {@link XbaseGeneratorFragment2} where we override the base
 * class for runtime and UI module, so that a DSL using Jbase will inherit our
 * customization. This must be used in the new MWE2 files (starting from Xtext
 * 2.9).
 * 
 * @author Lorenzo Bettini
 */
public class JbaseGeneratorFragment2 extends XbaseGeneratorFragment2 {
	@Inject
	private XbaseUsageDetector xbaseUsageDetector;

	@Inject
	private FileAccessFactory fileAccessFactory;

	@Inject
	private GrammarNaming grammarNaming;

	@Inject
	private ContentAssistGrammarNaming contentAssistNaming;

	@Override
	public void generate() {
		if (!xbaseUsageDetector.inheritsXbase(this.getGrammar())) {
			return;
		}
		ManifestAccess runtimeManifest = getProjectConfig().getRuntime().getManifest();
		if (runtimeManifest != null) {
			runtimeManifest.getRequiredBundles().add("jbase");
		}
		ManifestAccess uiManifest = getProjectConfig().getEclipsePlugin().getManifest();
		if (uiManifest != null) {
			uiManifest.getRequiredBundles().add("jbase.ui");
			uiManifest.getRequiredBundles().add("jbase.ide");
		}
		ManifestAccess ideManifest = getProjectConfig().getGenericIde().getManifest();
		if (ideManifest != null) {
			ideManifest.getRequiredBundles().add("jbase");
			ideManifest.getRequiredBundles().add("jbase.ide");
		}
		super.generate();
		generateUnicodeParser();
		generateUnicodeContentAssistParser();
		new GuiceModuleAccess.BindingFactory()
				.addTypeToType(TypeReference.typeRef(
						"org.eclipse.xtext.ide.editor.contentassist.antlr.IContentAssistParser"),
						unicodeContentAssistParserType())
				.addTypeToType(TypeReference.typeRef(
						"org.eclipse.xtext.ide.editor.contentassist.antlr.ContentAssistContextFactory"),
						TypeReference.typeRef(
								"jbase.ide.contentassist.JbaseUnicodeContentAssistContextFactory"))
				.contributeTo(getLanguage().getIdeGenModule());
	}

	/** Generates the parser subclass that translates Java Unicode escapes for this language. */
	private void generateUnicodeParser() {
		final var parserType = grammarNaming.getParserClass(getGrammar());
		final var lexerType = grammarNaming.getLexerClass(getGrammar());
		final var unicodeParserType = unicodeParserType();
		final var file = fileAccessFactory.createGeneratedJavaFile(unicodeParserType);
		final var parserName = file.importType(parserType);
		final var lexerName = file.importType(lexerType);
		for (final var type : new Class<?>[] { Reader.class, ParserRule.class,
				RuleCall.class, IParseResult.class, ReplaceRegion.class }) {
			file.importType(TypeReference.typeRef(type));
		}
		file.importType(TypeReference.typeRef("org.antlr.runtime.CharStream"));
		file.importType(TypeReference.typeRef("org.antlr.runtime.TokenSource"));
		file.importType(TypeReference.typeRef("jbase.conversion.JavaUnicodeIdentifierTokenSource"));
		file.importType(TypeReference.typeRef("jbase.conversion.JbaseUnicodeParserSupport"));
		final var content = """
			/** Applies Java Unicode translation to this language's generated parser. */
			public class %s extends %s {

				private final JbaseUnicodeParserSupport parserSupport = new JbaseUnicodeParserSupport();

				/** Combines adjacent Unicode identifier tokens emitted by the generated lexer. */
				@Override
				protected TokenSource createLexer(final CharStream input) {
					final var lexer = super.createLexer(input);
					return new JavaUnicodeIdentifierTokenSource(lexer, %s.RULE_ID);
				}

				/** Translates Unicode escapes before parsing the default rule. */
				@Override
				public IParseResult doParse(final Reader reader) {
					return parserSupport.parse(reader, super::doParse);
				}

				/** Translates Unicode escapes before parsing a selected parser rule. */
				@Override
				public IParseResult parse(final ParserRule rule, final Reader reader) {
					return parserSupport.parse(reader,
							translatedReader -> super.parse(rule, translatedReader));
				}

				/** Translates Unicode escapes before parsing a selected rule call. */
				@Override
				public IParseResult parse(final RuleCall ruleCall, final Reader reader,
						final int initialLookAhead) {
					return parserSupport.parse(reader,
							translatedReader -> super.parse(ruleCall, translatedReader, initialLookAhead));
				}

				/** Applies incremental edits to the original source before translation. */
				@Override
				protected IParseResult doReparse(final IParseResult previousParseResult,
						final ReplaceRegion replaceRegion) {
					return parserSupport.reparse(previousParseResult, replaceRegion, this::doParse);
				}

				/** Enables reparsing through the original-source tracker. */
				@Override
				protected boolean isReparseSupported() {
					return true;
				}
			}
			""".formatted(unicodeParserType.getSimpleName(), parserName, lexerName);
		file.setContent(new StringConcatenationClient() {
			@Override
			protected void appendTo(final TargetStringConcatenation builder) {
				builder.append(content);
			}
		});
		file.writeTo(getProjectConfig().getRuntime().getSrcGen());
	}

	/** Names a parser beside the generated ANTLR parser, in its already exported package. */
	private TypeReference unicodeParserType() {
		final var parserType = grammarNaming.getParserClass(getGrammar());
		final var parserName = parserType.getSimpleName();
		final var unicodeParserName = parserName.substring(0, parserName.length() - "Parser".length())
				+ "UnicodeParser";
		return TypeReference.typeRef(parserType.getPackageName() + "." + unicodeParserName);
	}

	/** Generates Unicode tokenization for the language-specific content assist parser. */
	private void generateUnicodeContentAssistParser() {
		final var parserType = contentAssistNaming.getParserClass(getGrammar());
		final var lexerType = contentAssistNaming.getLexerClass(getGrammar());
		final var unicodeParserType = unicodeContentAssistParserType();
		final var file = fileAccessFactory.createGeneratedJavaFile(unicodeParserType);
		final var parserName = file.importType(parserType);
		final var lexerName = file.importType(lexerType);
		file.importType(TypeReference.typeRef("org.antlr.runtime.TokenSource"));
		file.importType(TypeReference.typeRef("jbase.conversion.JavaUnicodeEscapeTranslator"));
		file.importType(TypeReference.typeRef("jbase.conversion.JavaUnicodeIdentifierTokenSource"));
		final var content = """
			/** Applies Java Unicode tokenization to content assist for this language. */
			public class %s extends %s {

				/** Translates escapes and combines Unicode identifier tokens for follow elements. */
				@Override
				protected TokenSource createTokenSource(final String input) {
					final var lexer = super.createTokenSource(
							JavaUnicodeEscapeTranslator.translate(input));
					return new JavaUnicodeIdentifierTokenSource(lexer, %s.RULE_ID);
				}
			}
			""".formatted(unicodeParserType.getSimpleName(), parserName, lexerName);
		file.setContent(new StringConcatenationClient() {
			@Override
			protected void appendTo(final TargetStringConcatenation builder) {
				builder.append(content);
			}
		});
		file.writeTo(getProjectConfig().getGenericIde().getSrcGen());
	}

	/** Names the content assist parser beside its generated ANTLR parser. */
	private TypeReference unicodeContentAssistParserType() {
		final var parserType = contentAssistNaming.getParserClass(getGrammar());
		final var parserName = parserType.getSimpleName();
		final var unicodeParserName = parserName.substring(0, parserName.length() - "Parser".length())
				+ "UnicodeContentAssistParser";
		return TypeReference.typeRef(parserType.getPackageName() + "." + unicodeParserName);
	}

	@Override
	protected void contributeRuntimeGuiceBindings() {
		super.contributeRuntimeGuiceBindings();
		getLanguage().getRuntimeGenModule()
			.setSuperClass(TypeReference.typeRef("jbase.DefaultJbaseRuntimeModule"));
		new GuiceModuleAccess.BindingFactory()
				.addTypeToType(TypeReference.typeRef(IParser.class), unicodeParserType())
				.contributeTo(getLanguage().getRuntimeGenModule());
	}

	@Override
	protected void contributeEclipsePluginGuiceBindings() {
		super.contributeEclipsePluginGuiceBindings();
		getLanguage().getEclipsePluginGenModule()
			.setSuperClass(TypeReference.typeRef("jbase.ui.DefaultJbaseUiModule"));
		new GuiceModuleAccess.BindingFactory()
				.addTypeToType(TypeReference.typeRef(
						"org.eclipse.xtext.ide.editor.contentassist.antlr.IContentAssistParser"),
						unicodeContentAssistParserType())
				.addTypeToType(TypeReference.typeRef(
						"org.eclipse.xtext.ide.editor.contentassist.antlr.ContentAssistContextFactory"),
						TypeReference.typeRef(
								"jbase.ide.contentassist.JbaseUnicodeContentAssistContextFactory"))
				.contributeTo(getLanguage().getEclipsePluginGenModule());
	}
}
