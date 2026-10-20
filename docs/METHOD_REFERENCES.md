# Implementing Java method and constructor references with Xbase

Jbase represents references with `XJMethodReference`, a subtype of Xbase's
`XClosure`. This lets references participate in deferred functional interface
typing when they occur in generic or overloaded method calls. Jbase supplies a
reference-specific matcher and emits native Java `::` expressions; it does not
create a synthetic lambda body.

For the shared closure infrastructure, see
[Lambda expressions](LAMBDA_EXPRESSIONS.md).

## Where the implementation lives

| Source | Responsibility |
| --- | --- |
| [Jbase.xtext](../jbase/src/jbase/Jbase.xtext) | Expression receivers, type receivers, `::`, type arguments, and `new`. |
| [Jbase.ecore](../jbase/model/custom/Jbase.ecore) and [Jbase.genmodel](../jbase/model/custom/Jbase.genmodel) | The hand-maintained reference model and its generated Java implementation. |
| [JbaseTypeComputer](../jbase/src/jbase/typesystem/JbaseTypeComputer.java) | Dispatches the `XClosure` subtype to the reference helper; supplies cast expectations. |
| [JbaseMethodReferences](../jbase/src/jbase/typesystem/JbaseMethodReferences.java) | Functional target constraints, member lookup, overload selection, conversions, and diagnostics. |
| [JbaseValidator](../jbase/src/jbase/validation/JbaseValidator.java) | Separates lambda body/capture rules from reference receiver evaluation. |
| [JbaseInitializedVariableFinder](../jbase/src/jbase/validation/JbaseInitializedVariableFinder.java) | Analyzes receivers in the current definite-assignment state. |
| [JbaseXbaseCompiler](../jbase/src/jbase/compiler/JbaseXbaseCompiler.java) | Prepares receiver expressions and emits Java reference syntax. |
| [JbaseProposalProvider](../jbase.ui/src/jbase/ui/contentassist/JbaseProposalProvider.java) | Completes visible method names and constructor syntax after `::`. |

## Grammar and model

Jbase overrides `XMemberFeatureCall`, so Xbase's use of `::` for static member
access does not define the meaning of Java reference syntax. The Java reference
suffix comes after the receiver's member-call chain:

```java
String::length
Integer::parseInt
System.out::println
receiver()::trim
Collections::<String, Integer>singletonMap
```

A simple name such as `String` or `text` remains an expression during parsing.
Only linking can determine whether it denotes a type or a variable. A separate
`XJTypedMethodReference` rule recognizes unambiguous parameterized and array type
receivers, including parameterized outer types followed by inner types:

```java
ArrayList<String>::new
List<String>::getFirst
int[]::new
String[][]::new
```

The type rules use syntactic predicates to distinguish these forms from normal
expressions. Explicit arguments after `::` are separate from the qualifier's
arguments: in `SomeType<A>::<B>method`, `A` belongs to the receiver type, while
`B` belongs to the referenced executable.

`XJMethodReference` stores:

| Feature | Meaning |
| --- | --- |
| `receiver` | The syntactic expression qualifier for simple names and bound receivers. |
| `type` | The `JvmTypeReference` for a parameterized or array qualifier. |
| `member` | The method name or the keyword `new`. |
| `typeArguments` | Explicit executable arguments written after `::`. |
| `executable` | The chosen `JvmExecutable`, recorded after inference and marked transient. |

The inherited closure body and formal parameter lists are not used to encode an
invocation. In particular, parsing `String::length` does not manufacture a
parameter named `s` and a body `s.length()`.

## Functional interface expectations

`JbaseTypeComputer._computeTypes(XClosure, ...)` checks for `XJMethodReference`
before the ordinary lambda path. `JbaseMethodReferences.computeTypes` examines
each expectation and uses Xbase's `FunctionTypes.findImplementingOperation` to
locate the functional operation. A missing target or a non-interface target
produces `INVALID_METHOD_REFERENCE`. Receiver computation still runs so an
unresolved receiver can report its own diagnostic.

The private `ReferenceTypeHelper` extends `ClosureWithExpectationHelper`.
Its superclass prepares a `FunctionTypeReference` and binds the interface's
generic arguments to Xbase's inference variables. Jbase replaces the body
computation with member matching, return inference, and exception checks.

