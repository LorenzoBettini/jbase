# Labeled statements, `break`, and `continue`

Jbase supports Java labeled statements and optional label names on `break` and
`continue`:

```java
exit: {
    break exit;
}

outer: for (int i = 0; i < 2; i++) {
    while (true) continue outer;
}
```

A labeled `continue` must target a labeled loop. Branches cannot cross a lambda
or anonymous-class boundary, and a branch cannot jump out of a switch expression.
When a basic `for` loop is lowered to a `while`, Jbase keeps its label on the loop
and performs the update expressions before a labeled `continue`.

The grammar models a labeled statement separately and stores optional branch labels.
The validator checks label targets, while the compiler and formatter preserve the
native Java syntax.

## Implementation and tests

[Jbase.xtext](../jbase/src/jbase/Jbase.xtext) defines labeled statements and
optional `break`/`continue` labels.
[JbaseValidator](../jbase/src/jbase/validation/JbaseValidator.java) checks
branch targets and Java's label boundaries.
[JbaseBreakStatementDetector](../jbase/src/jbase/controlflow/JbaseBreakStatementDetector.java)
and [JbaseBranchingStatementDetector](../jbase/src/jbase/controlflow/JbaseBranchingStatementDetector.java)
account for labels in control-flow analysis.
[JbaseXbaseCompiler](../jbase/src/jbase/compiler/JbaseXbaseCompiler.java)
preserves labels when lowering basic `for` loops.
[JbaseLabeledStatementTest](../jbase.tests/src/jbase/tests/JbaseLabeledStatementTest.java)
checks parsing and validation, while
[JbaseLabeledStatementCompilerTest](../jbase.tests/src/jbase/tests/JbaseLabeledStatementCompilerTest.java)
checks generated control flow and labeled `continue` behavior.
