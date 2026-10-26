package jbase.tests.util;

@FunctionalInterface
public interface VarargsConsumer {

	void accept(String prefix, String... values);
}
