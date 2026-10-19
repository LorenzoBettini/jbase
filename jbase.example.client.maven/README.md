This example uses Java 21, the _xtext-maven-plugin_, and the Jbase Maven artifacts.

From the repository root, install Jbase and the Jbasescript DSL into your local Maven repository:

```
./mvnw -f jbase.maven.releng/pom.xml clean install -Pjbasescript
```

Then build and test the standalone client:

```
./mvnw -f jbase.example.client.maven/pom.xml clean verify
```
