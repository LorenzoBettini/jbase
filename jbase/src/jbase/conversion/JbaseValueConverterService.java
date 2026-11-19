/**
 * 
 */
package jbase.conversion;

import org.eclipse.xtext.conversion.IValueConverter;
import org.eclipse.xtext.conversion.ValueConverter;
import org.eclipse.xtext.xbase.conversion.XbaseValueConverterService;

import com.google.inject.Inject;

/**
 * Converts Java character, text block, and numeric literals.
 * 
 * @author Lorenzo Bettini
 *
 */
public class JbaseValueConverterService extends XbaseValueConverterService {

	@Inject
	private JbaseStringValueConverter stringValueConverter;

	@Inject
	private JbaseCharacterValueConverter characterValueConverter;

	@Inject
	private JbaseIdentifierValueConverter identifierValueConverter;

	@Inject
	private JbaseTextBlockValueConverter textBlockValueConverter;

	@Inject
	private JbaseNumberValueConverter numberValueConverter;

	@ValueConverter(rule = "Number")
	public IValueConverter<String> Number() {
		return numberValueConverter;
	}

	@ValueConverter(rule = "TEXT_BLOCK")
	public IValueConverter<String> TEXT_BLOCK() {
		return textBlockValueConverter;
	}

	@Override
	@ValueConverter(rule = "STRING")
	public IValueConverter<String> STRING() {
		return stringValueConverter;
	}

	@ValueConverter(rule = "CHARACTER")
	public IValueConverter<String> CHARACTER() {
		return characterValueConverter;
	}

	@Override
	@ValueConverter(rule = "ID")
	public IValueConverter<String> ID() {
		return identifierValueConverter;
	}

}
