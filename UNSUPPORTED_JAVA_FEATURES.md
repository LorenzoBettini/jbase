# Unsupported Java features and known compatibility gaps

Last verified: 2026-10-22.

This is an implementation backlog for valid Java expressions and statements that
Jbase currently rejects, crashes on, or translates incorrectly. It records the
gaps found while building `AllJavaFeaturesTest`.
It is not an exhaustive comparison with every Java language feature.

Each example below was checked independently with JbaseTestlanguage's compilation
helper, validation, and the Java 21 compiler. The generated Java was also inspected: successful compilation alone does not
reveal value changes such as the former octal literal bug.
Completed items are checked below. On implementation, add focused regression tests
and update [AllJavaFeaturesTest](jbase.tests/src/jbase/tests/AllJavaFeaturesTest.java).

## Confirmed gaps from the compilation showcase

### [x] Compound assignment to array elements

```java
int[] values = {1};
values[0] += 2;
```

**Implemented:** Native Java generation supports `+=`, `-=`, `*=`, `/=`, `%=`,
`&=`, `|=`, `^=`, `<<=`, `>>=`, and `>>>=` on array elements, including
multidimensional accesses.
Receiver and index expressions are evaluated once, the element is read before the
right operand, and implicit narrowing conversions and assignment values are preserved.
Covered by `JbaseArrayCompoundAssignmentTest`, `JbaseShiftAssignmentTest`, and
`AllJavaFeaturesTest`.

### [x] Shift assignment operators

```java
int n = 8;
n <<= 1;
n >>= 1;
n >>>= 1;
```

**Implemented:** All three operators parse and compile for locals, fields, and
array elements. Native Java generation preserves single evaluation of receivers
and indexes, implicit narrowing conversions, and assignment values. Integral shift
distances include `long`; signed and unsigned right shifts retain Java semantics.

`OpMultiAssign` now matches the lexer's existing tokens, and shift/comparison
lookahead prevents higher-precedence expressions from consuming assignment prefixes.
Ordinary shifts, comparisons, and nested generic type arguments remain supported.

Covered by [JbaseShiftAssignmentTest](jbase.tests/src/jbase/tests/JbaseShiftAssignmentTest.java),
`JbaseParsingTest`, `JbaseSerializerTest`, and `AllJavaFeaturesTest`.

### [x] Binary integer literals

`int bits = 0b1010;` now compiles with value 10. Both `0b` and `0B`, `L`/`l`
suffixes, and internal underscore separators are supported.

### [x] Hexadecimal literals with a long suffix

`long mask = 0xFFL;` now compiles with value 255. Both prefix and suffix cases,
separators, and 32-bit/64-bit boundaries are covered by regression tests.
The inherited Xbase `#L` and `#BI` suffixes remain supported.

### [x] Octal integer literals change value during generation

`int octal = 077;` now generates an equivalent hexadecimal literal with value 63.
Octal `L`/`l` suffixes and separators are supported; invalid octal digits are rejected.
Floating-point literals such as `077.0` retain their decimal meaning.

[JbaseIntegerLiteralTest](jbase.tests/src/jbase/tests/JbaseIntegerLiteralTest.java)
checks generated runtime values, signed boundaries, invalid literals, and serialization
for these three features. They also appear in
[AllJavaFeaturesTest](jbase.tests/src/jbase/tests/AllJavaFeaturesTest.java).

### [x] Unary plus on a variable

```java
int n = 1;
int positive = +n;
```

Unary plus now validates and compiles with Java unary numeric promotion:
`byte`, `short`, and `char` operands promote to `int`; `int`, `long`, `float`, and
`double` retain their types. Boxed numeric operands are unboxed before promotion.
Constant narrowing for numeric literals, such as `short positive = +1;`, still works.

[JbaseNumericExtensions](jbase.lib/src/jbase/lib/JbaseNumericExtensions.java)
provides unary `operator_plus` overloads for normal operator resolution.
[JbaseUnaryPlusTest](jbase.tests/src/jbase/tests/JbaseUnaryPlusTest.java) covers
promotion, unboxing, invalid operands and narrowing, nested arithmetic, and side effects.
Variable operands also appear in
[AllJavaFeaturesTest](jbase.tests/src/jbase/tests/AllJavaFeaturesTest.java).

### [x] Multidimensional array class literals lose dimensions

```java
Class<?> primitiveType = int[][].class;
Class<?> referenceType = String[][][].class;
```

**Implemented:** Every dimension is preserved in type computation, generated Java,
and annotation values. Primitive array literals have primitive component types:
`int[][].class` has type `Class<int[][]>`, while `int.class` retains `Class<Integer>`.

The `XJClassObject.arrayDimensions` model attribute now retains duplicate `[]`
values instead of collapsing them into one dimension.
[JbaseArrayClassLiteralTest](jbase.tests/src/jbase/tests/JbaseArrayClassLiteralTest.java)
covers model dimensions, primitive and reference types, generated Java, runtime
class objects, annotations, and serialization of parsed and edited dimensions.
Both multidimensional examples also appear in `AllJavaFeaturesTest`.

## Forms checked that should not be tracked as unsupported

Method and constructor references are supported with functional interface target
typing, including static, bound and unbound methods, generic type receivers, and
array constructors. Covered by `JbaseMethodReferenceTest`, `JbaseSerializerTest`,
`JbaseContentAssistTest`, and `AllJavaFeaturesTest`.

Lambda parameters support `var` and `final var`, with types inferred from the
functional interface. Covered by `JbaseLambdaTest` and `AllJavaFeaturesTest`:

```java
java.util.function.IntUnaryOperator identity = (var n) -> n;
```

Numeric separators independently validate, preserve the literal in generated Java,
and compile:

```java
int separated = 1_000;
```

Its removal from the initial showcase draft was not evidence of missing support.
By contrast, `077` compiles but changes value, as recorded above.
Also, Java itself does not allow `var` for ordinary method parameters or catch
parameters; those restrictions are not Jbase compatibility gaps.

## Verification when implementing an item

Use a focused regression test that checks diagnostics, the complete generated
Java, and compilation. For silent translation bugs such as array rank loss, also
assert the expected type or runtime behavior as appropriate.

Run the Jbase tests from the repository root:

```sh
./mvnw -B -f jbase.releng/pom.xml verify -pl :jbase.tests -am \
  -DskipMWE2 -DskipXtend \
  -Dtycho.localArtifacts=ignore -Djgit.dirtyWorkingTree=ignore
```

Follow `AGENTS.md` for Maven filesystem access and JaCoCo coverage verification.
