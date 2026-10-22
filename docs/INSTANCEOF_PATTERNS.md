# Java instanceof patterns and parameterized tests

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

## Parameterized tests

Jbase accepts tests against parameterized types when the operand's static generic
constraints make the conversion checkable, with or without a binding:

```java
java.util.List<String> values = new java.util.ArrayList<String>();
if (values instanceof java.util.ArrayList<String> list) {
    System.out.println(list);
}
boolean arrayList = values instanceof java.util.ArrayList<String>;
```

This also supports checked conversions involving generic arrays, bounded
wildcards, nested type arguments and parameterized inner types. The generated
Java retains the tested type arguments. Tests requiring an unchecked conversion
remain rejected: an `Object`, raw `List`, or `List<?>` cannot be tested against
`ArrayList<String>`. Incompatible arguments and unconstrained extra type
parameters are also rejected.

## Record patterns

Jbase can deconstruct externally declared Java records, including nested and
generic records and records with no components. For records `Point(int x, int y)`
and `Segment(Point start, Point end)`:

```java
Object value = new Segment(new Point(1, 2), new Point(3, 4));
if (value instanceof Segment(Point(int x, var y), Point(var a, int b))) {
    System.out.println(x + y + a + b);
}
```

Components accept explicit types or inferred `var` bindings. Omitted generic
record arguments are inferred from the operand's static type, including generic
interface constraints. Bindings follow the same flow-sensitive scope rules as
type patterns, including negation, short-circuit operators, guard clauses and
loops. Bindings may be assigned unless declared `final`; `final` applies to a
component binding, not an entire record pattern.

Jbase validates record types, component arity, primitive compatibility, unchecked
conversions and duplicate bindings. Primitive component patterns must use the
component's exact primitive type. Generated Java retains native record patterns,
preserving component accessor evaluation, null matching and `MatchException`
behavior when an accessor throws. A null record value does not match, including
in a nested record pattern.

Record patterns also work in [switch statements and expressions](ENHANCED_SWITCH.md#record-patterns),
with guards and structural exhaustiveness checks.

## Implementation

* [Jbase.xtext](../jbase/src/jbase/Jbase.xtext) overrides relational expressions.
  `XJInstanceOfExpression` extends Xbase's ordinary instance-of node and adds an
  optional binding, a `final` flag and an optional `XJRecordPattern`. A type-pattern
  binding is a `JvmFormalParameter`; its type is assigned from the enclosing
  expression's checked type. `XJComponentPattern` models each record component.
* [JbaseRecordPatterns](../jbase/src/jbase/typesystem/JbaseRecordPatterns.java)
  infers generic record arguments, assigns component types and collects nested
  bindings. Switch validation also uses its coverage and dominance checks.
  Unresolved pattern types have no component fields; bindings receive unknown
  expected types during error recovery so serialization can preserve unresolved patterns.
* [JbasePatternVariables](../jbase/src/jbase/typesystem/JbasePatternVariables.java)
  computes definitely-matched bindings and isolated type-computation scopes.
  [JbaseTypeComputer](../jbase/src/jbase/typesystem/JbaseTypeComputer.java)
  invokes it before child computation and after statements in a block.
* [JbaseValidator](../jbase/src/jbase/validation/JbaseValidator.java) checks
  duplicate bindings across boolean/conditional operands, final assignment and
  record compatibility. Its `checkInstanceOf` override uses
  [JbaseInstanceOfTypes](../jbase/src/jbase/typesystem/JbaseInstanceOfTypes.java)
  to validate checked parameterized conversions using the operand's static type.
* [JbaseXbaseCompiler](../jbase/src/jbase/compiler/JbaseXbaseCompiler.java) emits
  native patterns. Conditions and pattern-dependent ternaries remain expressions;
  saving a match result in a temporary boolean would destroy Java's flow scope.
  For loops with pattern conditions likewise retain their native form.
* The serializer uses the overridden relational grammar and generic sequencing
  for pattern-containing binary expressions. Formatting and completion use the
  same model and scopes.

The scope rules follow [JLS 6.3](https://docs.oracle.com/javase/specs/jls/se21/html/jls-6.html#jls-6.3).

[JbaseInstanceOfPatternTest](../jbase.tests/src/jbase/tests/JbaseInstanceOfPatternTest.java)
checks validation, native Java generation, Java 21 compilation, execution,
branch/boolean/loop scopes, name reuse, assignments, captures, and guard clauses.
It also checks parameterized tests with full Java snapshots, compilation,
execution and diagnostics for unchecked or incompatible conversions.
[JbaseRecordPatternTest](../jbase.tests/src/jbase/tests/JbaseRecordPatternTest.java)
covers nested records, generic inference, component scopes, native accessor
behavior and invalid patterns.
The formatter, serializer, completion, and `AllJavaFeaturesTest` suites also cover
patterns. Follow the [build and coverage instructions](LAMBDA_EXPRESSIONS.md#regression-tests-and-maintenance)
after changing the grammar or model.
