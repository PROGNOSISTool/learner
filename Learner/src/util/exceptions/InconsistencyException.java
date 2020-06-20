package util.exceptions;

import net.automatalib.words.Word;

public class InconsistencyException extends Exception {
	private static final long serialVersionUID = 14324L;
	public final Word<?> oldWord, newWord;

	public InconsistencyException(Word<?> oldWord, Word<?> newWord) {
		super("previously encountered\n" +
				oldWord + "\nNow encountering\n" + newWord);
		this.oldWord = oldWord;
		this.newWord = newWord;
	}

	public Word<?> getOldWord() {
		return this.oldWord;
	}

	public Word<?> getNewWord() {
		return this.newWord;
	}
}
