# Anonymous class creation

Jbase supports anonymous class expressions with an empty default body:

```java
Object object = new Object() {};
java.io.Serializable marker = new java.io.Serializable() {};
```

The created type is an anonymous subclass or interface implementation. Its
constructor arguments follow ordinary constructor overload resolution. A body
must implement all inherited abstract methods: `new Runnable() {}` parses, but
fails validation because it does not implement `run()`.

## Member syntax belongs to the host language

Jbase does not define Java field or method declarations. A language embedding
Jbase overrides `XJAnonymousClassBody` with its own subtype of
`XJAnonymousClassBody`. The braces belong to Jbase's anonymous expression rule;
the overridden body rule describes the contents between them.

The test language uses its existing properties followed by instance methods:

```java
var point = new Object() {
    property x: int
    property y: int
    method getX(): int { return x; }
    method getY(): int { return this.y; }
};
point.x = 10;
point.y = 20;
point.x = 30;
point.y = 40;
System.out.println(point.getX() + ", " + point.getY());
```

This prints `30, 40`. A `var` retains the actual anonymous type, including its
additional fields and methods. Declaring `Object point` instead exposes only
`Object` members. Fields use the host language's visibility and initialization
rules; the test language does not add field-initializer syntax.

## Domainmodel example

The Domainmodel example supports anonymous classes using its usual properties
and `op` declarations. Properties generate private fields and public getters and
setters; explicit accessors replace generated ones, just as in an entity.

```java
entity Example {
    op calculate(int input):int {
        var counter = new Object() {
            value : int
            op getValue():int { return this.value + input; }
        };
        counter.setValue(40);
        return counter.getValue();
    }
}
```

Anonymous operations can implement interfaces, override superclass methods,
and contain further anonymous classes. Captured parameters and local variables
follow Jbase's final or effectively final rules.

## Adding anonymous classes to a DSL: the Domainmodel walkthrough

The following steps show how Domainmodel integrates anonymous classes. Reuse
these steps with your DSL's own member syntax and JVM model inference.

### 1. Keep the Jbase grammar and runtime integration

The host grammar must inherit `jbase.Jbase`. Domainmodel's
[GenerateDomainmodel.mwe2](../domainmodel/org.eclipse.xtext.example.domainmodel/src/org/eclipse/xtext/example/domainmodel/GenerateDomainmodel.mwe2)
already references Jbase's metamodel and uses its generator fragment:

```mwe2
referencedResource = "platform:/resource/jbase/model/custom/Jbase.genmodel"
xbaseSupport = jbase.mwe2.generator.JbaseGeneratorFragment2 {}
```

This makes the generated abstract runtime module extend
`jbase.DefaultJbaseRuntimeModule`, which supplies Jbase's type-system,
validation, and Java generation bindings. An existing DSL using this setup
needs no extra runtime binding just for anonymous classes.

### 2. Define the anonymous body's members in the host grammar

In [Domainmodel.xtext](../domainmodel/org.eclipse.xtext.example.domainmodel/src/org/eclipse/xtext/example/domainmodel/Domainmodel.xtext),
import Jbase's metamodel and override the body rule:

```xtext
import "http://www.Jbase.jbase" as jbase

@Override
XJAnonymousClassBody returns jbase::XJAnonymousClassBody:
    {AnonymousClassBody} features+=Feature*;
```

`Feature` is Domainmodel's existing choice between `Property` and `Operation`.
Reusing it gives anonymous bodies the same declarations as entities. The action
creates a host-specific `AnonymousClassBody` subtype containing those features.
Do not put braces in this rule: Jbase's anonymous-class expression already owns
them. The `*` also allows empty bodies.

Regenerate the language through its MWE2 workflow after changing the grammar;
this generates Domainmodel's extended metamodel, parser, and serializer. Check
the generation output for grammar ambiguity warnings. A DSL with a manually
maintained metamodel must also define the new body subtype and its containment
feature in its Ecore and GenModel files.

### 3. Reuse member inference and register local classes

In [DomainmodelJvmModelInferrer.xtend](../domainmodel/org.eclipse.xtext.example.domainmodel/src/org/eclipse/xtext/example/domainmodel/jvmmodel/DomainmodelJvmModelInferrer.xtend),
inject the Jbase helper:

```xtend
@Inject JbaseAnonymousClassInferrer anonymousClasses
```

Domainmodel extracts its entity feature loop into
`inferFeatures(JvmDeclaredType it, Iterable<Feature> features)`. Both entities
and anonymous types call that method. It maps properties to private fields with
public accessors and operations to JVM methods, and removes generated accessors
when explicit ones are present. Entity constructors and the generated entity
`toString()` stay in the entity-specific inference.

For each operation, assign the expression body, attach the method to its owner,
and then infer its local anonymous classes. The operation branch uses:

```xtend
val method = f.toMethod(f.name, f.type ?: inferredType) [
    documentation = f.documentation
    translateAnnotations(f.annotations)
    for (p : f.params) {
        parameters += p.toParameter(p.name, p.parameterType)
    }
    body = f.body
]
members += method
anonymousClasses.inferLocalClasses(method, f.body) [ source, type |
    val body = source.body as AnonymousClassBody
    type.inferFeatures(body.features)
]
```

Here `f` is a Domainmodel `Operation`, and `members` belongs to the JVM type
currently being initialized. `JvmTypesBuilder` supplies the source associations
and logical body containers. Keep this work inside the inferrer's normal
initialization callback; do not resolve cross references during pre-indexing.

