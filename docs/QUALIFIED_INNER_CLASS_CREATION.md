# Qualified inner-class creation

Jbase supports Java's qualified creation syntax for non-static member classes:

```java
Outer outer = new Outer();
Outer.Inner inner = outer.new Inner();
```

The qualifier is an expression, so it can also be a method call or another
primary expression:

```java
Outer.Inner inner = getOuter().new Inner();
```

Constructor lookup uses the qualifier's resolved type to find the member class.
The qualifier is type-checked without consuming the creation expression's target
type, and generated Java retains the `qualifier.new Member(...)` form. Ordinary
constructor calls and static nested-class creation continue to use their existing
syntax.

The grammar, constructor scoping, type computation, and Java generation are
covered by parsing, formatting, serialization, and compilation regressions, and
the Java-features showcase includes a qualified inner-class construction.
