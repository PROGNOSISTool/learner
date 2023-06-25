package learner.oracles;

import de.learnlib.api.query.Query;
import de.learnlib.oracle.membership.SULOracle;
import learner.SocketSUL;
import net.automatalib.words.Word;

import java.util.Collection;

public class SocketSULOracle extends SULOracle<String, String> {
	SocketSUL sul;

	public SocketSULOracle(SocketSUL sul) {
		super(sul);
		this.sul = sul;
	}

	@Override
	public Word<String> answerQuery(Word<String> input) {
		sul.pre();
		Word<String> output = Word.fromList(sul.step(input.asList()));
		sul.post();
		return output;
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
