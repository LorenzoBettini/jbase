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