This keeps references in the same expectation-driven protocol as lambda
arguments. Failed matches accept an incompatible, sealed result; successful
matches accept the functional type with the lambda expectation flag. Those
flags matter to selection of an enclosing overloaded method, not just to
diagnostic reporting.

`state.acceptCandidate` registers the helper. Its `applyToModel` records the
selected executable only when Xbase applies the inference result. It deliberately
does not use the superclass's model update, which would install implicit lambda
parameters and mark a lambda representation as linked.

The subtype also receives the cast handling described in the lambda guide:
`(ToIntFunction<String>) String::length` supplies a target expectation, while
`var length = String::length` supplies none.

## Receiver lookup and invocation shape

`receiverType` computes expression qualifiers without passing them the functional
interface expectation. A type qualifier is recognized from the linking
candidate's `isTypeLiteral` flag, not merely from the Java class of its linked
feature: `this` and `super` must remain instance receivers.

For simple type names, Xbase's actual expression type is `Class<T>`. Computation
unwraps its argument to obtain the member-search type `T`. The compiler likewise
prints the linked `JvmType`, rather than treating that qualifier as a class
object expression.

`OverrideHelper.getResolvedFeatures` supplies inherited members with receiver
type arguments substituted. `collectMatches` then distinguishes the invocation
shapes:

| Reference | Use of the functional descriptor parameters |
| --- | --- |
| `Integer::parseInt` | All parameters are arguments of a static method. |
| `text::trim` | The receiver is already bound; all parameters are method arguments. |
| `String::length` | The first parameter supplies the instance receiver; the rest are method arguments. |
| `ArrayList<String>::new` | All parameters are constructor arguments. |
| `int[]::new` | A single parameter supplies the array length. |

A primitive descriptor parameter cannot serve as an unbound instance receiver:
`Integer::intValue` is not an `IntUnaryOperator`. The receiver check uses strict
reference compatibility instead of silently boxing that first parameter.

Raw instance qualifiers can acquire their generic context from the receiver
parameter. For `Function<List<String>, String> first = List::getFirst`, the
resolved instance method must be viewed through `List<String>`.

## Generic inference and overload selection

`addMatch` checks contextual visibility, explicit executable argument counts and
bounds, and parameter compatibility. `TypeParameterByUnboundSubstitutor`
preserves inferred executable variables; explicit arguments are entered into its
mapping with invariant variance. `DeferredTypeParameterHintCollector` contributes
argument constraints without replacing Xbase's inference machinery.

Executable type arguments are ignored for a non-generic declaration, as in
`String::<Integer>length`. They must have the correct count and bounds when the
declaration is generic.

Three invocation phases are tried in order: fixed arity without boxing, fixed
arity with boxing/unboxing, and expanded varargs. `STRICT` and `LOOSE` disable
Xbase synonym conversions. `invocationCompatible` also preserves Java's
conversion order: boxing `int` to `Integer` and widening to `Number` is allowed;
widening `int` to `long` and then boxing to `Long` is not.

`moreSpecific` compares the effective parameter lists within an invocation phase.
An empty varargs tail retains its component type for comparison, so
`spread(String...)` can be more specific than `spread(Object...)` even for a
zero-argument descriptor. When effective parameter lists tie, a non-generic
declaration is preferred over a generic declaration.

Static and unbound instance searches stay separate. A result from each can be
ambiguous even when one needs boxing and the other does not. For example,
`Function<Integer, String> f = Integer::toString` must not be resolved merely by
preferring the invocation phase of one search.

Input and output inference require additional care:

* In `Comparator.comparing(String::length)`, the reference constrains the
  enclosing invocation's input variable. `collectMatches` contributes that
  constraint to the original target parameter before resolving the helper's
  corresponding variable.
* In `Supplier<List<String>> f = Collections::emptyList`, the target return type
  constrains the referenced method's generic result. When necessary, the result
  is projected onto the expected supertype before collecting paired hints.
* A raw constructor qualifier such as `ArrayList::new` receives unbound class
  arguments so the target can drive constructor result inference.
* Return conversion is checked against the original target descriptor as well
  as contributing synthetic closure hints. Otherwise, inference can conceal an
  invalid conversion such as `Function<String, Long> f = String::length`.

