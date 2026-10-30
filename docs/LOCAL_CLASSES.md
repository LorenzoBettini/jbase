# Local classes

Jbase supplies reusable JVM support for named local classes. Declaration and
member syntax belong to the embedding language; plain Jbase introduces no class
keyword or declaration rule. Local enums, records and interfaces are separate
features and are not implemented by this support.

The test language and Domainmodel opt in with `local` declarations, an optional
`extends` clause and optional `implements` interfaces. Their existing anonymous
body rules supply the members. For example, Domainmodel accepts:

```java
entity Example {
    op calculate(int input):int {
        final var offset = 2;
        local Counter implements java.util.function.IntSupplier {
            value : int
            op getAsInt():int { return this.value + input + offset; }
        }
        Counter counter = new Counter();
        counter.setValue(37);
        return counter.getAsInt();
    }
}
```

Calling `calculate(3)` returns `42`. Properties have the same private fields and
public accessors as entity properties; explicit accessors replace generated ones.
The `local` keyword is deliberately different from Java's `class`; generated Java
still uses `class`. It is reserved in these hosts; use another identifier name
(or escape it as `^local`) for variables and methods. Domainmodel entities also
accept an optional `implements` clause after `extends`, including multiple
interfaces:

```java
entity Values extends java.util.ArrayList<String>
    implements java.util.function.IntSupplier, java.lang.Cloneable {
    op getAsInt():int { return size(); }
}
```

Entity inference uses the same `superClass` and `addSuperInterface` builder APIs
as local classes. Entity properties and operations may implement interface members,
and missing implementations are reported by `JvmGenericTypeValidator`.

The test language uses `property value: int` and `method getAsInt(): int { ... }`
instead of Domainmodel's property and operation syntax.

## Integrating a host language

Keep the normal Jbase grammar, generator fragment and runtime integration
explained in [Anonymous classes](ANONYMOUS_CLASSES.md). Domainmodel is a complete
reference implementation for the additional steps below.

### Grammar and metamodel

The reusable, abstract `XJLocalClass` model extends `XExpression` and contains a
`name` and an `XJAnonymousClassBody` named `body`. It has no Jbase parser rule.
Extend the block-content rule in the host grammar with a concrete subtype:

```xtext
import "http://www.eclipse.org/xtext/xbase/Xbase" as xbase
import "http://www.Jbase.jbase" as jbase

@Override
XJStatementOrBlock returns xbase::XExpression:
    LocalClass | =>XBlockExpression | XJSingleStatement;

LocalClass returns jbase::XJLocalClass:
    {LocalClass} 'local' name=ValidID
    ('extends' superType=JvmParameterizedTypeReference)?
    ('implements' interfaces+=JvmParameterizedTypeReference
        (',' interfaces+=JvmParameterizedTypeReference)*)?
    '{' body=XJAnonymousClassBody '}';
```

Domainmodel's existing `XJAnonymousClassBody` override contains `Feature`s
(properties and operations). Reusing the body also establishes the correct class
boundary for captures, `return`, `yield`, `break` and `continue`.

The host may choose another declaration keyword or header. Additional modifiers,
type parameters, annotations and constructor syntax are host responsibilities.
Do not put the declaration in `XPrimaryExpression`: a local class is a block
content, not a value or a Java statement expression. The shared validator rejects
unbraced uses such as `if (condition) local Local {}`.

Regenerate the language through its owning MWE2 workflow. For a manually managed
host metamodel, define the concrete subtype and header features in Ecore and
GenModel too. Jbase's own base type is maintained in
[`Jbase.ecore`](../jbase/model/custom/Jbase.ecore) and
[`Jbase.genmodel`](../jbase/model/custom/Jbase.genmodel).

### JVM inference

Inject `JbaseAnonymousClassInferrer`. Its four-argument `inferLocalClasses` overload
handles both anonymous expressions and named declarations. The existing overloads
remain available for languages with only anonymous body syntax.

