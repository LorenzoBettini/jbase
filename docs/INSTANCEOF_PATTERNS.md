# Java instanceof type patterns

Jbase accepts Java type patterns and retains them in generated Java:

```java
Object value = "hello";
if (value instanceof String text && !text.isEmpty()) {
    System.out.println(text.length());
}
if (!(value instanceof final String text)) {
    System.out.println(value);
} else {
    System.out.println(text.length());
}
```

The checked operand keeps its declared type. The new variable receives the
checked type and is visible only where the pattern is definitely matched.
Negation exchanges the true and false scopes; `&&` makes true bindings available
on its right, and `||` makes false bindings available on its right. Boolean
assignment, equality, and non-short-circuit operators do not propagate bindings.
Conditional expressions apply the same rules to their two arms.

Ordinary pattern variables can be assigned and captured under Java's usual
effectively-final rules. A pattern written with `final` cannot be assigned.
Bindings may reuse names in disjoint scopes, but cannot redeclare a local or
parameter already in scope. Array types and unbounded wildcard types such as
`List<?>` work as pattern types.

Guard clauses can introduce a binding for following statements:

```java
if (!(value instanceof String text)) return;
System.out.println(text.length());
```

Loop conditions also supply true bindings to while/for bodies and basic-for
updates. A false binding can reach following statements when no break exits the
loop. Pattern variables are initialized wherever they are in scope.

## Implementation

* [Jbase.xtext](../jbase/src/jbase/Jbase.xtext) overrides relational expressions.
  `XJInstanceOfExpression` extends Xbase's ordinary instance-of node and adds an
  optional binding and a `final` flag. The binding is a `JvmFormalParameter`;
  its type is assigned from the enclosing expression's checked type.
* [JbasePatternVariables](../jbase/src/jbase/typesystem/JbasePatternVariables.java)
  computes definitely-matched bindings and isolated type-computation scopes.
  [JbaseTypeComputer](../jbase/src/jbase/typesystem/JbaseTypeComputer.java)
  invokes it before child computation and after statements in a block.
* [JbaseValidator](../jbase/src/jbase/validation/JbaseValidator.java) checks
  duplicate bindings across boolean/conditional operands and final assignment.
* [JbaseXbaseCompiler](../jbase/src/jbase/compiler/JbaseXbaseCompiler.java) emits
  native patterns. Conditions and pattern-dependent ternaries remain expressions;
  saving a match result in a temporary boolean would destroy Java's flow scope.
  For loops with pattern conditions likewise retain their native form.
* The serializer uses the overridden relational grammar and generic sequencing
  for pattern-containing binary expressions. Formatting and completion use the
  same model and scopes.

The scope rules follow [JLS 6.3](https://docs.oracle.com/javase/specs/jls/se21/html/jls-6.html#jls-6.3).
Record patterns and switch patterns are separate language features.

[JbaseInstanceOfPatternTest](../jbase.tests/src/jbase/tests/JbaseInstanceOfPatternTest.java)
checks validation, native Java generation, Java 21 compilation, execution,
branch/boolean/loop scopes, name reuse, assignments, captures, and guard clauses.
The formatter, serializer, completion, and `AllJavaFeaturesTest` suites also cover
patterns. Follow the [build and coverage instructions](LAMBDA_EXPRESSIONS.md#regression-tests-and-maintenance)
after changing the grammar or model.
