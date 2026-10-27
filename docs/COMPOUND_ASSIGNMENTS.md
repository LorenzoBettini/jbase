# Compound assignments

Jbase supports Java compound assignments on locals, fields, and array elements.
The operation uses the left operand's assignment type, including Java's implicit
narrowing for primitive numeric types such as `byte`, `short`, and `char`:

```java
byte value = 127;
value += 1; // -128
short count = 32767;
count += 1; // -32768
char letter = 65535;
letter += 1; // '\u0000'
```

Arithmetic, bitwise, shift, boolean, and string compound assignments retain
their native Java behavior. Native assignment syntax also preserves evaluation
order and evaluates array receivers and indices only once. Regression coverage
is in `JbaseCompoundAssignmentTest` and `JbaseArrayCompoundAssignmentTest`.
