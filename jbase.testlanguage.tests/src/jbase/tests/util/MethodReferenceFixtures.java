package jbase.tests.util;

import java.io.Serializable;

/** Java declarations used to compare reference overload selection with javac. */
public class MethodReferenceFixtures {
	public static class Box<T> {
		public class Value {
			public T get() {
				return null;
			}
		}
	}

	public static String choose(final Object value) {
		return "Object";
	}

	public static String choose(final int value) {
		return "int";
	}

	public static String spread(final Object... values) {
		return "Object";
	}

	public static String spread(final String... values) {
		return "String";
	}

	public static String prefixed(final Object prefix, final Object... values) {
		return "Object";
	}

	public static String prefixed(final Object prefix, final String... values) {
		return "String";
	}

	public static String ambiguous(final Serializable value) {
		return "Serializable";
	}

	public static String ambiguous(final Comparable<?> value) {
		return "Comparable";
	}

	public static String ambiguousVarargs(final String first, final String... rest) {
		return "first";
	}

	public static String ambiguousVarargs(final String... values) {
		return "all";
	}

	public static <T> Object pick(final T value) {
		return "generic";
	}

	public static String pick(final String value) {
		return "String";
	}

	public static Class<?> getClass(final int value) {
		return Integer.class;
	}

	public static <T> T identity(final T value) {
		return value;
	}

	public static <T extends Number> T bounded(final T value) {
		return value;
	}

	public static int apply(final java.util.function.ToIntFunction<String> function) {
		return function.applyAsInt("hi");
	}

	public static String apply(final java.util.function.Function<Integer, String> function) {
		return function.apply(1);
	}

	public static void unchecked() throws IllegalArgumentException, AssertionError {
		// Both exception types are permitted by a non-throwing functional interface.
	}
}
