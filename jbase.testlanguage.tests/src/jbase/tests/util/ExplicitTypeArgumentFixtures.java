package jbase.tests.util;

public final class ExplicitTypeArgumentFixtures {
	public ExplicitTypeArgumentFixtures() {
	}

	public <T> T identity(final T value) {
		return value;
	}

	public static class GenericConstructor<T> {
		public <U, V> GenericConstructor(final U first, final V second) {
		}
	}
}
