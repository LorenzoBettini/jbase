/**
 * 
 */
package jbase.conversion;

import org.eclipse.xtext.conversion.IValueConverter;
import org.eclipse.xtext.conversion.ValueConverter;
import org.eclipse.xtext.conversion.impl.STRINGValueConverter;
import org.eclipse.xtext.xbase.conversion.XbaseValueConverterService;

import com.google.inject.Inject;

/**
 * Deals with character literals.
 * 
 * @author Lorenzo Bettini
 *
 */
public class JbaseValueConverterService extends XbaseValueConverterService {

	@Inject
	private STRINGValueConverter stringValueConverter;

	@Inject
	private JbaseTextBlockValueConverter textBlockValueConverter;

	@ValueConverter(rule = "TEXT_BLOCK")
	public IValueConverter<String> TEXT_BLOCK() {
		return textBlockValueConverter;
	}
	
	@ValueConverter(rule = "CHARACTER")
	public IValueConverter<String> CHARACTER() {
		return stringValueConverter;
	}
}
