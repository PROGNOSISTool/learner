package util.exceptions;

import de.learnlib.api.exception.SULException;
import net.automatalib.words.Word;

public class NonDeterminismException extends SULException {
	protected final Word<?> input;

	public NonDeterminismException(String msg, Word<?> input) {
		super(new Throwable(msg));
		this.input = input;
	}

	public NonDeterminismException(Word<?> input) {
		super(new Throwable());
		this.input = input;
	}

	/**
	 * The full input for which the non-determinism was observed
	 * @return
	 */
	public Word<?> getInput() {
		return this.input;
	}
}
