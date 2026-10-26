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
