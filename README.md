# Jbase
Jbase is a reusable Xtext Expression language: it is a customization of Xtext Xbase to deal with pure Java expressions and statements.

[![Java CI with Maven](https://github.com/LorenzoBettini/jbase/actions/workflows/maven.yml/badge.svg)](https://github.com/LorenzoBettini/jbase/actions/workflows/maven.yml) [![Coverage Status](https://coveralls.io/repos/LorenzoBettini/jbase/badge.svg?branch=master&service=github)](https://coveralls.io/github/LorenzoBettini/jbase?branch=master)
[![Quality gate status](https://sonarcloud.io/api/project_badges/measure?project=LorenzoBettini_jbase&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=LorenzoBettini_jbase)
[![Maven Central](https://img.shields.io/maven-central/v/io.github.lorenzobettini.jbase/jbase.svg?label=Maven%20Central)](https://search.maven.org/search?q=g:%22io.github.lorenzobettini.jbase%22%20AND%20a:%22jbase%22)

## Eclipse Update Site (new URL)

All releases: https://lorenzobettini.github.io/jbase-releases/

Please make sure you add the Xtext update site (http://download.eclipse.org/modeling/tmf/xtext/updates/composite/releases/) before you install Jbase features, so that all the requested Xtext bundles, with the specific version required by Jbase, are found.

WARNING: the previous update site hosted on bintray will disappear soon, so please make sure you update your existing Eclipse distribution and Target Platform where you were already using Jbase.

### Specific versions

Please make sure to use the version of Jbase that corresponds to the version of Xtext you want to use.  You can find the right version by looking at the update site category that is of the shape, for example

```
Jbase 0.9.x (for Xtext 2.15.0)
```

If you want to make sure you do not upgrade to a newer version of Jbase by using the "All releases" update site, you can use the update site of a specific major.minor version, for example, for version 0.9.x you can use the update site

https://lorenzobettini.github.io/jbase-releases/updates/0.x/0.9.x

or you can use the update site of a specific major version, for example, for version 0.x you can use the update site

https://lorenzobettini.github.io/jbase-releases/updates/0.x

## Maven Artifacts

Maven Artifacts for Jbase are available from Maven Central (both releases and snapshots), in case you need to process your DSL files with the _xtext-maven-plugin_.  An example can be found in the project *jbase.example.client.maven*.

The groupId and artifactId are as follows

```
<dependency>
	<groupId>io.github.lorenzobettini.jbase</groupId>
	<artifactId>jbase</artifactId>
	<version>...</version>
</dependency>
```

For versions earlier than version 1.x the groupId and artifactId are as follows

```
<dependency>
	<groupId>net.sf.xtext-jbase</groupId>
	<artifactId>jbase</artifactId>
	<version>...</version>
</dependency>
```

## Documentation

Jbase assumes that you are already familiar with Xtext and in particular with Xbase concepts.

Contributor guides explain the Xbase integration for
[lambda expressions](docs/LAMBDA_EXPRESSIONS.md),
[method and constructor references](docs/METHOD_REFERENCES.md),
[`instanceof` type patterns](docs/INSTANCEOF_PATTERNS.md), and
[Java 21 switches](docs/ENHANCED_SWITCH.md).

The Jbase SDK that you install in Eclipse comes with (currently small) documentation in Eclipse format.  The same documentation can be [browsed online](http://xtext-jbase.sourceforge.net/jbase-documentation/00-Main.html).

## Java lambda expressions

Lambda expressions use Java syntax and a functional interface as their target type:

```java
java.util.function.Function<String, Integer> length = s -> s.length();
java.util.function.IntBinaryOperator sum = (int x, int y) -> x + y;
java.util.function.IntUnaryOperator identity = (var n) -> n;
Runnable task = () -> { System.out.println("hello"); };
java.util.function.IntUnaryOperator abs = n -> {
	if (n < 0) return -n;
	return n;
};
```

Parameter types can be inferred, written with `var` (optionally `final var`), or
declared explicitly. All parameters in a `var` parameter list must use `var`.
Block bodies use explicit `return` statements for results. Captured local variables must be final or
effectively final. Lambdas are represented by Xbase's `XClosure`, reusing its
functional interface typing and Java generation.

Method and constructor references use Java syntax and functional interface target typing:

```java
java.util.function.ToIntFunction<String> length = String::length;
java.util.function.Consumer<String> print = System.out::println;
java.util.function.Function<String, Integer> parse = Integer::parseInt;
java.util.function.Supplier<java.util.ArrayList<String>> list =
    java.util.ArrayList<String>::new;
java.util.function.IntFunction<int[]> array = int[]::new;
```

References are emitted as Java references. Bound receivers are evaluated when the
reference is created. References also work as method arguments, return values,
assignments, conditional branches, and cast operands. Content assist after `::`
proposes method names without invocation parentheses.

## Pattern matching for instanceof

Type patterns retain Java syntax in the generated code:

```java
if (value instanceof String text && !text.isEmpty()) {
    System.out.println(text.length());
}
if (!(value instanceof String text)) return;
System.out.println(text.length());
```

Bindings follow Java's flow scopes in then/else branches, negated and
short-circuit conditions, conditional expressions, and loops. Optional `final`
prevents reassignment. See the [type pattern guide](docs/INSTANCEOF_PATTERNS.md).

## Enhanced switches

Jbase supports Java 21 switch statements and expressions, including arrow rules,
grouped labels, type patterns, `when` guards, `yield`, and `case null, default`:

```java
Object value = "hello";
int length = switch (value) {
    case String text when !text.isEmpty() -> text.length();
    case Integer number -> {
        int result = number.intValue();
        yield result;
    }
    case null, default -> 0;
};
switch (length) {
    case 0, 1 -> System.out.println("short");
    default -> System.out.println(length);
}
```

Generated Java preserves native switches, patterns, guards, and `yield`.
Expression rules may become blocks containing `yield`. Switch expressions must
be exhaustive; arrow rules do not fall through and cannot be mixed with colon
labels. Pattern bindings are visible in their guard and body. Compile generated
pattern switches with Java 21 or later. See the [enhanced switch guide](docs/ENHANCED_SWITCH.md)
for details and current limitations.

## Local variable type inference

Jbase supports Java's `var` and `final var` for local variables, basic and enhanced
`for` loops, and `try`-with-resources declarations:

```java
var count = 0;
final var names = new java.util.ArrayList<String>();
for (var i = 0; i < 3; i++) {
    count += i;
}
for (final var name : names) {
    System.out.println(name);
}
try (var input = new java.io.StringReader("hello")) {
    System.out.println(input.read());
}
```

The inferred type stays fixed after initialization. Local `var` declarations need
one variable and an initializer; `null` and bare array initializers such as `{1, 2}`
cannot supply the type. Use `new int[] {1, 2}` for an inferred array type.
Lambdas also need an explicit target type, for example
`var task = (Runnable) () -> System.out.println("hello");`.
`var` is not supported for method or catch parameters. Resource variables are
implicitly final, and `var` remains valid as a variable or method name.

Known Java compatibility gaps and implementation tasks are tracked in
[Unsupported Java features](UNSUPPORTED_JAVA_FEATURES.md).

## Contribute to Jbase

We provide an [Oomph setup](https://wiki.eclipse.org/Eclipse_Installer), which is available from the official Oomph catalog.

## Compiling From Sources

To build Jbase locally, use Java 21 and the Maven wrapper (Maven 3.9.9). The build targets Xtext 2.42.0 and Eclipse 2026-03. From the repository root, run

```
./mvnw -f jbase.releng/pom.xml clean verify
```

You will find the p2 update site in the _target_ directory of the _jbase.site_ project.

The above command will also execute all the tests, including UI tests that will run Eclipse instances (you will see Eclipse appear at some point during the build).

If you want to skip tests, you should run the following command instead of the previous one:

```
./mvnw -f jbase.releng/pom.xml clean verify -DskipTests
```

