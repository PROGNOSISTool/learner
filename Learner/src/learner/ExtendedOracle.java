package learner;

import util.exceptions.CacheInconsistencyException;
import de.ls5.jlearn.interfaces.Oracle;

public interface ExtendedOracle extends Oracle {
	/**
	 * Sends a single input String and returns an output string. This is done in the same run as
	 * the query.
	 * @throws CacheInconsistencyException
	 */
	public String sendInput(String input);
}
