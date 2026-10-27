# Annotations on local variables, parameters, and type uses

Jbase accepts annotations on local declarations and Java-like parameter
positions. The annotation type is supplied by the embedding Java environment:
Jbase does not declare annotation types.

```java
@A final String value = "text";
method print(@A String parameter): void {}
java.util.function.Function<String, Integer> length =
    (@A var text) -> text.length();
for (@A String text : values) {
    System.out.println(text);
}
try (@A var input = openInput()) {
    input.read();
} catch (@A java.io.IOException exception) {
    System.out.println(exception.getMessage());
}
```

Type-use annotations are retained in generic arguments and wildcards, array
dimensions, casts, object and array creation, and nested types:

```java
java.util.List<@A String> values;
java.util.List<@A ?> unknownValues;
String @A [] names;
String text = (@A String) value;
Object created = new @A String("text");
Object array = new String @A [1];
```

For an inner type, import both the owner and member type:

```java
import java.util.Map;
import java.util.Map.Entry;

Map.@A Entry<String, String> entry;
```

The annotation's `@Target` must permit its use at the selected source position.
Annotations are emitted in generated Java, including when a DSL infers JVM
parameters from Jbase source parameters. The formatter and serializer preserve
the source syntax, including serialization without a node model. See
[AllJavaFeaturesTest](../jbase.tests/src/jbase/tests/AllJavaFeaturesTest.java)
for an embedding-language compilation example.

## Implementation and tests

[Jbase.xtext](../jbase/src/jbase/Jbase.xtext) adds annotation positions to
Jbase declarations and parameters.
[JbaseXbaseCompiler](../jbase/src/jbase/compiler/JbaseXbaseCompiler.java)
emits annotations on inferred JVM parameters, while
[JbaseTypeReferenceSerializer](../jbase/src/jbase/compiler/JbaseTypeReferenceSerializer.java)
preserves type-use annotation positions during Java generation.
[JbaseFormatter](../jbase/src/jbase/formatting2/JbaseFormatter.java) handles
the Jbase-specific source formatting.

[JbaseCompilerTest](../jbase.tests/src/jbase/tests/JbaseCompilerTest.java)
checks generated Java,
[JbaseSerializerTest](../jbase.tests/src/jbase/tests/JbaseSerializerTest.java)
checks annotation serialization, and
[JbaseFormatterTest](../jbase.tests/src/jbase/tests/JbaseFormatterTest.java)
checks formatting. [AllJavaFeaturesTest](../jbase.tests/src/jbase/tests/AllJavaFeaturesTest.java)
covers annotations in the end-to-end showcase.
