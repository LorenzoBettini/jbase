# Java assert statements

Jbase supports Java's assertion statement, with or without a detail expression:

```java
assert condition;
assert condition : "expected the condition to hold";
```

The condition must have type `boolean`. The optional detail expression may have
any non-void type. Jbase generates native Java `assert` statements, so their
runtime behavior follows the Java assertion status of the generated class:
assertions are disabled by default and can be enabled with `-ea`.

The grammar represents each assertion as `XJAssertStatement` inside the usual
semicolon statement wrapper. The type computer checks the condition and detail
expression; the compiler and formatter preserve the native statement syntax.

## Implementation and tests

[Jbase.xtext](../jbase/src/jbase/Jbase.xtext) defines the assertion node.
[JbaseTypeComputer](../jbase/src/jbase/typesystem/JbaseTypeComputer.java)
computes its condition and detail expressions,
[JbaseXbaseCompiler](../jbase/src/jbase/compiler/JbaseXbaseCompiler.java)
emits native Java, and
[JbaseFormatter](../jbase/src/jbase/formatting2/JbaseFormatter.java) formats
the statement.
[JbaseAssertStatementTest](../jbase.tests/src/jbase/tests/JbaseAssertStatementTest.java)
checks generated Java, validation, and disabled-assertion behavior.
[JbaseFormatterTest](../jbase.tests/src/jbase/tests/JbaseFormatterTest.java)
and [JbaseSerializerTest](../jbase.tests/src/jbase/tests/JbaseSerializerTest.java)
cover formatting and serialization.