A void descriptor may discard a method result, as in
`Consumer<String> trim = String::trim`. For a value descriptor, the result must
pass the Java invocation conversion check.

## Special members, constructors, and exceptions

Array creation and array `clone` use synthetic `Match` entries with no underlying
`JvmExecutable`. Array constructors require an `int`-compatible length, a
reifiable array type, and no executable type arguments. `List<?>[]::new` is
accepted, whereas `List<String>[]::new` is rejected. Bound and unbound array
`clone` return the same array type; other array members are searched on `Object`.

`referenceReturnType` specializes `getClass` to `Class<? extends |T|>`, using the
qualifying receiver type. Consequently `String::getClass` can satisfy
`Function<String, Class<? extends String>>`, while `Object::getClass` cannot
satisfy that same target merely because its functional parameter is `String`.

Class constructor matching requires a type qualifier, a non-abstract class, and
no wildcard class arguments. Visibility is evaluated in the generated logical
container through `ILogicalContainerProvider` and `ContextualVisibilityHelper`.
Completion shares this visibility helper.

`checkExceptions` compares the selected executable's resolved exceptions with
the functional operation's resolved exceptions. `RuntimeException` and `Error`
subtypes do not require a throwing descriptor. Checked exceptions do: the
surrounding method's ability to throw does not itself make a non-throwing
functional interface suitable for the reference.

## Evaluation, generation, and editor behavior

The `XClosure` inheritance is an inference integration choice, not a declaration
that receiver evaluation takes place in a lambda body. For example:

```java
String text = "first";
java.util.function.Supplier<String> captured = text::trim;
text = "second"; // legal; captured still refers to the first String
```

`JbaseValidator.checkJavaLambda` skips this subtype, and `checkCapturedLocal`
skips reference ancestors when locating an enclosing real lambda. The definite
assignment finder traverses a reference receiver using the current initialized
set, rather than the copy used for a deferred lambda body.

`JbaseXbaseCompiler` intercepts both statement preparation and expression
generation before inherited closure compilation. It prepares the receiver
through the normal expression compiler and prints the qualifier, `::`, optional
type arguments, and member name. `JvmType` appendables and the type reference
serializer preserve import management. Native reference syntax preserves Java's
creation-time receiver evaluation and null check, and the meaning of `super`.

Two generated completion hooks reach the same implementation:
`completeXMemberFeatureCall_Member` for expression qualifiers and
`completeXJTypedMethodReference_Member` for type qualifiers. The callback model
is an `XJMethodReference`, despite the first hook's grammar-rule name. Completion
uses batch-resolved types, deduplicates overloads by name, filters visibility and
bound static access, and proposes `new` for array or non-abstract class type
qualifiers. Inserted method names have no invocation parentheses. These are
member-name proposals; full applicability remains the type computer's job.

## Tests and maintenance

[JbaseMethodReferenceTest](../jbase.tests/src/jbase/tests/JbaseMethodReferenceTest.java)
checks target contexts, Java compilation and execution, resolved executable
selection, generics, overload phases, varargs ambiguity, exceptions, conversions,
receiver evaluation order, and incomplete editor input. Java declarations used
by those tests are in
[MethodReferenceFixtures](../jbase.testlanguage.tests/src/jbase/tests/util/MethodReferenceFixtures.java),
in the bundle visible to the compilation helper's class loader.

[JbaseContentAssistTest](../jbase.ui.tests/src/jbase/ui/tests/JbaseContentAssistTest.java)
checks proposals and insertion, while
[JbaseSerializerTest](../jbase.tests/src/jbase/tests/JbaseSerializerTest.java),
[JbaseInitializedVariableFinderTest](../jbase.tests/src/jbase/tests/JbaseInitializedVariableFinderTest.java),
and [AllJavaFeaturesTest](../jbase.tests/src/jbase/tests/AllJavaFeaturesTest.java)
cover serialization, receiver analysis, and the supported-feature showcase.

Keep the Ecore and GenModel definitions together when changing the model, and
regenerate the derived languages after grammar changes. Generated model and
parser sources are not the implementation source of truth. The
[lambda guide's verification instructions](LAMBDA_EXPRESSIONS.md#regression-tests-and-maintenance)
describe the runtime/UI test and JaCoCo command and its sandbox requirements.
