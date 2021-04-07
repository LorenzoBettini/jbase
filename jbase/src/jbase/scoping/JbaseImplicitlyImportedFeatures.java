package jbase.scoping;

import java.util.Collections;
import java.util.List;

import org.eclipse.xtext.xbase.lib.ArrayExtensions;
import org.eclipse.xtext.xbase.lib.BigDecimalExtensions;
import org.eclipse.xtext.xbase.lib.BigIntegerExtensions;
import org.eclipse.xtext.xbase.lib.BooleanExtensions;
import org.eclipse.xtext.xbase.lib.ByteExtensions;
import org.eclipse.xtext.xbase.lib.CharacterExtensions;
import org.eclipse.xtext.xbase.lib.ComparableExtensions;
import org.eclipse.xtext.xbase.lib.DoubleExtensions;
import org.eclipse.xtext.xbase.lib.FloatExtensions;
import org.eclipse.xtext.xbase.lib.IntegerExtensions;
import org.eclipse.xtext.xbase.lib.LongExtensions;
import org.eclipse.xtext.xbase.lib.ObjectExtensions;
import org.eclipse.xtext.xbase.lib.ShortExtensions;
import org.eclipse.xtext.xbase.lib.StringExtensions;
import org.eclipse.xtext.xbase.scoping.batch.ImplicitlyImportedFeatures;

import com.google.common.collect.Lists;

import jbase.lib.JbaseBooleanExtensions;

/**
 * Avoid all the default extension classes and methods.
 * 
 * @author Lorenzo Bettini
 */
public class JbaseImplicitlyImportedFeatures extends ImplicitlyImportedFeatures {
	@Override
	protected List<Class<?>> getStaticImportClasses() {
		return Collections.emptyList();
	}

	@Override
	protected List<Class<?>> getExtensionClasses() {
		return Lists.newArrayList(
			ArrayExtensions.class,
			BigDecimalExtensions.class,
			BigIntegerExtensions.class,
			BooleanExtensions.class,
			ByteExtensions.class,
			CharacterExtensions.class,
			ComparableExtensions.class,
			DoubleExtensions.class,
			FloatExtensions.class,
			IntegerExtensions.class,
			LongExtensions.class,
			ObjectExtensions.class,
			ShortExtensions.class,
			StringExtensions.class,
			JbaseBooleanExtensions.class
		);
	}
}
