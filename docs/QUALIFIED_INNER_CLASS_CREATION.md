# Qualified inner-class creation

Jbase supports Java's qualified creation syntax for non-static member classes:

```java
Outer outer = new Outer();
Outer.Inner inner = outer.new Inner();
```

The qualifier is an expression, so it can also be a method call or another
primary expression:

```java
Outer.Inner inner = getOuter().new Inner();
```

Constructor lookup uses the qualifier's resolved type to find the member class.
The qualifier is type-checked without consuming the creation expression's target
type, and generated Java retains the `qualifier.new Member(...)` form. Ordinary
constructor calls and static nested-class creation continue to use their existing
syntax.

## Implementation and tests

[Jbase.xtext](../jbase/src/jbase/Jbase.xtext) models the qualifier separately
from the member constructor call.
[JbaseAnonymousClassConstructorScopes](../jbase/src/jbase/scoping/JbaseAnonymousClassConstructorScopes.java)
and [JbaseSerializerScopeProvider](../jbase/src/jbase/serializer/JbaseSerializerScopeProvider.java)
provide constructor scopes for qualified creation and serialization.
[JbaseTypeComputer](../jbase/src/jbase/typesystem/JbaseTypeComputer.java)
computes the qualifier without the created type's target expectation, and
[JbaseXbaseCompiler](../jbase/src/jbase/compiler/JbaseXbaseCompiler.java)
emits the native `qualifier.new Member(...)` syntax.

[JbaseParsingTest](../jbase.tests/src/jbase/tests/JbaseParsingTest.java) checks
the parsed form;
[JbaseCompilerTest](../jbase.tests/src/jbase/tests/JbaseCompilerTest.java)
checks generated Java and compilation;
[JbaseFormatterTest](../jbase.tests/src/jbase/tests/JbaseFormatterTest.java)
and [JbaseSerializerTest](../jbase.tests/src/jbase/tests/JbaseSerializerTest.java)
cover formatting and serialization. The Java-features showcase also includes a
qualified inner-class construction.
