package jbase.mwe2.generator.xbase

import org.eclipse.xtext.Grammar
import org.eclipse.xtext.xtext.generator.xbase.XbaseUsageDetector

import static extension org.eclipse.xtext.xtext.generator.util.GrammarUtil2.*

class JbaseXbaseUsageDetector extends XbaseUsageDetector {

	override boolean inheritsXbase(Grammar grammar) {
		grammar.inherits('jbase.Jbase')
	}

}
