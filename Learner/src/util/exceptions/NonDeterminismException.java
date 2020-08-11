package util.exceptions;

import de.learnlib.api.exception.SULException;
import net.automatalib.words.Word;

public class NonDeterminismException extends SULException {
	protected final Word<?> input;

	public NonDeterminismException(String msg, Word<?> input) {
		super(new Throwable(msg));
		this.input = input;
		System.out.println("NIE: INPUT: " + this.input.toString());
	}

	public NonDeterminismException(Word<?> input) {
		super(new Throwable());
		this.input = input;
		System.out.println("NIE: INPUT: " + this.input.toString());
	}

	/**
	 * The full input for which the non-determinism was observed
	 * @return
	 */
	public Word<?> getInput() {
		return this.input;
	}
}
