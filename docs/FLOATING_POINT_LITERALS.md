# Floating-point literals

Jbase supports Java decimal and hexadecimal floating-point literal forms,
including hexadecimal significands with binary exponents, leading-dot decimals,
and a decimal point without following fractional digits:

```java
double hexadecimal = 0x1.8p1;
double integralSignificand = 0x1p2;
double leadingDot = .5;
double trailingDot = 1.;
double trailingDotExponent = 1.e2;
```

These literals are preserved by parsing and serialization and compile with their
Java runtime values. Regression coverage is in `JbaseIntegerLiteralTest` and
`AllJavaFeaturesTest`.
