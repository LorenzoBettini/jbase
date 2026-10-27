# Constant-expression narrowing

Java assignment conversion permits an integral constant expression to narrow to
`byte`, `short`, or `char` when its value is representable by the target type.
Jbase supports this in local variable initializers and assignment right-hand sides:

```java
final int ONE = 1;
final var INFERRED = 3;
byte fromConstant = ONE;
byte fromInferredConstant = INFERRED;
byte fromExpression = 1 + 2;
byte fromUnaryExpression = -(ONE + 2);
char letter = 65 + 1;

byte assigned;
assigned = 1 + 2;
```

Final local constants of type `byte`, `short`, `char`, or `int` may be used in
these expressions, including a `final var` whose inferred type is one of those
types. Integral unary operators, arithmetic, shifts, and bitwise operators are
evaluated for the range check. A `long` expression is not eligible, even when its
value would fit:

```java
byte invalid = 1L;
```

In the applicable assignment contexts Java also permits narrowing followed by
boxing:

```java
Byte boxedByte = 1;
Byte boxedConstant = ONE;
Short boxedShort = 1 + 2;
Character boxedCharacter = 65 + 1;

Byte assignedBox;
assignedBox = ONE;
```

Method invocation conversion is distinct from assignment conversion, so a
constant expression is not narrowed to match a method parameter.

## Implementation and tests

[JbaseTypeComputer](../jbase/src/jbase/typesystem/JbaseTypeComputer.java)
evaluates integral constant expressions in assignment contexts and checks
representability. [JbaseValidator](../jbase/src/jbase/validation/JbaseValidator.java)
validates the resulting assignment conversions, and
[JbaseXbaseCompiler](../jbase/src/jbase/compiler/JbaseXbaseCompiler.java)
preserves narrowing followed by boxing in generated Java.
[JbaseTypeComputerTest](../jbase.tests/src/jbase/tests/JbaseTypeComputerTest.java)
checks inferred expression types and range boundaries.
[JbaseValidatorTest](../jbase.tests/src/jbase/tests/JbaseValidatorTest.java)
covers valid and invalid narrowing, boxing, integral operators, and the
distinction from method-invocation conversion.
