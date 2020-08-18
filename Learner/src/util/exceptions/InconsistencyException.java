package util.exceptions;

import net.automatalib.words.Word;

public class InconsistencyException extends Exception {
	private static final long serialVersionUID = 14324L;
	protected final Word<String> oldWord, newWord;

	public InconsistencyException(Word<String> oldWord, Word<String> newWord) {
		super("previously encountered\n" +
				oldWord + "\nNow encountering\n" + newWord);
		this.oldWord = oldWord;
		this.newWord = newWord;
	}

	public Word<String> getOldWord() {
		return this.oldWord;
	}

	public Word<String> getNewWord() {
		return this.newWord;
	}
}
