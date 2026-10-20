package jbase.tests.util;

public class LambdaFixtures {
	public interface SevenArguments {
		int apply(int a, int b, int c, int d, int e, int f, int g);
	}

	public interface GenericOperation {
		<T> T apply(T value);
	}
}
