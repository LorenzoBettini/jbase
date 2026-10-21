# Java 21 switches

Jbase accepts arrow rules in statements and expressions, grouped constant labels,
`yield` in expression blocks, type patterns with optional `final`, `when` guards,
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

Generated Java retains native switches, arrow rules, type patterns, guards and
`yield`. Expression rules may be emitted as blocks containing `yield`, allowing
Jbase to generate intermediate statements inside the selected branch. Compile
generated pattern switches with Java 21 or later.

A pattern variable is visible in its guard and body, and keeps its declared type.
The selector variable is not narrowed. Different rules can reuse a binding name.
Arrow rules do not fall through. A switch cannot mix arrows and colon groups.
`default` can appear between cases, and its position is preserved during Java
generation.

Switch expressions must be exhaustive. Jbase recognizes a default, a total type
pattern, and complete enum coverage, including grouped enum labels. Pattern
switch statements also require exhaustiveness. A rule block in an expression
must yield or throw; `return`, `break`, and `continue` cannot exit an expression.
Loops and lambdas inside an expression retain their own control-flow boundaries.

Record deconstruction patterns and exhaustiveness over combinations of sealed
subtypes are separate features and are not implemented by this change.

## Implementation and tests

`XJSwitchExpression` and `XJCasePart` extend the Xbase model and reuse its selector,
default, case body and type guard. Jbase adds syntax flags, grouped labels and a
pattern parameter. `XJYieldStatement` uses Xbase's return-type computation inside
an isolated root expectation so yielded values do not become method returns.
Grammar fragments recognize arrow headers before committing to their bodies;
colon cases retain editor recovery for missing labels and delimiters. Xbase's
enum importer supplies unqualified enum constants. Java pattern scopes
are computed independently for each case, without Xbase's selector narrowing.

[JbaseEnhancedSwitchTest](../jbase.tests/src/jbase/tests/JbaseEnhancedSwitchTest.java)
checks Java compilation, execution, scopes, initialization and diagnostics.
`AllJavaFeaturesTest`, formatter and serializer tests cover the surrounding
integration. Follow the [build and coverage instructions](LAMBDA_EXPRESSIONS.md#regression-tests-and-maintenance)
when modifying the grammar or model.
