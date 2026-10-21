# Java text blocks

Jbase supports native Java text blocks as string literals. The opening `"""`
must be followed by optional spaces, tabs or form feeds and a line terminator.
The closing delimiter can appear on its own line or after the final content.

```java
String html = """
    <p class="message">
      Hello
    </p>
    """;
```

The value above is `<p class="message">\n  Hello\n</p>\n`. Moving the closing
delimiter two spaces to the left preserves two more spaces on each content line.
Moving it onto the `</p>` line omits the final newline. Tabs and spaces each count
as one character when computing incidental indentation.

Line endings become LF, incidental indentation and trailing whitespace are removed,
and then escapes are interpreted. `\s` preserves a space, including at the end
of a line. A backslash followed by a line terminator joins the next line without
adding a newline; indentation is removed before joining. Java string escapes,
octal escapes and Unicode escapes in the content are supported. Unicode translation
precedes line ending normalization and indentation removal.

These rules follow [JLS 3.10.6](https://docs.oracle.com/javase/specs/jls/se21/html/jls-3.html#jls-3.10.6).
Compile generated code containing text blocks with Java 15 or later.

## Implementation

`TEXT_BLOCK` is a separate terminal assigned to the existing `XStringLiteral.value`
feature. `JbaseTextBlockValueConverter` supplies its actual string value, so existing
Xbase typing, constant evaluation and annotation handling continue to work.

`JbaseXbaseCompiler` retrieves the literal's raw token from the node model. It
normalizes incidental whitespace while retaining ordinary escapes and emits a
native text block. The Java appendable adds the surrounding indentation uniformly
to its lines. Unicode escapes are translated before reindentation, so even escapes
that introduce whitespace or line terminators retain their Java meaning.
Ordinary strings and synthetic string literals keep the existing Xbase generation.

`JbaseTextBlockTest` compares parsed values with executed generated Java across
indentation, line endings, escapes, quotes and empty blocks. It also checks exact
generated Java, syntax errors, serialization and formatting. `AllJavaFeaturesTest`
includes a text block in the complete Java feature showcase.