In Domainmodel's `inferFeatures`, each operation is attached to its owner after
assigning its body. Then register its local classes during normal initialization:

```xtend
anonymousClasses.inferLocalClasses(method, f.body, [ source, type |
    type.inferFeatures((source.body as AnonymousClassBody).features)
], [ source, type |
    val declaration = source as LocalClass
    type.superClass = declaration.superType ?: typeRef(Object)
    for (implemented : declaration.interfaces)
        type.addSuperInterface(implemented)
    if (source.body !== null)
        type.inferFeatures((source.body as AnonymousClassBody).features)
])
```

The first callback initializes anonymous types; the second initializes named
types. The helper has already attached and associated the supplied JVM type,
assigned the source name and set package visibility. Populate this supplied type
rather than creating another one. Use `JvmTypesBuilder.setSuperClass` (the Xtend
`superClass` property) and `addSuperInterface` instead of appending to `superTypes`.
Both methods clone references with proxies and preserve source associations. They
also mark whether each reference is expected to be a class or an interface, so
`JvmGenericTypeValidator` rejects an interface in `extends`, a class in
`implements`, and duplicate interfaces at the source declaration. Supply an Object
superclass when there are only implemented interfaces. When no supertypes are
supplied at all, the helper supplies Object itself.

If the host supplies no constructors, the helper creates a default constructor.
Its superclass must have an accessible no-argument constructor. A host supporting
explicit constructors supplies their JVM signatures and bodies in the callback;
Jbase emits those inferred members. No constructor or member declaration syntax
is added to Jbase or to these examples by this feature.

Use the same inference path recursively for member operation bodies. The helper
prunes class bodies during its enclosing-feature traversal; those bodies belong
to the new type's own features. Domainmodel reuses `inferFeatures`, so local and
anonymous classes can nest in either direction. Apply the helper to every field,
method or constructor expression that the host permits to contain declarations.
Do this during initialization, after attaching the JVM feature, rather than
resolving cross references during pre-indexing.

### Formatting and serialization

The host formats its own declaration header, braces and member body.
`DomainmodelFormatter._format(LocalClass, IFormattableDocument)` formats the
`local`, `extends` and `implements` header and delegates the body to the existing
anonymous-body formatter. Use one newline after the final member to avoid
conflicting constraints at the closing brace.

Ordinary Jbase type and constructor serialization uses the same lexical scope
as linking. No local-class-specific serializer binding is needed. Test both
parsed models and resolved copies without node models; the latter exercises name
recovery independently of the original source text.

## Semantics and validation

A named type is visible from its declaration through the rest of its block,
including its own body and nested blocks. It is unavailable before its
declaration and outside that block. Imported types may be shadowed. Disjoint
blocks may reuse a name; overlapping local declarations and names matching an
enclosing type are rejected.

Local methods have their own `this` and `super`. Captured parameters and local
variables must be final or effectively final, and captured locals must be
definitely assigned at the declaration. Deferred member writes do not initialize
enclosing locals. Branches cannot leave a class body to target an enclosing loop,
label or switch expression.

The shared JVM validator checks inherited abstract methods, constructor
availability, final supertypes, duplicate members and overriding signatures.
Local classes cannot directly extend or implement sealed types. Java generation
emits a native local declaration at its original block position, followed by
ordinary constructor calls and type references. Java preserves local class
identity, enclosing instances and capture behavior; declarations are not hoisted
to top-level or member types.

## Implementation and regression tests

The shared implementation is in:

- [`JbaseAnonymousClassInferrer`](../jbase/src/jbase/jvmmodel/JbaseAnonymousClassInferrer.java):
  JVM skeletons, callbacks, default constructors and class-body traversal boundaries.
- [`JbaseImportSectionNamespaceScopeProvider`](../jbase/src/jbase/scoping/JbaseImportSectionNamespaceScopeProvider.java):
  declaration-order type lookup and filtering local names out of resource-wide lookup.
