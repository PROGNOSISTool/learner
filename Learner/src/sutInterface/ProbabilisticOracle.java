package sutInterface;

import java.util.Collection;
import java.util.List;

import de.learnlib.api.oracle.MembershipOracle;
import de.learnlib.api.query.Query;
import net.automatalib.words.Word;
import util.Counter;
import util.Log;
import util.exceptions.NonDeterminismException;

public class ProbabilisticOracle implements MembershipOracle<String, Word<String>> {
	private final MembershipOracle<String, Word<String>> oracle;
	private final int minimumAttempts, maximumAttempts;
	private final double minimumFraction;

	public ProbabilisticOracle(MembershipOracle<String, Word<String>> oracle, int minimumAttempts, double minimumFraction, int maximumAttempts) {
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
	public Word<String> answerQuery(Word<String> inputWord) {
		List<String> inputs = inputWord.asList();
		Counter<List<String>> responseCounter = new Counter<List<String>>();
		boolean finished = false;
		boolean firstAttempt = true;
		do {

			if (responseCounter.getTotalNumber() >= this.maximumAttempts) {
				Log.err("Non-determinism found by probablistic oracle for input\n" + inputs + "\noutputs:\n" + responseCounter);
				if (firstAttempt) {
					try {
						Log.err("Sleeping 1 minute to check if the non-det will clear");
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
			List<String> output = this.oracle.answerQuery(inputWord).asList();
			responseCounter.count(output);
			finished = responseCounter.getTotalNumber() >= this.minimumAttempts &&
					(responseCounter.getHighestFrequencyFraction() >= this.minimumFraction  || responseCounter.getObjectsCounted() == 1);
		} while (!finished);

		List<String> mostFrequent = responseCounter.getMostFrequent();
		if (responseCounter.getObjectsCounted() > 1) {
			Log.err("Non-determinism detected on input\n" + inputs + "\nResponses:\n" + responseCounter + "\naccepted most frequent:\n" + mostFrequent);
		} else {
			Log.err("Concluded unanimously in " + responseCounter.getTotalNumber() + " attempts:\n" + mostFrequent);
		}
		return Word.fromList(mostFrequent);
	}

	@Override
	public Word<String> answerQuery(Word<String> prefix, Word<String> suffix) {
		return answerQuery(prefix.concat(suffix)).suffix(suffix.length());
	}

	@Override
	public void processQuery(Query<String, Word<String>> query) {
		query.answer(answerQuery(query.getPrefix(), query.getSuffix()));
	}

	@Override
	public void processQueries(Collection<? extends Query<String, Word<String>>> collection) {
		for (Query<String, Word<String>> query : collection) {
			processQuery(query);
		}
	}
}
