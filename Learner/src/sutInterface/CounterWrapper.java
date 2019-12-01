package sutInterface;

import de.ls5.jlearn.abstractclasses.LearningException;
import de.ls5.jlearn.interfaces.Oracle;
import de.ls5.jlearn.interfaces.Symbol;
import de.ls5.jlearn.interfaces.Word;
import util.Container;

/**
 * Counts the number of queries
 */
public class CounterWrapper implements Oracle {
	private static final long serialVersionUID = 8252274170967533026L;
	private final Container<Integer> counter;
	private final boolean onQuery, onReset;
	private final Oracle oracle;
	
	/**
	 * Counts input queries, resets, or both, that are passed to the oracle, and adds them to the counter
	 * Assumes a normal an oracle endpoint that resets once at least for every query
	 * @param oracle
	 * @param counter
	 * @param onQuery
	 * @param onReset
	 */
	public CounterWrapper(Oracle oracle, Container<Integer> counter, boolean onQuery, boolean onReset) {
		this.counter = counter;
		this.onQuery = onQuery;
		this.onReset = onReset;
		this.oracle = oracle;
	}

	@Override
	public Word processQuery(Word input) throws LearningException {
		synchronized(counter) {
			if (onReset) {
				for (Symbol symbol : input.getSymbolList()) {
					if (symbol.toString().equals("reset")) {
						counter.value++;
					}
				}
			}
			// assumption: endpoint oracle resets every query
			if (onReset) {
				counter.value++;
			}
			if (onQuery) {
				counter.value++;
			}
		}
		return oracle.processQuery(input);
	}
}
