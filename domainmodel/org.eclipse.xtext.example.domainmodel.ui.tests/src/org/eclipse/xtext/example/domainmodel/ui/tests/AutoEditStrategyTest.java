/**
 * Copyright (c) 2010, 2021 itemis AG (http://www.itemis.eu) and others.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.xtext.example.domainmodel.ui.tests;

import static org.junit.Assert.*;

import java.util.ArrayList;

import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.DocumentCommand;
import org.eclipse.jface.text.IAutoEditStrategy;
import org.eclipse.xtext.example.domainmodel.ui.autoedit.FantasticEditStrategyProvider;
import org.eclipse.xtext.testing.InjectWith;
import org.eclipse.xtext.testing.XtextRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.google.inject.Inject;
import com.google.inject.Injector;

@RunWith(XtextRunner.class)
@InjectWith(DomainmodelUiInjectorProvider.class)
public class AutoEditStrategyTest {
	@Inject
	private Injector injector;

	@Test
	public void restoresOriginalDocumentAfterSurprise() {
		final var strategy = surpriseStrategy();
		final var document = new Document("Xtex");
		final var command = command("t", 4);

		strategy.customizeDocumentCommand(document, command);
		assertTrue(document.get().contains("MMMM"));
		assertEquals("", command.text);
		assertEquals(document.get().lastIndexOf("M") + 1, command.offset);

		strategy.customizeDocumentCommand(document, command("x", 4));
		assertEquals("Xtex", document.get());
		document.set("edited");
		strategy.customizeDocumentCommand(document, command("y", 6));
		assertEquals("edited", document.get());
	}

	@Test
	public void ignoresOtherPrefixes() {
		final var document = new Document("Jbas");
		final var command = command("t", 4);
		surpriseStrategy().customizeDocumentCommand(document, command);
		assertEquals("Jbas", document.get());
		assertEquals("t", command.text);
		assertEquals(4, command.offset);
	}

	@Test
	public void ignoresTypingBeforeFourCharacters() {
		final var document = new Document();
		final var command = command("t", 0);
		surpriseStrategy().customizeDocumentCommand(document, command);
		assertEquals("", document.get());
		assertEquals("t", command.text);
		assertEquals(0, command.offset);
	}

	private IAutoEditStrategy surpriseStrategy() {
		return injector.getInstance(TestableProvider.class).surpriseStrategy();
	}

	private static DocumentCommand command(final String text, final int offset) {
		final var command = new DocumentCommand() {};
		command.text = text;
		command.offset = offset;
		return command;
	}

	public static class TestableProvider extends FantasticEditStrategyProvider {
		IAutoEditStrategy surpriseStrategy() {
			final var strategies = new ArrayList<IAutoEditStrategy>();
			configure((strategy, contentType) -> strategies.add(strategy));
			return strategies.getLast();
		}
	}
}
