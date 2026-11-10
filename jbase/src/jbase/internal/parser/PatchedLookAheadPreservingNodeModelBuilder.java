package jbase.internal.parser;

import java.util.Iterator;

import org.eclipse.xtext.nodemodel.INode;
import org.eclipse.xtext.nodemodel.impl.AbstractNode;
import org.eclipse.xtext.nodemodel.impl.CompositeNode;
import org.eclipse.xtext.nodemodel.impl.NodeModelBuilder;
import org.eclipse.xtext.xbase.parser.LookAheadPreservingNodeModelBuilder;

/**
 * Transfers lookahead during partial reparsing while allowing a composite node to be replaced
 * by a leaf. This adapts {@link LookAheadPreservingNodeModelBuilder} for
 * https://github.com/eclipse/xtext-xtend/issues/1067 and
 * https://github.com/LorenzoBettini/jbase/issues/131.
 */
public class PatchedLookAheadPreservingNodeModelBuilder extends NodeModelBuilder {

	/**
	 * Copies lookahead only between corresponding composite nodes. A changed parse can replace
	 * a composite with a leaf, in which case there is no lookahead to transfer.
	 */
	@Override
	public void replaceAndTransferLookAhead(final INode oldNode, final INode newRootNode) {
		final Iterator<AbstractNode> oldNodes = ((AbstractNode) oldNode).basicIterator();
		final Iterator<AbstractNode> newNodes = ((AbstractNode) newRootNode).basicIterator();
		newNodes.next(); // Skip the wrapper root; its first child replaces oldNode.
		while (oldNodes.hasNext()) {
			final AbstractNode nextOld = oldNodes.next();
			final AbstractNode nextNew = newNodes.next();
			if (nextOld instanceof CompositeNode oldComposite
					&& nextNew instanceof CompositeNode newComposite) {
				setLookAhead(newComposite, oldComposite.getLookAhead());
			}
		}
		super.replaceAndTransferLookAhead(oldNode, newRootNode);
	}
}
