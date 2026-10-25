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
    x: int
    y: int
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

## Inferrer integration

Use `DefaultJbaseRuntimeModule` for the Jbase runtime bindings. Inject
`jbase.jvmmodel.JbaseAnonymousClassInferrer` into the host JVM model inferrer.
After assigning a JVM feature's expression body, call:

```java
anonymousClasses.inferLocalClasses(feature, expression);
```

For nonempty bodies, use the overload accepting a
`BiConsumer<XJAnonymousClass, JvmGenericType>`. Infer the body's members into the
supplied type with ordinary `JvmTypesBuilder` associations and expression bodies.
Call the helper for each inferred member's body as well, to support nested
anonymous classes. The helper handles anonymous classes in constructor arguments.
Do this during the inferrer's normal initialization callback, without resolving
cross references during pre-indexing.

The helper creates local anonymous JVM types and associates them with source
expressions. Jbase resolves their supertypes and constructors during type
resolution, captures the enclosing local scope, validates inheritance and
members, and generates the inferred members inside native Java anonymous bodies.
No Xtend runtime dependency is required.

Local variables and parameters captured by methods must be final or effectively
final and definitely assigned at creation. Constructor arguments are evaluated
immediately. Anonymous methods have their own `this` and `super`, and their
control-flow statements cannot target enclosing loops or switch expressions.

Only the constructor forms already available in Jbase are supported. This does
not add local class declarations or qualified inner-class creation. Other example
languages must opt in through their own inferrers before using anonymous classes.
