# Jbase
Jbase is a reusable Xtext Expression language: it is a customization of Xtext Xbase to deal with pure Java expressions and statements.

[![Java CI with Maven](https://github.com/LorenzoBettini/jbase/actions/workflows/maven.yml/badge.svg)](https://github.com/LorenzoBettini/jbase/actions/workflows/maven.yml) [![Coverage Status](https://coveralls.io/repos/LorenzoBettini/jbase/badge.svg?branch=master&service=github)](https://coveralls.io/github/LorenzoBettini/jbase?branch=master) [![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=net.sf.xtext-jbase%3Ajbase.releng&metric=alert_status)](https://sonarcloud.io/dashboard?id=net.sf.xtext-jbase%3Ajbase.releng)

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

## Contribute to Jbase

We provide an [Oomph setup](https://wiki.eclipse.org/Eclipse_Installer), which is available from the official Oomph catalog.

## Compiling From Sources

If you want to build Jbase update site locally, you need Maven: cd to `jbase.releng` directory and run

```
mvn clean verify
```

You will find the p2 update site in the _target_ directory of the _jbase.site_ project.

The above command will also execute all the tests, including UI tests that will run Eclipse instances (you will see Eclipse appear at some point during the build).

If you want to skip tests, you should run the following command instead of the previous one:

```
mvn clean verify -DskipTests
```

