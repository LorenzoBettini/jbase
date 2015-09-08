/**
 * 
 */
package jbase.ui.syntaxcoloring;

import org.eclipse.xtext.ui.editor.syntaxcoloring.DefaultAntlrTokenToAttributeIdMapper;

/**
 * Highlighting for char literals.
 * 
 * @author Lorenzo Bettini
 *
 */
public class JbaseTokenToAttributeIdMapper extends
		DefaultAntlrTokenToAttributeIdMapper {

	@Override
	protected String calculateId(String tokenName, int tokenType) {
		if("RULE_CHARACTER".equals(tokenName)) {
			return JbaseHighlightingConfiguration.CHAR_ID;
		}
		return super.calculateId(tokenName, tokenType);
	}
}
