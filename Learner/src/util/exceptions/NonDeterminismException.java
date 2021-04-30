package util.exceptions;

import net.automatalib.words.Word;

public class NonDeterminismException extends CorruptedLearningException {
	protected final Word<String> input;

	public NonDeterminismException(Word<String> input) {
		super();
		this.input = input;
		System.out.println("NIE: INPUT: " + this.input.toString());
	}

	/**
	 * The full input for which the non-determinism was observed
	 * @return Word<String>
	 */
	public Word<String> getInput() {
		return this.input;
	}
}
