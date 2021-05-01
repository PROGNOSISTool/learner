package learner.oracles;

import java.io.File;
import java.util.Collection;
import java.util.Collections;

import de.learnlib.api.oracle.MembershipOracle;
import de.learnlib.api.logging.LearnLogger;
import de.learnlib.api.query.Query;
import net.automatalib.words.Word;
import util.FileManager;

// Based on LogOracle from StateLearner by Joeri de Ruiter
public class LogOracle implements MembershipOracle<String, Word<String>> {
	private LearnLogger logger;
	private File file;
	private final MembershipOracle<String, Word<String>> queryOracle;

	public LogOracle(LearnLogger logger, MembershipOracle<String, Word<String>> oracle) {
		this.logger = logger;
		this.queryOracle = oracle;
	}

	public LogOracle(String filename, MembershipOracle<String, Word<String>> oracle) {
		this.file = FileManager.createFile(filename, true);
		this.queryOracle = oracle;
	}

    @Override
	public Word<String> answerQuery(Word<String> input) {
		Word<String> answer = queryOracle.answerQuery(input);
		String query = input.toString() + " / " + answer.toString();
		if (logger != null) {
			logger.logQuery(query);
		} else {
			FileManager.appendToFile(file, Collections.singletonList(query));
		}
		return answer;
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
