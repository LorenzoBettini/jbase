# Java escape sequences and Unicode translation

Whole-source Java Unicode translation is **opt in** for DSLs using Jbase. When
enabled, Jbase translates eligible Unicode escapes before tokenization. Repeated
`u` markers are accepted, and backslash eligibility rules prevent escaped
backslashes from starting a Unicode escape. This makes Unicode escapes work in
identifiers and source syntax as well as in literals.

String and character literals support Java's standard escapes, the `\s` space
escape, and octal escapes in both modes. With whole-source translation enabled,
Unicode translation is already complete when literal converters interpret these
escapes. The parser combines non-ASCII identifier characters with adjacent ASCII
identifier tokens, and identifiers retain their original spelling in the model.

Malformed ordinary literal escapes and invalid identifier code points are
reported as conversion errors. Text blocks translate Unicode escapes before
indentation normalization and escape processing in either mode.

## Enabling whole-source translation

`JbaseGeneratorFragment2` leaves whole-source translation disabled by default.
To enable it for a DSL, set the parameter explicitly in its MWE2 workflow:

```mwe2
xbaseSupport = jbase.mwe2.generator.JbaseGeneratorFragment2 {
	enableUnicodeSourceTranslation = true
}
```

The Jbase test language enables it. PureJbase, Jbasescript, and `.jbase` use
the default partial-reparse parser. Whole-source translation reparses the entire
file after an edit, which can increase editor latency for large files containing
many small methods. Ordinary Java literal escapes do not require this option.

## Implementation

`JavaUnicodeEscapeTranslator.translate` scans source characters before lexing,
tracking contiguous backslashes and whether the previous character came from a
Unicode escape. When enabled, `JbaseGeneratorFragment2` generates a Unicode
parser in `src-gen` and binds it in the generated base runtime module. Its
normal, parser-rule, and rule-call entry points use `JbaseUnicodeParserSupport`;
reparsing applies edits to the saved original source and parses it again.
Without the option, Xtext uses partial reparsing and
`PatchedLookAheadPreservingNodeModelBuilder` safely transfers lookahead when a
composite node changes into a leaf.

`JavaUnicodeIdentifierTokenSource.nextToken` joins adjacent lexer tokens only
when their text forms a valid Java identifier by code point. This preserves
keyword tokens while allowing identifiers that mix ASCII and Unicode
characters, including supplementary code points. The identifier value
converter validates the resulting token before storing it in the model.
`JbaseIdentifierValueConverter` also uses the combined Unicode tokens when
validating proposal text for serialization.

With the option enabled, the generated content assist parser translates escapes
and combines identifier tokens for follow-element calculation.
`JbaseUnicodeContentAssistContextFactory` maps cursor and replacement offsets
between the original editor text and the translated node model. The generator
binds these components in the DSL's IDE and UI modules.

String and character converters decode ordinary escapes. The default text-block
converter translates Unicode escapes locally; opted-in DSLs bind
`JbasePretranslatedTextBlockValueConverter` so those escapes are not translated
twice.

## Parser benchmark

[`ReparseBenchmarkTest`](../jbase.tests/src/jbase/tests/ReparseBenchmarkTest.java)
compares the generated partial parser with the opted-in parser on the same test
language grammar. It changes one character in a string literal in the middle
of a file with many small methods. Each median follows 12 warmup edits and 30
timed edits in the Tycho test JVM. The test checks tree reuse but has no timing
threshold. Run it from the repository root with `-Dtest=ReparseBenchmarkTest`
using the focused `jbase.tests` Maven command in `AGENTS.md`.

| Methods | Source size | Partial parser median | Unicode parser median |
| ---: | ---: | ---: | ---: |
| 100 | 3.6 KB | 0.16 ms | 0.45 ms |
| 1,000 | 37.8 KB | 0.97 ms | 3.51 ms |
| 3,000 | 117.8 KB | 1.84 ms | 9.03 ms |

These Java 25 measurements cover parsing only, with ASCII input and no editor linking,
validation, or UI work. They are indicative; hardware and JVM warmup affect
the exact timings.

Regression coverage is in `JbaseJavaEscapeTest`, `JbaseTextBlockTest`,
`PureJbaseParsingTest`, `PatchedLookAheadPreservingNodeModelBuilderTest`,
`JbaseContentAssistTest`, and `AllJavaFeaturesTest`.
