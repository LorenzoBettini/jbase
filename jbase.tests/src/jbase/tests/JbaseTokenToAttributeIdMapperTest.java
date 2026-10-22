package jbase.tests;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import jbase.ui.syntaxcoloring.JbaseHighlightingConfiguration;
import jbase.ui.syntaxcoloring.JbaseTokenToAttributeIdMapper;

public class JbaseTokenToAttributeIdMapperTest {

	private static class Mapper extends JbaseTokenToAttributeIdMapper {
		public String id(final String tokenName) {
			return calculateId(tokenName, 0);
		}
	}

	@Test
	public void stringsAndTextBlocksUseTheSameHighlighting() {
		final var mapper = new Mapper();
		assertEquals(mapper.id("RULE_STRING"), mapper.id("RULE_TEXT_BLOCK"));
		assertEquals(JbaseHighlightingConfiguration.CHAR_ID, mapper.id("RULE_CHARACTER"));
	}
}
