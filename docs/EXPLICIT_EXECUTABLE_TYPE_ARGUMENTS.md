# Explicit executable type arguments

Jbase supports explicit type arguments on Java method and constructor
invocations:

```java
java.util.List<String> values = java.util.Collections.<String>emptyList();
Object value = new <String>GenericConstructor("x");
```

Method invocation type arguments follow the receiver separator and precede the
method name. Constructor invocation type arguments follow `new` (or `.new` for
a qualified inner-class creation) and precede annotations and the created type.
They are distinct from class type arguments written after the type name:

```java
GenericConstructor<Integer> value =
	new <String, Integer>GenericConstructor<Integer>("text", 1);
```

The AST stores constructor executable arguments separately from class type
arguments. Constructor linking combines the lists in Java's executable-first
order for generic inference, while serialization and Java generation preserve
their source positions. Qualified and anonymous constructor calls use the same
ordering. Parsing, formatting, serialization, type computation and generated
Java are covered by regression tests and the Java-features showcase.

## Implementation and tests

[Jbase.xtext](../jbase/src/jbase/Jbase.xtext) models executable arguments
separately from class type arguments.
[JbaseTypeComputer](../jbase/src/jbase/typesystem/JbaseTypeComputer.java)
and [JbaseConstructorCallState](../jbase/src/jbase/typesystem/JbaseConstructorCallState.java)
apply Java's executable-first constructor inference order.
[JbaseXbaseCompiler](../jbase/src/jbase/compiler/JbaseXbaseCompiler.java)
emits method and constructor arguments in their source positions.
[JbaseCompilerTest](../jbase.tests/src/jbase/tests/JbaseCompilerTest.java)
checks generated Java and compilation for methods, constructors, qualified
inner constructors, and anonymous constructor calls.
[JbaseParsingTest](../jbase.tests/src/jbase/tests/JbaseParsingTest.java),
[JbaseFormatterTest](../jbase.tests/src/jbase/tests/JbaseFormatterTest.java),
and [JbaseSerializerTest](../jbase.tests/src/jbase/tests/JbaseSerializerTest.java)
cover parsing, formatting, and serialization.
