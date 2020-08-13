package sutInterface;

import de.learnlib.api.oracle.MembershipOracle;
import de.learnlib.api.query.Query;
import net.automatalib.words.Word;
import util.ObservationTree;
import util.StringColorizer;

import java.util.Collection;

public class CacheReaderOracle implements MembershipOracle<String, Word<String>> {
	private static final int MAX_VERBOSE_CACHE_HITS = 3;

	private static final long serialVersionUID = 4600277L;
	private final MembershipOracle<String, Word<String>> oracle;
	private final ObservationTree tree;
	private int cacheHitsInSuccession = 0;

	public CacheReaderOracle(ObservationTree tree, MembershipOracle<String, Word<String>> oracle) {
		this.oracle = oracle;
		this.tree = tree;
	}

	@Override
	public Word<String> answerQuery(Word<String> input) {
		Word<String> cachedOutput = tree.getObservation(input);
		if (cachedOutput != null) {
			if (cacheHitsInSuccession < MAX_VERBOSE_CACHE_HITS) {
				System.out.println(StringColorizer.toColor("Cache hit for " + input.asList() + " -> " + cachedOutput.asList(), StringColorizer.TextColor.CYAN));
			}
			cacheHitsInSuccession++;
			return cachedOutput;
		} else {
			if (cacheHitsInSuccession > MAX_VERBOSE_CACHE_HITS) {
				System.out.println(StringColorizer.toColor("...and "  + (cacheHitsInSuccession - MAX_VERBOSE_CACHE_HITS) + " more cache hits", StringColorizer.TextColor.CYAN));
			}
			cacheHitsInSuccession = 0;
			return oracle.answerQuery(input);
		}
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
