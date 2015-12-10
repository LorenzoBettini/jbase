# jbase
A Customization of Xtext Xbase to deal with pure Java expressions and statements.

[![Build Status](https://travis-ci.org/LorenzoBettini/jbase.svg?branch=master)](https://travis-ci.org/LorenzoBettini/jbase) [![Coverage Status](https://coveralls.io/repos/LorenzoBettini/jbase/badge.svg?branch=master&service=github)](https://coveralls.io/github/LorenzoBettini/jbase?branch=master)

There is still no update site for Jbase (it will be available soon).

If you want to build its update site locally, you need Maven.

First of all, make sure to increase memory

```
export MAVEN_OPTS="-Xmx512m -XX:MaxPermSize=256m"
```

Then cd to jbase.releng directory and run

```
mvn clean verify
```

You will find the p2 update site in the target directory of the jbase.site project.
