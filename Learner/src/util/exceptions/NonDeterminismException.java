package util.exceptions;

import de.learnlib.api.exception.SULException;
import net.automatalib.words.Word;

public class NonDeterminismException extends CorruptedLearningException {
	protected final Word<String> input;

	public NonDeterminismException(String msg, Word<String> input) {
		super(msg);
		this.input = input;
		System.out.println("NIE: INPUT: " + this.input.toString());
	}

	public NonDeterminismException(Word<String> input) {
		super();
		this.input = input;
		System.out.println("NIE: INPUT: " + this.input.toString());
	}

	public NonDeterminismException(Word<String> prefix, Word<String> suffix) {
		super();
		this.input = prefix.concat(suffix);
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
