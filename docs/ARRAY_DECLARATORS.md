# Array declarators and initializers

Jbase accepts Java's post-name array dimensions for local declarations,
additional declarators, and typed parameters:

```java
int values[] = {1, 2,};
int scalar = 1, more[] = {3,};
java.util.function.Consumer<String[]> consumer =
    (String items[]) -> System.out.println(items[0]);
```

Array initializer lists, including those following `new T[]`, may have one
trailing comma. Generated Java uses the same array types and initializer values;
the post-name dimensions do not change the declared variable's type.

## Implementation and tests

[Jbase.xtext](../jbase/src/jbase/Jbase.xtext) records post-name dimensions on
declarations, additional declarators, and formal parameters.
[JbaseXbaseCompiler](../jbase/src/jbase/compiler/JbaseXbaseCompiler.java)
emits those dimensions in their Java positions.
[JbaseArrayDeclaratorTest](../jbase.tests/src/jbase/tests/JbaseArrayDeclaratorTest.java)
checks compilation, execution, target compatibility, and malformed initializers.
[JbaseFormatterTest](../jbase.tests/src/jbase/tests/JbaseFormatterTest.java)
and [JbaseSerializerTest](../jbase.tests/src/jbase/tests/JbaseSerializerTest.java)
cover formatting and serialization.
