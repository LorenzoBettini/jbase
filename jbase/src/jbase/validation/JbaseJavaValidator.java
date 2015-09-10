/**
 * 
 */
package jbase.validation;

/**
 * Needed because the Java-based JavaValidatorFragment looks for a class with this name, 
 * while the ValidatorFragment looks for {@link JbaseValidator}. We have to support
 * both types of workflows.
 * 
 * @author Lorenzo Bettini - Initial contribution and API
 */
public class JbaseJavaValidator extends JbaseValidator {

}
