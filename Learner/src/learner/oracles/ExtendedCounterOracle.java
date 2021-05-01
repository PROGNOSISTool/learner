package learner.oracles;

import de.learnlib.api.logging.LearnLogger;
import de.learnlib.api.oracle.MembershipOracle;
import de.learnlib.api.query.Query;
import de.learnlib.filter.statistic.oracle.CounterOracle;
import net.automatalib.words.Word;

import java.util.Collection;

public class ExtendedCounterOracle extends CounterOracle<String, Word<String>> {
	LearnLogger logger;

	public ExtendedCounterOracle(LearnLogger logger, MembershipOracle<String, Word<String>> nextOracle, String name) {
		super(nextOracle, name);
		this.logger = logger;
	}

	@Override
	public void processQueries(Collection<? extends Query<String, Word<String>>> queries) {
		super.processQueries(queries);
		logger.info(super.getCounter().getName() + ": " + super.getCount());
	}
}
