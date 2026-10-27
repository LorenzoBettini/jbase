# Blank final local variables

Jbase supports final local variables declared without an initializer. They may
be assigned once when Java's definite-assignment and definite-unassignment
rules are satisfied:

```java
final int value;
if (condition) {
    value = 1;
} else {
    value = 2;
}
System.out.println(value);
```

Jbase reports reads before definite assignment and rejects repeated assignments
or assignments that may run in a loop. The same rules apply to blank final
additional declarators.

## Implementation and tests

[JbaseInitializedVariableFinder](../jbase/src/jbase/validation/JbaseInitializedVariableFinder.java)
tracks definite assignment, while
[JbaseValidator](../jbase/src/jbase/validation/JbaseValidator.java) applies
blank-final assignment checks.
[JbaseXbaseCompiler](../jbase/src/jbase/compiler/JbaseXbaseCompiler.java)
preserves blank final declarations in generated Java.
[JbaseValidatorTest](../jbase.tests/src/jbase/tests/JbaseValidatorTest.java)
covers assignment, branch, read-before-assignment, and loop cases.
[AllJavaFeaturesTest](../jbase.tests/src/jbase/tests/AllJavaFeaturesTest.java)
checks generated code for blank final locals and additional declarators.
