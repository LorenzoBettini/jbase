# Java assert statements

Jbase supports Java's assertion statement, with or without a detail expression:

```java
assert condition;
assert condition : "expected the condition to hold";
```

The condition must have type `boolean`. The optional detail expression may have
any non-void type. Jbase generates native Java `assert` statements, so their
runtime behavior follows the Java assertion status of the generated class:
assertions are disabled by default and can be enabled with `-ea`.

The grammar represents each assertion as `XJAssertStatement` inside the usual
semicolon statement wrapper. The type computer checks the condition and detail
expression; the compiler and formatter preserve the native statement syntax.
