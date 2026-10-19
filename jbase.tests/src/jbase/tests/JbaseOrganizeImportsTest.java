package jbase.tests;

import static org.junit.Assert.*;
import com.google.inject.Inject;
import java.util.Comparator;
import jbase.testlanguage.tests.JbaseTestlanguageInjectorProvider;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.resource.XtextResource;
import org.eclipse.xtext.testing.InjectWith;
import org.eclipse.xtext.testing.XtextRunner;
import org.eclipse.xtext.testing.util.ParseHelper;
import org.eclipse.xtext.util.ReplaceRegion;
import org.eclipse.xtext.xbase.imports.ImportOrganizer;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(XtextRunner.class)
@InjectWith(JbaseTestlanguageInjectorProvider.class)
public class JbaseOrganizeImportsTest extends JbaseAbstractTest {

	/**
	 * Since we use both Jbase and JbaseTestlanguage we specify EObject
	 * as the type returned by ParseHelper
	 */
	@Inject
	protected ParseHelper<EObject> parseHelper;

	@Inject
	private ImportOrganizer importOrganizer;

	protected void assertIsOrganizedTo(final CharSequence model, final CharSequence expected)
			throws Exception {
		// in our DSL the following would fail for inputs such as
		// "java.io.Serializable s = null;"
		// so we must make sure to have enough leading whitespaces in the input
		final var program = parseHelper.parse(model);
		final var changes = importOrganizer
				.getOrganizedImportChanges((XtextResource) program.eResource());
		final var builder = new StringBuilder(model);
		final var sortedChanges = changes.stream()
				.sorted(Comparator.comparingInt(ReplaceRegion::getOffset)).toList();
		ReplaceRegion lastChange = null;
		for (final ReplaceRegion change : sortedChanges) {
			if ((lastChange != null) && (lastChange.getEndOffset() > change.getOffset())) {
				fail("Overlapping text edits: " + lastChange + " and " + change);
			}
			lastChange = change;
		}
		for (final ReplaceRegion change : sortedChanges.reversed()) {
			builder.replace(change.getOffset(), change.getOffset() + change.getLength(),
					change.getText());
		}
		assertEquals(expected.toString(), builder.toString());
	}

	@Test
	public void testDefaultPackage() throws Exception {
		assertIsOrganizedTo("""

			\s\s\s\t
			java.io.Serializable s = null;
			""", """
			import java.io.Serializable;

			Serializable s = null;
			""");
	}

	@Test
	public void testDefaultPackages() throws Exception {
		assertIsOrganizedTo("""

			\s\s\s\t
			java.io.Serializable s = null;
			java.util.List l = null;
			""", """
			import java.io.Serializable;
			import java.util.List;

			Serializable s = null;
			List l = null;
			""");
	}

	@Test
	public void testDefaultPackagesSorted() throws Exception {
		assertIsOrganizedTo("""
			import java.util.List;
			import java.io.Serializable;

			Serializable s = null;
			List l = null;
			""", """
			import java.io.Serializable;
			import java.util.List;

			Serializable s = null;
			List l = null;
			""");
	}
}
