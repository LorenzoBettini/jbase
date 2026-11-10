# Java escape sequences and Unicode translation

Jbase translates eligible Java Unicode escapes throughout a source file before
tokenization. Repeated `u` markers are accepted, and the backslash eligibility
rules prevent escaped backslashes from starting a Unicode escape. This makes
Unicode escapes work in identifiers and in source syntax as well as in literals.

String and character literals support Java's standard escapes, the `\s` space
escape, and octal escapes. Unicode translation is already complete when literal
converters interpret these escapes. The parser combines non-ASCII identifier
characters with adjacent ASCII identifier tokens, and identifiers retain their
original spelling in the model.

Malformed ordinary literal escapes and invalid identifier code points are
reported as conversion errors. Text blocks use the same source-level translation
before their existing indentation normalization and escape processing.

## Implementation

`JavaUnicodeEscapeTranslator.translate` scans source characters before lexing,
tracking contiguous backslashes and whether the previous character came from a
Unicode escape. `JbaseUnicodeParser` handles Jbase itself. For languages using
Jbase, `JbaseGeneratorFragment2` generates a Unicode parser in `src-gen` and
binds it in the generated base runtime module; no handwritten parser or binding
is needed. Both parsers route their normal, parser-rule, and rule-call entry
points through `JbaseUnicodeParserSupport`; incremental reparsing applies edits
to the saved original source and translates the result once.

`JavaUnicodeIdentifierTokenSource.nextToken` joins adjacent lexer tokens only
when their text forms a valid Java identifier by code point. This preserves
keyword tokens while allowing identifiers that mix ASCII and Unicode
characters, including supplementary code points. The identifier value
converter validates the resulting token before storing it in the model.
`JbaseIdentifierValueConverter` also uses the combined Unicode tokens when
validating proposal text for serialization.

The generated content assist parser translates escapes and combines identifier
tokens for follow-element calculation. `JbaseUnicodeContentAssistContextFactory`
maps cursor and replacement offsets between the original editor text and the
translated node model. `JbaseGeneratorFragment2` binds these components in each
consumer's generated IDE and UI modules, so consuming languages need no manual
content assist setup.

String and character converters decode ordinary escapes after source-level
translation. The text-block converter intentionally does not repeat that
translation.

Regression coverage is in `JbaseJavaEscapeTest`, `JbaseTextBlockTest`,
`PureJbaseParsingTest`, `JbaseContentAssistTest`, and `AllJavaFeaturesTest`.
