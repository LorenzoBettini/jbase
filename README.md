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
functional interface typing and Java generation. Method references are not supported.

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

