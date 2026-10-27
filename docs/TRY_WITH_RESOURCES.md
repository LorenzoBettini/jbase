# Try-with-resources

Jbase supports resource declarations and Java 9 existing-variable resources:

```java
var input = new java.io.ByteArrayInputStream(new byte[0]);
try (input) {}
```

An existing resource must refer to an `AutoCloseable` local variable or parameter
that is final or effectively final, or to a final field. Existing resources and
resource declarations can appear together and retain their written order:

```java
try (input; var second = createInput()) {
    // use both resources
}
```

The grammar stores existing-variable resources separately from Xbase's
declaration-only resource list. The compiler merges the two lists by source
position and emits the native Java resource syntax. Validation checks resource
types, finality, and separators.

The implementation is in [Jbase.xtext](../jbase/src/jbase/Jbase.xtext),
[JbaseValidator](../jbase/src/jbase/validation/JbaseValidator.java), and
[JbaseXbaseCompiler](../jbase/src/jbase/compiler/JbaseXbaseCompiler.java).

[JbaseVarTest](../jbase.tests/src/jbase/tests/JbaseVarTest.java) checks
resource inference, finality, resource types, serialization, compilation, and
execution. [JbaseCompilerTest](../jbase.tests/src/jbase/tests/JbaseCompilerTest.java)
checks generated Java and the written ordering of existing-variable and
declared resources. [JbaseParsingTest](../jbase.tests/src/jbase/tests/JbaseParsingTest.java)
also covers resource syntax and parser recovery.
