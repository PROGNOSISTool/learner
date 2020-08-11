package sutInterface;

import java.util.List;

import util.Counter;
import util.Log;
import util.exceptions.NonDeterminismException;
import util.learnlib.WordConverter;
import de.ls5.jlearn.abstractclasses.LearningException;
import de.ls5.jlearn.interfaces.Oracle;
import de.ls5.jlearn.interfaces.Symbol;
import de.ls5.jlearn.interfaces.Word;

public class ProbablisticOracle implements Oracle {
	private static final long serialVersionUID = 165336527L;
	private final Oracle oracle;
	private final int minimumAttempts, maximumAttempts;
	private final double minimumFraction;

	public ProbablisticOracle(Oracle oracle, int minimumAttempts, double minimumFraction, int maximumAttempts) {
		this.oracle = oracle;
		if (minimumAttempts > maximumAttempts) {
			throw new RuntimeException("minimum number of attempts should not be greater than maximum");
		}
		if (minimumFraction > 1 || minimumFraction < 0.5) {
			throw new RuntimeException("Minimum fraction should be in interval [0.5, 1]");
		}
		this.minimumAttempts = minimumAttempts;
		this.minimumFraction = minimumFraction;
		this.maximumAttempts = maximumAttempts;
	}

	@Override
	public Word processQuery(Word inputWord) throws LearningException {
		List<Symbol> input = inputWord.getSymbolList();
		Counter<List<Symbol>> responseCounter = new Counter<List<Symbol>>();
		boolean finished = false;
		boolean firstAttempt = true;
		do {

			if (responseCounter.getTotalNumber() >= this.maximumAttempts) {
				Log.err("Non-determinism found by probablistic oracle for input\n" + inputWord + "\noutputs:\n" + responseCounter);
			    if (firstAttempt) {
                    try {
                        Log.err("Sleeping 1 minutes to check if the non-det will clear");
                        Thread.sleep(60000);
                        firstAttempt = false;
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    responseCounter.reset();
    			}
			    else {
    				throw new NonDeterminismException(inputWord);
			    }
			}
			List<Symbol> output = this.oracle.processQuery(inputWord).getSymbolList();
			responseCounter.count(output);
			finished = responseCounter.getTotalNumber() >= this.minimumAttempts &&
					(responseCounter.getHighestFrequencyFraction() >= this.minimumFraction  || responseCounter.getObjectsCounted() == 1);
		} while (!finished);

		List<Symbol> mostFrequent = responseCounter.getMostFrequent();
		if (responseCounter.getObjectsCounted() > 1) {
			Log.err("Non-determinism detected on input\n" + input + "\nResponses:\n" + responseCounter + "\naccepted most frequent:\n" + mostFrequent);
		} else {
			Log.err("Concluded unanimously in " + responseCounter.getTotalNumber() + " attempts:\n" + mostFrequent);
		}
		return WordConverter.toWord(mostFrequent);
	}
}
