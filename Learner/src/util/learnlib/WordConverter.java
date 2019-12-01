package util.learnlib;

import java.util.LinkedList;
import java.util.List;

import de.ls5.jlearn.interfaces.Symbol;
import de.ls5.jlearn.interfaces.Word;
import de.ls5.jlearn.shared.WordImpl;

public class WordConverter {
	public static Word toWord(List<Symbol> symbolList) {
		Symbol[] outputArray = new Symbol[symbolList.size()];
		int i = 0;
		for (Symbol s : symbolList) {
			outputArray[i++] = s;
		}
		return new WordImpl(outputArray);
	}
	
	public static List<Symbol> toSymbolList(Word word) {
		return new LinkedList<>(word.getSymbolList());
	}
}
