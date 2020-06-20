package util;

import java.util.List;

import net.automatalib.words.Word;
import net.automatalib.words.impl.Symbol;

public class LearnlibUtils {

	public static Word<String> symbolsToWords(String... symbolStrings) {
		return Word.fromArray(symbolStrings, 0, symbolStrings.length);
	}

	public static Word<String> symbolsToWords(List<String> symbolStrings) {
		return Word.fromList(symbolStrings);
	}

	public static Word<Symbol> symbolsToWord(List<Symbol> symbols) {
		return Word.fromList(symbols);
	}
}
