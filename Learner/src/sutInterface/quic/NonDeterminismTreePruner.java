package sutInterface.quic;

import de.ls5.jlearn.abstractclasses.LearningException;
import de.ls5.jlearn.interfaces.Oracle;
import de.ls5.jlearn.interfaces.Word;
import learner.Main;
import util.ObservationTree;
import util.exceptions.CacheInconsistencyException;

public class NonDeterminismTreePruner implements Oracle {
	private static final long serialVersionUID = 315060501453418373L;
	protected final Oracle oracle;
	protected final ObservationTree tree;

	public NonDeterminismTreePruner(ObservationTree tree, Oracle oracle) {
		this.oracle = oracle;
		this.tree = tree;
	}

	@Override
    public Word processQuery(Word word) throws LearningException {
        try {
            return oracle.processQuery(word);
        } catch (CacheInconsistencyException nonDet) {
        	prune(nonDet);
            throw nonDet;
    	}
    }

	protected void prune(CacheInconsistencyException cacheException) {
		Main.writeCacheTree(tree, false);
        tree.remove(cacheException.getShortestInconsistentInput());
	}
}
