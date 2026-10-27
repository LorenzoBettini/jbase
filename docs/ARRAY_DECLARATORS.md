# Array declarators and initializers

Jbase accepts Java's post-name array dimensions for local declarations,
additional declarators, and typed parameters:

```java
int values[] = {1, 2,};
int scalar = 1, more[] = {3,};
java.util.function.Consumer<String[]> consumer =
    (String items[]) -> System.out.println(items[0]);
```

Array initializer lists, including those following `new T[]`, may have one
trailing comma. Generated Java uses the same array types and initializer values;
the post-name dimensions do not change the declared variable's type.
