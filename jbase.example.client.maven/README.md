If you want to try this example, which uses the _xtext-maven-plugin_ and the Jbase Maven artifacts, you need:

First of all, make sure to increase memory

```
export MAVEN_OPTS="-Xmx512m -XX:MaxPermSize=256m"
```

Then cd to jbase.releng directory and run (this will install in your local Maven repository the DSL Jbasescript, which is used in this example):

```
mvn clean install -DskipTests -Pjbasescript
```

Then come back to this directory and run

```
mvn clean verify
```