- [`JbaseTypeComputer`](../jbase/src/jbase/typesystem/JbaseTypeComputer.java) and
  [`JbaseReentrantTypeResolver`](../jbase/src/jbase/typesystem/JbaseReentrantTypeResolver.java):
  void declaration typing, capture snapshots and member inference. The resolver
  preserves class/interface markers when wrapping a local supertype for captures,
  allowing JVM inheritance validation to inspect the wrapper.
- [`JbaseAnonymousClassValidator`](../jbase/src/jbase/validation/JbaseAnonymousClassValidator.java),
  [`JbaseValidator`](../jbase/src/jbase/validation/JbaseValidator.java) and
  [`JbaseInitializedVariableFinder`](../jbase/src/jbase/validation/JbaseInitializedVariableFinder.java):
  JVM checks, lexical restrictions, effectively final captures and definite assignment.
- [`JbaseXbaseCompiler`](../jbase/src/jbase/compiler/JbaseXbaseCompiler.java):
  native local declaration emission using the JVM model generator.

The host integration is in Domainmodel's
[`Domainmodel.xtext`](../domainmodel/org.eclipse.xtext.example.domainmodel/src/org/eclipse/xtext/example/domainmodel/Domainmodel.xtext),
[`DomainmodelJvmModelInferrer`](../domainmodel/org.eclipse.xtext.example.domainmodel/src/org/eclipse/xtext/example/domainmodel/jvmmodel/DomainmodelJvmModelInferrer.xtend)
and [`DomainmodelFormatter`](../domainmodel/org.eclipse.xtext.example.domainmodel/src/org/eclipse/xtext/example/domainmodel/formatting2/DomainmodelFormatter.java).

[`JbaseLocalClassTest`](../jbase.tests/src/jbase/tests/JbaseLocalClassTest.java)
checks parsing, diagnostics, exact Java, Java compilation, execution, local class
identity, lexical scope, inheritance, captures, mixed nesting, host constructors
and serialization with and without node models.
[`JbaseFormatterTest`](../jbase.tests/src/jbase/tests/JbaseFormatterTest.java)
checks local declaration formatting, and
[`AllJavaFeaturesTest`](../jbase.tests/src/jbase/tests/AllJavaFeaturesTest.java)
includes a named class in its full compilation showcase.
[`JbaseContentAssistTest`](../jbase.ui.tests/src/jbase/ui/tests/JbaseContentAssistTest.java)
checks local members, captured-variable completion and names outside their block.
Domainmodel's
[`CompilerTest`](../domainmodel/org.eclipse.xtext.example.domainmodel.tests/src/org/eclipse/xtext/example/domainmodel/tests/CompilerTest.xtend),
[`DomainmodelParsingTest`](../domainmodel/org.eclipse.xtext.example.domainmodel.tests/src/org/eclipse/xtext/example/domainmodel/tests/DomainmodelParsingTest.xtend)
and [`FormatterTest`](../domainmodel/org.eclipse.xtext.example.domainmodel.tests/src/org/eclipse/xtext/example/domainmodel/tests/FormatterTest.xtend)
exercise the complete example integration, including entity superclass and
interface generation, execution and serialization, inheritance-role diagnostics
for entities and local classes, and formatting of both declaration headers.
The test-language grammar, inferrer and formatter are
[`JbaseTestlanguage.xtext`](../jbase.testlanguage/src/jbase/testlanguage/JbaseTestlanguage.xtext),
[`JbaseTestlanguageJvmModelInferrer`](../jbase.testlanguage/src/jbase/testlanguage/jvmmodel/JbaseTestlanguageJvmModelInferrer.xtend)
and [`JbaseTestlanguageFormatter`](../jbase.testlanguage/src/jbase/testlanguage/formatting2/JbaseTestlanguageFormatter.xtend).
`JbaseLocalClassTest` also checks the expected class/interface roles and duplicate
interfaces in this host.
