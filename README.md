# Jbase
Jbase is a reusable Xtext Expression language: it is a customization of Xtext Xbase to deal with pure Java expressions and statements.

[![Build Status](https://travis-ci.org/LorenzoBettini/jbase.svg?branch=master)](https://travis-ci.org/LorenzoBettini/jbase) [![Coverage Status](https://coveralls.io/repos/LorenzoBettini/jbase/badge.svg?branch=master&service=github)](https://coveralls.io/github/LorenzoBettini/jbase?branch=master)

## Eclipse Update Site

All releases: https://dl.bintray.com/lorenzobettini/xtext-jbase/updates

Zipped update sites: https://dl.bintray.com/lorenzobettini/xtext-jbase/zipped

Please make sure you add the Xtext update site (http://download.eclipse.org/modeling/tmf/xtext/updates/composite/releases/) before you install Jbase features, so that all the requested Xtext bundles, with the specific version required by Jbase, are found.

### Specific versions

- Version 0.3.x is based on Xtext 2.10.x, https://dl.bintray.com/lorenzobettini/xtext-jbase/updates/0.3/
- Version 0.2.x is based on Xtext 2.10.x, https://dl.bintray.com/lorenzobettini/xtext-jbase/updates/0.2/
- Version 0.1.x is based on Xtext 2.8.4, https://dl.bintray.com/lorenzobettini/xtext-jbase/updates/0.1/

## Maven Artifacts

Maven Artifacts for Jbase are available from Maven Central (both releases and snapshots), in case you need to process your DSL files with the _xtext-maven-plugin_.  An example can be found in the project *jbase.example.client.maven*.

The groupId and artifactId are as follows

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

If you want to build Jbase update site locally, you need Maven.

First of all, make sure to increase memory

```
export MAVEN_OPTS="-Xmx512m -XX:MaxPermSize=256m"
```

Then cd to jbase.releng directory and run

```
mvn clean verify
```

You will find the p2 update site in the _target_ directory of the _jbase.site_ project.

The above command will also execute all the tests, including UI tests that will run Eclipse instances (you will see Eclipse appear at some point during the build).

If you want to skip tests, you should run the following command instead of the previous one:

```
mvn clean verify -DskipTests
```

