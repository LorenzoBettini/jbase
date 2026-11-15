# Array assignment targets

A simple assignment can target an array element returned by a method call:

```java
java.util.List<int[]> arrays = java.util.List.of(new int[] {1});
arrays.get(0)[0] = 2;
```

The same syntax also permits `(array)[0] = 2` and `((int[]) object)[0] = 2`.

`Jbase.xtext` parses the postfix array access as the assignment's `assignable`
expression. `JbaseTypeComputer` computes the array element type, checks the
right-hand value against it, and gives the assignment expression that type.
`JbaseXbaseCompiler` emits the
array access directly in Java, preserving receiver and index evaluation.

`JbaseArrayCompoundAssignmentTest` covers parsing, invalid value types,
compilation, and runtime behavior. `JbaseSerializerTest` checks serialization
with and without a node model, and `AllJavaFeaturesTest` includes the form in
its compilation showcase.
