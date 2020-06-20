package util.exceptions;

import util.learnlib.WordConverter;
import net.automatalib.words.Word;

/**
 * Contains the full input for which non-determinism was observed, as well as the full new output
 * and the (possibly shorter) old output with which it disagrees
 */
public class CacheInconsistencyException extends NonDeterminismException {
	private static final long serialVersionUID = 9265414L;
	private final Word<?> oldOutput, newOutput;

	public CacheInconsistencyException(Word<?> input, Word<?> oldOutput, Word<?> newOutput) {
		super(input);
		this.oldOutput = oldOutput;
		this.newOutput = newOutput;
	}

	public CacheInconsistencyException(String message, Word<?> input, Word<?> oldOutput, Word<?> newOutput) {
		super(message, input);
		this.oldOutput = oldOutput;
		this.newOutput = newOutput;
	}


	/**
	 * The shortest cached output word which does not correspond with the new output
	 * @return
	 */
	public Word<?> getOldOutput() {
		return this.oldOutput;
	}

	/**
	 * The full new output word
	 * @return
	 */
	public Word<?> getNewOutput() {
		return this.newOutput;
	}

	/**
	 * The shortest sublist of the input word which still shows non-determinism
	 * @return
	 */
	public Word<?> getShortestInconsistentInput() {
	    int indexOfInconsistency = 0;
	    while (oldOutput.getSymbol(indexOfInconsistency).equals(newOutput.getSymbol(indexOfInconsistency))) {
	        indexOfInconsistency ++;
	    }
		return WordConverter.toWord(WordConverter.toSymbolList(this.input).subList(0, indexOfInconsistency));
	}

	@Override
	public String toString() {
		return "full input:\n" + this.input + "\nfull new output:\n" + this.newOutput + "\nold output:\n" + this.oldOutput;
	}
}
