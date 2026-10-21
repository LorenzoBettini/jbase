# Unsupported Java features and known compatibility gaps

Last verified: 2026-10-21.

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
`&=`, `|=`, and `^=` on array elements, including multidimensional accesses.
Receiver and index expressions are evaluated once, the element is read before the
right operand, and implicit narrowing conversions and assignment values are preserved.
Covered by `JbaseArrayCompoundAssignmentTest` and `AllJavaFeaturesTest`.
Shift assignments remain subject to the separate parsing gap below.

### [ ] Shift assignment operators

These fail independently:

```java
int n = 8;
n <<= 1;
n >>= 1;
n >>>= 1;
```

**Current behavior:** Parsing reports `no viable alternative` around `=`, `>=`,
or `>`, with additional recovery diagnostics.

**Expected:** Accept and compile all three standard Java shift assignments.

**Workaround for an `int` local:** `n = n << 1;`, `n = n >> 1;`, or
`n = n >>> 1;`. For other left-hand sides, preserve single evaluation and Java's
implicit narrowing conversion.

**Investigate:** `OpMultiAssign` in [Jbase.xtext](jbase/src/jbase/Jbase.xtext).
The operators are already declared there; investigate tokenization and parsing
rather than assuming the grammar alternatives are simply missing.

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

### [ ] Unary plus on a variable

```java
int n = 1;
int positive = +n;
```

**Current behavior:** Validation resolves an unsuitable binary `operator_plus`
overload, reporting invalid argument count and a `double`-to-`int` mismatch.

**Expected:** Apply Java unary numeric promotion and compile the expression.

**Workaround for this `int` example:** `int positive = n;`.
Unary plus on numeric literals, such as `short positive = +1;`, already works.
For `byte`, `short`, or `char` operands, a replacement must account for promotion
to `int`.

**Investigate:** [PatchedTypeComputer](jbase/src/jbase/typesystem/PatchedTypeComputer.java),
[JbaseExpressionHelper](jbase/src/jbase/util/JbaseExpressionHelper.java), and
operator resolution. The larger showcase expression failed for the same reason;
it was not a general failure of arithmetic or unary minus.

### [ ] Multidimensional array class literals lose dimensions

```java
Class<?> primitiveType = int[][].class;
Class<?> referenceType = String[][][].class;
```

**Current behavior:** Both validate and compile, but generate `int[].class` and
`String[].class`, respectively. Array rank is lost silently.

**Expected:** Preserve every dimension in both type computation and generated Java.
Single-dimensional literals such as `String[].class` already work.

**Workaround when a class object is needed:** `new int[0][0].getClass()` or
`new String[0][0][0].getClass()`. These allocate arrays and are not substitutes for
class literals in annotation values. These workarounds were not separately tested.

**Investigate:** `XJClassObject` and `ArrayBrackets` in
[Jbase.xtext](jbase/src/jbase/Jbase.xtext), `computeJbaseTypes(XJClassObject, ...)` in
[JbaseTypeComputer](jbase/src/jbase/typesystem/JbaseTypeComputer.java), and
`compileClassObject` in
[JbaseXbaseCompiler](jbase/src/jbase/compiler/JbaseXbaseCompiler.java).
Check how repeated dimension values are retained in the model.

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
