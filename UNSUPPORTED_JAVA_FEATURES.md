# Unsupported Java features and known compatibility gaps

Last verified: 2026-10-22.

Jbase does **not** currently support the complete Java 21 expression and statement
language. This backlog includes syntax, validation, and generation gaps found by
comparing the grammar and implementation with Java SE 21, not just the features
used by `AllJavaFeaturesTest`.

The audit covers the permanent expression and block/statement language in
[JLS chapter 14](https://docs.oracle.com/javase/specs/jls/se21/html/jls-14.html) and
[JLS chapter 15](https://docs.oracle.com/javase/specs/jls/se21/html/jls-15.html),
including relevant [lexical forms](https://docs.oracle.com/javase/specs/jls/se21/html/jls-3.html).
Top-level compilation units, modules, and general class/member declaration syntax
are outside this scope. Local types and anonymous class expressions are included.
Java 21 preview features are listed separately below.

Suspected gaps were checked with independent JbaseTestlanguage parsing and
compilation-helper probes and corresponding sources compiled by JDK 21 `javac`.
The permanent-feature examples compile without preview enabled. Examples that use
records, sealed hierarchies, annotations, or inner classes assume externally
compiled Java fixtures, so their expression support is tested independently of
Jbase's ability to declare those types. Recovered invalid models can crash the
compiler; such crashes are recorded as failures, not successful language support.
Generated Java was also inspected where relevant, since compilation alone does
not establish that values and evaluation behavior are preserved.

This is a confirmed backlog, not a proof of conformance for every combination of
Java typing, overload resolution, control flow, and run-time semantics. Existing
positive tests remain evidence of the supported forms listed below.
Completed items are checked. On implementation, add focused regression tests and
update [AllJavaFeaturesTest](jbase.tests/src/jbase/tests/AllJavaFeaturesTest.java).

## Open gaps from the Java 21 audit

### [x] Record patterns in `instanceof` and `switch`

Assume an externally declared Java record `Point(int x, int y)`:

```java
Object value = new Point(1, 2);
if (value instanceof Point(int x, int y)) System.out.println(x + y);
int sum = switch (value) {
    case Point(var x, var y) -> x + y;
    default -> 0;
};
```

**Implemented:** Recursive record patterns support explicit and inferred `var`
components, generic records (including inference through generic interfaces), empty
records, final and mutable bindings, flow-sensitive scopes, guarded rules and colon
switch groups. Component arity, record types, primitive compatibility, unchecked
conversions, duplicate bindings, dominance and record-pattern coverage are validated.
Native Java preserves null matching, component accessor evaluation and `MatchException`
behavior. Sealed-hierarchy exhaustiveness is also implemented, as described below.
Covered by `JbaseRecordPatternTest`, `JbaseSerializerTest` and `AllJavaFeaturesTest`,
including full Java snapshots, compilation, execution and serialization without a
node model. The runtime/UI JaCoCo aggregate has 100% instruction, branch and line coverage.

### [x] Exhaustiveness over sealed hierarchies

Assume these externally declared Java types:

```java
sealed interface Shape permits Circle, Square {}
record Circle(int radius) implements Shape {}
record Square(int side) implements Shape {}
```

```java
Shape shape = new Circle(1);
int size = switch (shape) {
    case Circle c -> c.radius();
    case Square s -> s.side();
};
```

**Implemented:** Exhaustiveness follows sealed `permits` hierarchies recursively,
including nested sealed types, concrete sealed classes, generic hierarchies, and enum
leaves. Unconditional type and record patterns contribute to coverage; a literal
`true` guard is also unconditional, while other guarded patterns do not establish
coverage. Incomplete or unresolved permitted-type coverage remains non-exhaustive.
Covered by `JbaseRecordPatternTest`, `JbaseEnhancedSwitchTest`, and
`AllJavaFeaturesTest`.

### [x] Pattern switches on some boxed selector types

```java
Long value = 1L;
int result = switch (value) {
    case Long number -> number.intValue();
};
```

**Implemented:** `Long`, `Boolean`, `Float`, and `Double` remain reference selector
types during validation. Type patterns, guards, null labels, arrow rules and colon
groups compile and execute with native Java switch syntax. Primitive `long`,
`boolean`, `float`, and `double` selectors remain rejected for Java 21.
Covered by `JbaseEnhancedSwitchTest` and `AllJavaFeaturesTest`; the changed validator
method has 100% instruction, branch and line coverage.

### [x] Legal `instanceof` checks against parameterized types

```java
java.util.List<String> values = new java.util.ArrayList<String>();
if (values instanceof java.util.ArrayList<String> list) System.out.println(list);
```

**Implemented:** Checked narrowing uses the operand's static generic constraints.
Native Java preserves parameterized tests with and without bindings, generic arrays,
bounded wildcards, nested arguments and parameterized inner types. Raw operands,
unconstrained extra type parameters, incompatible arguments and unchecked narrowing
from `Object` or wildcard operands remain rejected. Regression tests compare the
complete generated Java, compile it and execute representative matches.
Covered by `JbaseInstanceOfPatternTest` and `AllJavaFeaturesTest`; the new type utility
and validator override have 100% instruction, branch and line coverage.

### [x] `assert` statements

```java
assert true;
assert true : "message";
```

**Implemented:** Assertions with optional detail expressions are parsed,
type-checked, formatted, serialized, and generated as native Java statements.
The condition must be boolean and the detail expression must not be void.
Runtime assertion status follows Java's `-ea` behavior. Covered by
`JbaseAssertStatementTest`, `JbaseFormatterTest`, `JbaseSerializerTest`, and
`AllJavaFeaturesTest`; see the [assert statement guide](docs/ASSERT_STATEMENTS.md).

### [x] Labeled statements, `break`, and `continue`

```java
exit: { break exit; }
outer: for (int i = 0; i < 2; i++) {
    for (;;) { continue outer; }
}
```

**Implemented:** Labeled statements and optional labels on `break` and `continue`
are parsed and compiled. `continue` labels must target loops, branches cannot jump
across lambda boundaries or out of switch expressions, and labeled continues preserve
updates when a basic `for` is lowered to a `while`. Covered by
`JbaseLabeledStatementTest`, `JbaseLabeledStatementCompilerTest`,
`JbaseBreakStatementDetectorTest`, `JbaseBranchingStatementDetectorTest`, and
`AllJavaFeaturesTest`; see the [labeled statements guide](docs/LABELED_STATEMENTS.md).

### [x] Multi-catch

```java
try {
    throw new java.io.IOException();
} catch (java.io.IOException | IllegalArgumentException ex) {
    ex.getMessage();
}
```

`XCatchClause` represents the alternatives with `JvmSynonymTypeReference`. The
compiler emits a type check for each alternative and resolves the catch variable
to their common supertype. Multi-catch is supported with and without
try-with-resources.

### [x] Existing-variable resources in `try`-with-resources

```java
var input = new java.io.ByteArrayInputStream(new byte[0]);
try (input) {}
```

**Implemented:** Existing final or effectively final local variables and parameters,
as well as final fields, can be used as resources when they implement
`AutoCloseable`. Existing-variable resources can be mixed with resource declarations
and retain source order. Covered by `JbaseParsingTest`, `JbaseFormatterTest`,
`JbaseVarTest`, and `JbaseCompilerTest`; see the
[try-with-resources guide](docs/TRY_WITH_RESOURCES.md).

### [x] Annotations on local variables, parameters, and type uses

```java
@A final String value = "x";
java.util.function.Consumer<String> consumer = (@A var s) -> {};
for (@A String s : new String[0]) {}
try { throw new java.io.IOException(); } catch (@A java.io.IOException ex) {}
java.util.List<@A String> values = null;
String cast = (@A String) value;
Object instance = new @A String("x");
Object array = new String @A [1];
```

Local declarations, method and lambda parameters, enhanced-for variables,
try-with-resources variables, and catch parameters retain declaration annotations.
Type-use annotations are preserved in parameterized, wildcard, array, cast,
constructor, and nested types. The annotation type must be available from the
embedding Java environment and applicable to the annotated element. Covered by
`JbaseCompilerTest`, `JbaseFormatterTest`, `JbaseSerializerTest`, and
`AllJavaFeaturesTest`; see the [annotations guide](docs/ANNOTATIONS.md).

### [x] Anonymous class creation

Solved with an extensible anonymous-class expression and an empty default body.
Jbase handles anonymous type inference (including `var` retaining added fields
and methods), constructor resolution, validation, captures, and Java generation.

Member syntax remains the responsibility of the embedding language. The test
language demonstrates nonempty bodies with its existing properties and methods:

```java
var point = new Object() {
	x: int
	y: int
	method getX(): int { return x; }
	method getY(): int { return y; }
};
point.x = 30;
point.y = 40;
System.out.println(point.getX() + ", " + point.getY());
```

See [Anonymous classes](docs/ANONYMOUS_CLASSES.md) for the grammar and inferrer
extension contract. General Java member declaration syntax remains outside Jbase.

### [x] Qualified inner-class creation

Assume an external Java class `Outer` with a public non-static member class `Inner`:

```java
Outer outer = new Outer();
Outer.Inner inner = outer.new Inner();
```

Jbase resolves the member-class constructor from the qualifier's type and emits
Java's qualified creation syntax. Ordinary constructors and qualified names of
static nested classes continue to use their existing syntax. See the
[qualified inner-class creation guide](docs/QUALIFIED_INNER_CLASS_CREATION.md).

### [x] Explicit method and constructor type arguments

```java
java.util.List<String> values = java.util.Collections.<String>emptyList();
Object value = new <String>GenericConstructor("x");
```

Method invocations and constructor invocations accept explicit executable type
arguments in Java's source positions. Constructor executable arguments remain
separate from class type arguments after the created type name, including when
both lists are present. Existing class type arguments and diamond inference are
unchanged. Explicit type arguments on method references are also supported.
Covered by parsing, serialization, formatting, compilation and the
`AllJavaFeaturesTest` showcase; see the
[explicit executable type arguments guide](docs/EXPLICIT_EXECUTABLE_TYPE_ARGUMENTS.md).

### [x] Intersection casts

```java
Object task = (Runnable & java.io.Serializable) () -> {};
```

Intersection cast types are represented with Xtext's `JvmMultiTypeReference`.
Lambda and method-reference targets use the unique functional-interface bound for
inference, while generated Java retains the complete intersection cast.

### [ ] Array declarator dimensions and trailing initializer commas

These independent forms fail:

```java
int values[] = {1};
int scalar = 1, more[] = {2};
java.util.function.Consumer<String[]> consumer = (String items[]) -> {};
int[] withComma = {1, 2,};
int[] constructed = new int[] {1, 2,};
```

Dimensions must currently appear on the type rather than after the declarator
name, and `XJArrayLiteral` requires an element after each comma. Java allows both
array declarator styles and an optional trailing initializer comma.

### [ ] Some legal assignment targets

```java
int n = 0;
(n) = 1;
java.util.List<int[]> arrays = java.util.List.of(new int[] {1});
arrays.get(0)[0] = 2;
Object array = new int[] {1};
((int[]) array)[0] = 2;
```

These simple assignments have syntax errors. The assignment grammar handles named
variables, fields, and array accesses rooted in its named-feature form, but not all
Java variable expressions. Even `(array)[0] = 2` fails when `array` has an array
type. Ordinary `values[0] = 2`, multidimensional named-array assignment, and
parenthesized compound assignment or increment such as `(n) += 1` and `(n)++` pass.

### [ ] Blank final local variables

```java
final int value;
value = 1;
System.out.println(value);
```

Jbase reports both `Value must be initialized` and `Assignment to final variable`.
Java permits a blank final local to be assigned once when definite assignment and
unassignment rules are satisfied.

### [ ] Constant-expression narrowing and narrowing followed by boxing

```java
final int ONE = 1;
byte first = ONE;
byte second = 1 + 2;
char letter = 65 + 1;
Byte boxed = 1Aeport type mismatches. Java assignment conversion allows representable
constant `int` expressions to narrow to `byte`, `short`, or `char`, and permits
narrowing followed by boxing to `Byte`, `Short`, or `Character` in the applicable
cases. Simple primitive literal narrowing such as `byte b = 1` and `char c = 65`
already works.

### [ ] Implicit narrowing in ordinary compound assignments

```java
byte b = 1;
b += 2;
b *= 2;
b |= 2;
short s = 1;
s += 2;
char c = 65;
c += 1;
```

Jbase reports type mismatches after numeric promotion. Java compound assignment
implicitly converts the result back to the left operand's type. The completed
array-element and shift-assignment items below do not cover these ordinary
compound assignments to narrow numeric locals.

### [ ] Additional Java floating-point literal forms

```java
double hexadecimal = 0x1.8p1;
double integralSignificand = 0x1p2;
double leadingDot = .5;
double trailingDot = 1.;
double trailingDotExponent = 1.e2;
```

These valid Java literal forms are not recognized correctly. Java's hexadecimal
floating-point syntax is distinct from hexadecimal integer literals. Decimal
literals with digits on both sides of the point and signed decimal exponents,
such as `1e+2`, are supported.

### [ ] Java escape sequences and Unicode lexical translation

```java
String space = "\s";
String octal = "\141";
char letter = '\141';
char unicode = '\uuuu0061';
int café = 1;
int v\u0061lue = 1;
```

Ordinary string and character converters reject Java's space and octal escapes;
the multi-`u` Unicode escape also fails. Generation after converter errors can drop
the backslash and change the value or emit an invalid character literal. Text-block
escape handling is separate and already has focused tests.

The inherited identifier lexer also rejects the shown non-ASCII identifier, and
Unicode escapes are not translated throughout the source before tokenization as
required by Java. `$` in identifiers is supported; these failures do not imply
that every non-alphanumeric identifier is rejected.

## Java features that are not related to expressions and statements

### [ ] Local type declarations

Each of these independently valid block contents is rejected:

```java
class Local {}
enum Choice { ONE }
record Item(int value) {}
interface Marker {}
```

Local declarations belong to Java's block language even though they are not
statements in the narrow JLS grammar.

Jbase, being only a Java expression and statement language, does not support local types. 

## Java 21 preview expression and statement features

These are separate from the permanent Java 21 language baseline. Oracle's
[Java 21 language changes](https://docs.oracle.com/en/java/javase/21/language/java-language-changes-release.html)
identify string templates and unnamed patterns and variables as preview features.
The Java comparison probes for this section used JDK 21 with `--enable-preview --release 21`.

### [ ] String templates

```java
String value = STR."answer \{1 + 2}";
```

The processor/template syntax is rejected. Ordinary strings and text blocks are
supported independently.

### [ ] Full unnamed-pattern and unnamed-variable semantics

Assuming an external record `Point(int x, int y)`:

```java
Object value = new Point(1, 2);
if (value instanceof Point(int x, _)) System.out.println(x);
```

Unnamed component patterns such as the bare `_` above are not parsed. Named record
patterns are supported. A single unused `_` local or lambda parameter can compile because Jbase renames it to an
ordinary Java identifier. That alone does not implement the preview feature:
multiple unnamed declarations in one scope and repeated unnamed lambda parameters
must be allowed, and reading `_` must be forbidden. Jbase rejects the repeated
forms as duplicate variables and accepts `int _ = 1; System.out.println(_);`,
which the Java 21 preview compiler rejects. An unnamed type binding such as
`value instanceof String _` also fails generated-Java compilation.

## Completed gaps from the compilation showcase

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

The following modern Java forms already have positive compilation tests:

| Feature | Evidence |
| --- | --- |
| Local `var`, including enhanced `for` and resource declarations | `JbaseVarTest`, `JbaseCompilerTest`, `AllJavaFeaturesTest` |
| Text blocks | `JbaseTextBlockTest`, `AllJavaFeaturesTest` |
| `instanceof` type patterns, checked parameterized tests and flow-sensitive binding scopes | `JbaseInstanceOfPatternTest` |
| Record patterns in `instanceof` and `switch`, including nested and inferred components | `JbaseRecordPatternTest`, `AllJavaFeaturesTest` |
| Switch expressions, arrow rules, grouped constants, `yield`, type patterns, `when`, and null labels | `JbaseEnhancedSwitchTest`, `AllJavaFeaturesTest` |

The audit also independently compiled qualified enum labels such as
`case Thread.State.NEW -> 1` with both an enum selector and an `Object` selector.
These Java 21 forms work; the switch gaps above concern other cases.

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
Octal literals such as `077` now also preserve their Java values, as recorded above.
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
