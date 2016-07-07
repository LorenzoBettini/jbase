package jbase.scoping

import org.eclipse.xtext.xbase.scoping.batch.ImplicitlyImportedFeatures

/**
 * Avoid all the default extension classes and methods.
 * 
 * @author Lorenzo Bettini
 * 
 */
class JbaseImplicitlyImportedFeatures extends ImplicitlyImportedFeatures {

	override protected getStaticImportClasses() {
		#[]
	}

	override protected getExtensionClasses() {
		#[
			ArrayExtensions,
			BigDecimalExtensions,
			BigIntegerExtensions,
			BooleanExtensions,
			ByteExtensions,
			CharacterExtensions,
			ComparableExtensions,
			DoubleExtensions,
			FloatExtensions,
			IntegerExtensions,
			LongExtensions,
			ObjectExtensions,
			ShortExtensions,
			StringExtensions,
			JbaseBooleanExtensions
		]
	}

}
