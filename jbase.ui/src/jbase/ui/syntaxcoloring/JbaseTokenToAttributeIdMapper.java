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
	protected String calculateId(final String tokenName, final int tokenType) {
		if("RULE_CHARACTER".equals(tokenName)) {
			return JbaseHighlightingConfiguration.CHAR_ID;
		}
		if ("RULE_TEXT_BLOCK".equals(tokenName)) {
			return JbaseHighlightingConfiguration.STRING_ID;
		}
		return super.calculateId(tokenName, tokenType);
	}
}
