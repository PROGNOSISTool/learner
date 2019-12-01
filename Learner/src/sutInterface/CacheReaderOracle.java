package sutInterface;

import util.ObservationTree;
import util.StringColorizer;
import de.ls5.jlearn.abstractclasses.LearningException;
import de.ls5.jlearn.interfaces.Oracle;
import de.ls5.jlearn.interfaces.Word;

public class CacheReaderOracle implements Oracle {
	private static final int MAX_VERBOSE_CACHE_HITS = 3;
	
	private static final long serialVersionUID = 4600277L;
	private final Oracle oracle;
	private final ObservationTree tree;
	private int cacheHitsInSuccession = 0;
	
	public CacheReaderOracle(ObservationTree tree, Oracle oracle) {
		this.oracle = oracle;
		this.tree = tree;
	}
	
	@Override
	public Word processQuery(Word input) throws LearningException {
		Word cachedOutput = tree.getObservation(input);
		if (cachedOutput != null) {
			if (cacheHitsInSuccession < MAX_VERBOSE_CACHE_HITS) {
				System.out.println(StringColorizer.toColor("Cache hit for " + input.getSymbolList() + " -> " + cachedOutput.getSymbolList(), StringColorizer.TextColor.CYAN));
			}
			cacheHitsInSuccession++;
			return cachedOutput;
		} else {
			if (cacheHitsInSuccession > MAX_VERBOSE_CACHE_HITS) {
				System.out.println(StringColorizer.toColor("...and "  + (cacheHitsInSuccession - MAX_VERBOSE_CACHE_HITS) + " more cache hits", StringColorizer.TextColor.CYAN));
			}
			cacheHitsInSuccession = 0;
			return oracle.processQuery(input);
		}
	}
}
