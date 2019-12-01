package sutInterface;

import java.util.HashSet;
import java.util.Set;

import de.ls5.jlearn.abstractclasses.LearningException;
import de.ls5.jlearn.interfaces.Oracle;
import de.ls5.jlearn.interfaces.Symbol;
import de.ls5.jlearn.interfaces.Word;
import util.Container;

/**
 * Counts the number of unique queries, i.e. records the queries so far and
 * compares every new query to determine whether to count it
 */
public class UniqueQueryCounterWrapper implements Oracle {
	private static final long serialVersionUID = -6252687709933603768L;
	private final Container<Integer> counter;
	private final Oracle oracle;
	private final Set<Word> inputs = new HashSet<>();
	
	/**
	 * Counts input unique queries that are passed to the oracle, and adds them to the counter
	 * @param oracle
	 * @param counter
	 */
	public UniqueQueryCounterWrapper(Oracle oracle, Container<Integer> counter) {
		this.counter = counter;
		this.oracle = oracle;
	}

	@Override
	public Word processQuery(Word input) throws LearningException {
		synchronized(counter) {
			if (!inputs.contains(input)) {
				counter.value++;
				inputs.add(input);
			}
		}
		return oracle.processQuery(input);
	}
}
