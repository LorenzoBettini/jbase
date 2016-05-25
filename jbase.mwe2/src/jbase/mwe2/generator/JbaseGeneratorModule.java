package jbase.mwe2.generator;

import org.eclipse.xtext.xtext.generator.DefaultGeneratorModule;
import org.eclipse.xtext.xtext.generator.xbase.XbaseUsageDetector;

import jbase.mwe2.generator.xbase.JbaseXbaseUsageDetector;

public class JbaseGeneratorModule extends DefaultGeneratorModule {
	public Class<? extends XbaseUsageDetector> bindXbaseUsageDetector() {
		return JbaseXbaseUsageDetector.class;
	}
}
