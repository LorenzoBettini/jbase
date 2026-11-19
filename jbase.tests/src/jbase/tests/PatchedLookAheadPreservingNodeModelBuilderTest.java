package jbase.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

import org.eclipse.xtext.nodemodel.ICompositeNode;
import org.eclipse.xtext.nodemodel.impl.NodeModelBuilder;
import org.eclipse.xtext.parser.IParser;
import org.eclipse.xtext.testing.InjectWith;
import org.eclipse.xtext.testing.XtextRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.google.inject.Inject;

import jbase.parser.antlr.JbaseParser;

/** Exercises lookahead transfer when a partial parse changes a node's shape. */
@RunWith(XtextRunner.class)
@InjectWith(JbaseInjectorProvider.class)
public class PatchedLookAheadPreservingNodeModelBuilderTest {

	@Inject
	private NodeModelBuilder builder;

	@Inject
	private IParser parser;

	@Test
	public void transfersLookAheadAcrossMatchingAndDifferentNodeKinds() {
		assertEquals(JbaseParser.class, parser.getClass());
		assertEquals("jbase.internal.parser.PatchedLookAheadPreservingNodeModelBuilder",
				builder.getClass().getName());

		final ICompositeNode oldRoot = builder.newRootNode("");
		final ICompositeNode oldNode = builder.newCompositeNode(null, 1, oldRoot);
		builder.newCompositeNode(null, 2, oldNode);
		builder.newCompositeNode(null, 3, oldNode);
		builder.newLeafNode(0, 0, null, false, null, oldNode);

		final ICompositeNode newRoot = builder.newRootNode("");
		final ICompositeNode newNode = builder.newCompositeNode(null, 4, newRoot);
		builder.newCompositeNode(null, 5, newNode);
		builder.newLeafNode(0, 0, null, false, null, newNode);
		builder.newLeafNode(0, 0, null, false, null, newNode);

		builder.replaceAndTransferLookAhead(oldNode, newRoot);

		assertSame(newNode, oldRoot.getFirstChild());
		assertEquals(1, newNode.getLookAhead());
		assertEquals(2, ((ICompositeNode) newNode.getFirstChild()).getLookAhead());
	}
}