The callback receives the anonymous expression and its new local JVM type.
Populate that supplied type with the body's members. Since `inferFeatures`
applies the same operation branch recursively, anonymous classes inside anonymous
methods are registered too. The helper handles anonymous classes in constructor
arguments itself. For a DSL allowing only empty bodies, the two-argument overload
`anonymousClasses.inferLocalClasses(method, expression)` is sufficient.

Attaching the method before invoking the helper matters in Domainmodel:
accessor cleanup resolves JVM operation signatures and needs the anonymous type
to be attached to a resource through its enclosing method. Calling the helper
inside `toMethod`'s initializer, before adding the method to `members`, leaves
that resource unavailable during cleanup.

Apply the helper to every JVM feature expression that your DSL permits to
contain anonymous classes, including field initializers or constructor bodies
if your DSL supports them. Domainmodel properties have no initializers, so its
operation bodies are the integration points.

### 4. Format the host-specific body

Jbase's formatter handles the constructor call, braces, and indentation. The
host formatter formats the declarations between those braces. Domainmodel's
[DomainmodelFormatter.java](../domainmodel/org.eclipse.xtext.example.domainmodel/src/org/eclipse/xtext/example/domainmodel/formatting2/DomainmodelFormatter.java)
extends `JbaseFormatter` and adds:

```java
protected void _format(AnonymousClassBody body, IFormattableDocument document) {
    for (final var feature : body.getFeatures()) {
        document.format(feature);
        document.append(feature, this::newLine);
    }
}
```

This reuses the existing property and operation formatters. Use a single newline
after each member here: the last member shares its trailing whitespace with
Jbase's closing-brace formatting. Conflicting newline constraints at that
boundary cause formatting exceptions, which can also surface during serialization.

### 5. Test the integration through generated Java

Domainmodel's regression tests provide a starting point:

- [CompilerTest.xtend](../domainmodel/org.eclipse.xtext.example.domainmodel.tests/src/org/eclipse/xtext/example/domainmodel/tests/CompilerTest.xtend)
  checks interface implementations with captured parameters and locals,
  properties with explicit accessors, nested anonymous classes, superclass
  constructor arguments and `super` calls, and empty bodies. It compiles and
  executes the generated Java, and checks the complete Java output for an empty
  anonymous body.
- [DomainmodelParsingTest.xtend](../domainmodel/org.eclipse.xtext.example.domainmodel.tests/src/org/eclipse/xtext/example/domainmodel/tests/DomainmodelParsingTest.xtend)
  checks missing interface implementations and serialization with and without
  a node model. For the latter, resolve references, copy the parsed model,
  replace the resource contents with the copy, then serialize and reparse it.
- [FormatterTest.xtend](../domainmodel/org.eclipse.xtext.example.domainmodel.tests/src/org/eclipse/xtext/example/domainmodel/tests/FormatterTest.xtend)
  checks formatting of properties and operations inside an anonymous body.

Checking parsing alone does not establish that local types are inferred,
captures are scoped correctly, or valid Java is generated. Keep compilation and
runtime assertions alongside diagnostics and serialization checks.

## Behavior supplied by Jbase

The helper creates local anonymous JVM types and associates them with source
expressions. Jbase resolves their supertypes and constructors during type
resolution, captures the enclosing local scope, validates inheritance and
members, and generates the inferred members inside native Java anonymous bodies.
No Xtend runtime dependency is required.

Local variables and parameters captured by methods must be final or effectively
final and definitely assigned at creation. Constructor arguments are evaluated
immediately. Anonymous methods have their own `this` and `super`, and their
control-flow statements cannot target enclosing loops or switch expressions.

Only the constructor forms already available in Jbase are supported. This provides
no declaration syntax itself. Host-defined named local classes are supported through
an additional inferrer callback; see [Local classes](LOCAL_CLASSES.md).
Qualified inner-class creation is described in
[Qualified inner-class creation](QUALIFIED_INNER_CLASS_CREATION.md). Other example
languages must opt in through their own inferrers before using anonymous classes.

The core implementation is in
[JbaseAnonymousClassInferrer](../jbase/src/jbase/jvmmodel/JbaseAnonymousClassInferrer.java),
[JbaseAnonymousClassUtil](../jbase/src/jbase/jvmmodel/JbaseAnonymousClassUtil.java),
[JbaseAnonymousClassConstructorScopes](../jbase/src/jbase/scoping/JbaseAnonymousClassConstructorScopes.java),
[JbaseReentrantTypeResolver](../jbase/src/jbase/typesystem/JbaseReentrantTypeResolver.java),
[JbaseAnonymousClassValidator](../jbase/src/jbase/validation/JbaseAnonymousClassValidator.java),
and [JbaseXbaseCompiler](../jbase/src/jbase/compiler/JbaseXbaseCompiler.java).
The Jbase regression tests are
[JbaseAnonymousClassTest](../jbase.tests/src/jbase/tests/JbaseAnonymousClassTest.java),
[JbaseAnonymousClassInfrastructureTest](../jbase.tests/src/jbase/tests/JbaseAnonymousClassInfrastructureTest.java),
and [JbaseAnonymousClassRecoveryTest](../jbase.tests/src/jbase/tests/JbaseAnonymousClassRecoveryTest.java).
