package jbase.example.client.maven

import org.junit.Test

import static org.junit.Assert.*
import jbasescript.JbasescriptExample

class FooTest {
	@Test
	def void testNull() {
		assertEquals("passed: null", JbasescriptExample.testMe(null))
	}

	@Test
	def void testNotNull() {
		assertEquals("passed: foo", JbasescriptExample.testMe("foo"))
	}
}
