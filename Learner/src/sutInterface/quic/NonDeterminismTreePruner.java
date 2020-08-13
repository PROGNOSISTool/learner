package sutInterface.quic;

import de.learnlib.api.oracle.MembershipOracle;
import de.learnlib.api.query.Query;
import learner.Main;
import net.automatalib.words.Word;
import util.ObservationTree;
import util.exceptions.CacheInconsistencyException;

import java.util.Collection;

public class NonDeterminismTreePruner implements MembershipOracle<String, Word<String>> {
	protected final MembershipOracle<String, Word<String>> oracle;
	protected final ObservationTree tree;

	public NonDeterminismTreePruner(ObservationTree tree, MembershipOracle<String, Word<String>> oracle) {
		this.oracle = oracle;
		this.tree = tree;
	}

	@Override
    public Word<String> answerQuery(Word<String> word) {
		return oracle.answerQuery(word);
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

	protected void prune(CacheInconsistencyException cacheException) {
		Main.writeCacheTree(tree, false);
        tree.remove(cacheException.getShortestInconsistentInput().asList());
	}
}
