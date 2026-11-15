# Assignment targets

A variable can be parenthesized on the left side of a simple assignment:

```java
int n = 0;
(n) = 1;
```

`Jbase.xtext` creates an `XJAssignment` with its `parenthesized` flag set.
`Jbase.ecore` declares the flag; `Jbase.genmodel` generates the corresponding
model accessors. `JbaseTypeComputer` delegates its typing to Xbase, and
`JbaseXbaseCompiler.compileParenthesizedAssignment` preserves the parentheses
in generated Java. Its assignment dispatcher also delegates ordinary variable
and field targets to `compileVariableAssignment`.
`JbaseFormatter` removes spaces inside the parentheses.
`JbaseParenthesizedAssignmentTest` covers parsing, invalid value types,
compilation, and runtime behavior. `JbaseFormatterTest` covers spacing,
`JbaseSerializerTest` checks serialization with and without a node model, and
`AllJavaFeaturesTest` includes the form in its compilation showcase.

## Array elements

A simple assignment can target an array element returned by a method call:

```java
java.util.List<int[]> arrays = java.util.List.of(new int[] {1});
arrays.get(0)[0] = 2;
```

The same syntax also permits `(array)[0] = 2` and `((int[]) object)[0] = 2`.

`Jbase.xtext` parses the postfix array access as the assignment's `assignable`
expression. `JbaseTypeComputer` computes the array element type, checks the
right-hand value against it, and gives the assignment expression that type.
`JbaseXbaseCompiler.compileArrayAssignment` emits the array access directly in
Java, preserving receiver and index evaluation.

`JbaseArrayCompoundAssignmentTest` covers parsing, invalid value types,
compilation, and runtime behavior. `JbaseSerializerTest` checks serialization
with and without a node model, and `AllJavaFeaturesTest` includes the form in
its compilation showcase.
