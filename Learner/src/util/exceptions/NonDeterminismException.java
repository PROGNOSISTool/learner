package util.exceptions;

import de.learnlib.api.exception.SULException;
import net.automatalib.words.Word;

public class NonDeterminismException extends SULException {
	protected final Word<String> input;

	public NonDeterminismException(String msg, Word<String> input) {
		super(new Throwable(msg));
		this.input = input;
		System.out.println("NIE: INPUT: " + this.input.toString());
	}

	public NonDeterminismException(Word<String> input) {
		super(new Throwable());
		this.input = input;
		System.out.println("NIE: INPUT: " + this.input.toString());
	}

	public NonDeterminismException(Word<String> prefix, Word<String> suffix) {
		super(new Throwable());
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
