# Java 21 switches

Jbase accepts arrow rules in statements and expressions, grouped constant labels,
`yield` in expression blocks, type patterns with optional `final`, record patterns, `when` guards,
`case null`, and `case null, default`. Colon-style statements keep their existing
fall-through behavior. Colon-style expressions can return values with `yield`.

```java
Object value = "hello";
int length = switch (value) {
    case String text when !text.isEmpty() -> text.length();
    case Integer number -> {
        int result = number.intValue();
        yield result;
    }
    case null, default -> 0;
};
switch (length) {
    case 0, 1 -> System.out.println("short");
    default -> System.out.println(length);
}
```

Generated Java retains native switches, arrow rules, type and record patterns, guards and
`yield`. Expression rules may be emitted as blocks containing `yield`, allowing
Jbase to generate intermediate statements inside the selected branch. Compile
generated pattern switches with Java 21 or later.

A pattern variable is visible in its guard and body, and keeps its declared type.
The selector variable is not narrowed. Different rules can reuse a binding name.
Arrow rules do not fall through. A switch cannot mix arrows and colon groups.
`default` can appear between cases, and its position is preserved during Java
generation.

Switch expressions must be exhaustive. Jbase recognizes a default, a total type
pattern, a record pattern covering every component type, and complete enum coverage,
including grouped enum labels. Record coverage is checked recursively for nested
patterns. Pattern switch statements also require exhaustiveness. A rule block in an expression
must yield or throw; `return`, `break`, and `continue` cannot exit an expression.
Loops and lambdas inside an expression retain their own control-flow boundaries.

Exhaustiveness over permitted subclasses of sealed types remains unsupported;
add a `default` when switching over such a hierarchy.

## Record patterns

Record patterns can deconstruct externally declared Java records in arrow rules
and colon groups. For a record `Point(int x, int y)`:

```java
Object value = new Point(1, 2);
int sum = switch (value) {
    case Point(var x, int y) when x > 0 -> x + y;
    default -> 0;
};
```

Components can use explicit types, inferred `var` bindings, or nested record
patterns. Generic record arguments can be inferred from the selector's static
type. For a record `Box<T>(T value)` and a `Box<String>` selector, this expression
is exhaustive and `text` has type `String`:

```java
String trimmed = switch (box) {
    case Box(var text) -> text.trim();
};
```

Component bindings are visible in the guard and body. They can be mutable or
declared `final`; `final` cannot qualify an entire record pattern. Jbase checks
record types, component arity and compatibility, unchecked conversions, duplicate
bindings, dominance and structural coverage. A partial pattern such as
`Box(Point(var x, var y))` does not cover every value of `Box<Object>`.

Generated Java preserves native record matching, including accessor evaluation,
null behavior and `MatchException` when an accessor throws. See
[instanceof patterns](INSTANCEOF_PATTERNS.md#record-patterns) for nested examples
and flow-sensitive component scopes.

## Boxed selectors

`Long`, `Boolean`, `Float`, and `Double` are supported reference selector types
for pattern switches, with guards, null labels, arrow rules and colon groups:

```java
Long value = 1L;
int result = switch (value) {
    case null -> 0;
    case Long number -> number.intValue();
};
```

Their primitive counterparts `long`, `boolean`, `float`, and `double` remain
invalid selector types in Java 21.

## Implementation and tests

`XJSwitchExpression` and `XJCasePart` extend the Xbase model and reuse its selector,
default, case body and type guard. Jbase adds syntax flags, grouped labels and a
pattern parameter. `XJRecordPattern` and `XJComponentPattern` represent recursive
deconstruction; `JbaseRecordPatterns` assigns component types and checks record
coverage and dominance. `XJYieldStatement` uses Xbase's return-type computation inside
an isolated root expectation so yielded values do not become method returns.
Grammar fragments recognize arrow headers before committing to their bodies;
colon cases retain editor recovery for missing labels and delimiters. Xbase's
enum importer supplies unqualified enum constants. Java pattern scopes
are computed independently for each case, without Xbase's selector narrowing.

[JbaseEnhancedSwitchTest](../jbase.tests/src/jbase/tests/JbaseEnhancedSwitchTest.java)
checks Java compilation, execution, scopes, initialization and diagnostics.
[JbaseRecordPatternTest](../jbase.tests/src/jbase/tests/JbaseRecordPatternTest.java)
checks nested and generic record patterns, inference, exhaustiveness, colon groups,
native accessor behavior and invalid patterns. Boxed selectors are covered by
both suites, including switch result conversions.
`AllJavaFeaturesTest`, formatter and serializer tests cover the surrounding
integration. Follow the [build and coverage instructions](LAMBDA_EXPRESSIONS.md#regression-tests-and-maintenance)
when modifying the grammar or model.
