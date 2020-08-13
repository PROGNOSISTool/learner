package util.exceptions;

import net.automatalib.words.Word;

/**
 * Contains the full input for which non-determinism was observed, as well as the full new output
 * and the (possibly shorter) old output with which it disagrees
 */
public class CacheInconsistencyException extends NonDeterminismException {
	private static final long serialVersionUID = 9265414L;
	private final Word<String> oldOutput, newOutput;

	public CacheInconsistencyException(Word<String> input, Word<String> oldOutput, Word<String> newOutput) {
		super(input);
		this.oldOutput = oldOutput;
		this.newOutput = newOutput;
	}

	public CacheInconsistencyException(String message, Word<String> input, Word<String> oldOutput, Word<String> newOutput) {
		super(message, input);
		this.oldOutput = oldOutput;
		this.newOutput = newOutput;
	}


	/**
	 * The shortest cached output word which does not correspond with the new output
	 * @return
	 */
	public Word<String> getOldOutput() {
		return this.oldOutput;
	}

	/**
	 * The full new output word
	 * @return
	 */
	public Word<String> getNewOutput() {
		return this.newOutput;
	}

	/**
	 * The shortest sublist of the input word which still shows non-determinism
	 * @return
	 */
	public Word<String> getShortestInconsistentInput() {
	    int indexOfInconsistency = 0;
	    while (oldOutput.getSymbol(indexOfInconsistency).equals(newOutput.getSymbol(indexOfInconsistency))) {
			indexOfInconsistency++;
		}
		return this.input.subWord(0, indexOfInconsistency);
	}

	@Override
	public String toString() {
		return "full input:\n" + this.input + "\nfull new output:\n" + this.newOutput + "\nold output:\n" + this.oldOutput;
	}
}
