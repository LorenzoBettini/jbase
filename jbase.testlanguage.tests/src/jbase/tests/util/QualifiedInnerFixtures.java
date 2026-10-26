package jbase.tests.util;

public final class QualifiedInnerFixtures {
	private QualifiedInnerFixtures() {
	}

	public static class Outer {
		public class Inner {
			public int value() {
				return 42;
			}
		}

		public class GenericInner {
			public <T> GenericInner(final T value) {
			}
		}

		public static class StaticNested {
		}

		public interface Marker {
		}
	}
}
