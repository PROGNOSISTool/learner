package util.learnlib;

import java.util.List;

import net.automatalib.words.Word;

public class WordConverter {
	public static <I> Word<I> toWord(List<I> symbolList) {
		return Word.fromList(symbolList);
	}

	public static <I> List<I> toSymbolList(Word<I> word) {
		return word.asList();
	}
}
