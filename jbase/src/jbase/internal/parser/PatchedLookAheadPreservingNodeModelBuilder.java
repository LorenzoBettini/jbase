/**
 * 
 */
package jbase.internal.parser;

import java.util.Iterator;

import org.eclipse.xtext.nodemodel.INode;
import org.eclipse.xtext.nodemodel.impl.AbstractNode;
import org.eclipse.xtext.nodemodel.impl.CompositeNode;
import org.eclipse.xtext.nodemodel.impl.NodeModelBuilder;
import org.eclipse.xtext.xbase.parser.LookAheadPreservingNodeModelBuilder;

/**
 * Copied from {@link LookAheadPreservingNodeModelBuilder} adapted to fix the
 * issue https://github.com/eclipse/xtext-xtend/issues/1067
 * and https://github.com/LorenzoBettini/jbase/issues/131
 * 
 * @author Lorenzo Bettini
 *
 */
public class PatchedLookAheadPreservingNodeModelBuilder extends NodeModelBuilder {

	@Override
	public void replaceAndTransferLookAhead(INode oldNode, INode newRootNode) {
		Iterator<AbstractNode> oldNodes = ((AbstractNode) oldNode).basicIterator();
		Iterator<AbstractNode> newNodes = ((AbstractNode) newRootNode).basicIterator();
		newNodes.next(); // basicGetFirstChild to skip that one
		while(oldNodes.hasNext()) {
			AbstractNode nextOld = oldNodes.next();
			AbstractNode nextNew = newNodes.next();
			if (nextOld instanceof CompositeNode && nextNew instanceof CompositeNode) {
				setLookAhead((CompositeNode) nextNew, ((CompositeNode) nextOld).getLookAhead());
			}
		}
		super.replaceAndTransferLookAhead(oldNode, newRootNode);
	}
}
