# Unsupported Java features and known compatibility gaps

Last verified: 2026-10-20.

This is an implementation backlog for valid Java expressions and statements that
Jbase currently rejects, crashes on, or translates incorrectly. It records the
gaps found while building `AllJavaFeaturesTest`, plus two related lambda gaps.
It is not an exhaustive comparison with every Java language feature.

Each example below was checked independently with JbaseTestlanguage's compilation
helper, validation, and the Java 21 compiler. The generated Java was also inspected: successful compilation alone does not
reveal the array class literal and octal literal bugs.
All checklist items remain open. On implementation, add focused regression tests
and update [AllJavaFeaturesTest](jbase.tests/src/jbase/tests/AllJavaFeaturesTest.java).

## Confirmed gaps from the compilation showcase

### [ ] Compound assignment to array elements

```java
int[] values = {1};
values[0] += 2;
```

**Current behavior:** Java generation throws `ClassCastException`, attempting to
cast `XJArrayAccessExpressionImpl` to `XAbstractFeatureCall`.

**Expected:** Compile an array element compound assignment with Java semantics.
Only `+=` was reproduced; check the other compound operators when implementing.

**Workaround for this simple example:** `values[0] = values[0] + 2;`.
A general rewrite must evaluate the array receiver and index only once and preserve
the implicit narrowing conversion performed by Java compound assignment.

**Investigate:** [JbaseXbaseCompiler](jbase/src/jbase/compiler/JbaseXbaseCompiler.java),
especially its inherited compound assignment handling in `FeatureCallCompiler`.

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

### [ ] Binary integer literals

```java
int bits = 0b1010;
```

**Current behavior:** Lexing/parsing reports `mismatched character '1' expecting
set null`.

**Expected:** Accept the literal as decimal value 10.

**Workaround:** Use `10` or `0xA`.

**Investigate:** The numeric literal rules inherited from Xbase and their value
conversion. Check uppercase `0B`, long suffixes, and separators in regression tests;
those variants were not independently checked here.

### [ ] Hexadecimal literals with a long suffix

```java
long mask = 0xFFL;
```

**Current behavior:** `L` is treated as a separate unresolved feature, producing
`The method or field L is undefined` and a missing-semicolon diagnostic.

**Expected:** Accept the literal as long value 255.

**Workaround for this value:** `255L`. Unsuffixed hexadecimal literals such as
`int mask = 0xFF;` work. Larger values require a suitably typed replacement.

**Investigate:** The inherited numeric literal rules and value conversion.
Check lowercase `l` and boundary values when implementing; the reproduced case
uses uppercase `L`.

### [ ] Octal integer literals change value during generation

```java
int octal = 077;
```

**Current behavior:** Validation and compilation succeed, but generated Java is
`int octal = 77;`. The leading zero is dropped without converting the octal value.

**Expected:** Preserve `077` or emit its decimal equivalent `63`.

**Workaround for this value:** Write `63` or `0x3F`.

**Investigate:** Numeric literal value conversion and generation inherited from
Xbase. Add a runtime value assertion to prevent compilable but incorrect output.
Other octal values and long suffixes were not independently checked here.

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

## Additional confirmed lambda gaps

### [ ] Method references

```java
java.util.function.ToIntFunction<String> length = String::length;
```

**Current behavior:** Parsing fails at `::`, with additional recovery diagnostics.
The [README](README.md#java-lambda-expressions) also documents this limitation.

**Expected:** Support Java method references with functional interface target typing.
The reproduced case is an unbound instance method reference; static, bound instance,
and constructor references should be considered in the implementation scope.

**Workaround:** `s -> s.length()` for this example.

**Investigate:** Expression grammar and functional interface type inference.

### [ ] `var` in lambda parameter declarations

```java
java.util.function.IntUnaryOperator identity = (var n) -> n;
```

**Current behavior:** Validation reports `var cannot be resolved to a type` and a
lambda parameter type mismatch.

**Expected:** Infer the lambda parameter type from the functional interface when
Java's `var` parameter syntax is used.

**Workaround:** `n -> n` or `(int n) -> n`.
Local `var` declarations and enhanced-for/resource inference already work.

**Investigate:** `XJLambdaParameter` and `XJLambdaParameterType` in
[Jbase.xtext](jbase/src/jbase/Jbase.xtext), and lambda parameter type inference.

## Forms checked that should not be tracked as unsupported

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
