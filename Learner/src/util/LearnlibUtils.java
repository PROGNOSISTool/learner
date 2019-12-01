package util;

import java.util.List;

import de.ls5.jlearn.interfaces.Symbol;
import de.ls5.jlearn.interfaces.Word;
import de.ls5.jlearn.shared.SymbolImpl;
import de.ls5.jlearn.shared.WordImpl;

public class LearnlibUtils {
	
	public static Word symbolsToWords(String... symbolStrings) {
		SymbolImpl[] symbols = new SymbolImpl[symbolStrings.length];
		for (int i = 0; i < symbolStrings.length; i++) {
			symbols[i] = new SymbolImpl(symbolStrings[i]);
		}
		return new WordImpl(symbols);
	}
	
	public static Word symbolsToWords(List<String> symbolStrings) { 
		return symbolsToWords(symbolStrings.toArray(new String[symbolStrings.size()]));
	}
	
	public static Word symbolsToWord(List<Symbol> symbols) {
		return new WordImpl(symbols.toArray(new Symbol[symbols.size()]));
	}
}
