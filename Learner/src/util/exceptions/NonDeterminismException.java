package util.exceptions;

import de.ls5.jlearn.abstractclasses.LearningException;
import de.ls5.jlearn.interfaces.Word;

public class NonDeterminismException extends LearningException {
	protected final Word input;
	
	public NonDeterminismException(String msg, Word input) {
		super(msg);
		this.input = input;
	}
	
	public NonDeterminismException(Word input) {
		this.input = input;
	}

	/**
	 * The full input for which the non-determinism was observed
	 * @return
	 */
	public Word getInput() {
		return this.input;
	}
}
