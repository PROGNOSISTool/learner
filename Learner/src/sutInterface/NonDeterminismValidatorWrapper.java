package sutInterface;

import java.util.List;

import learner.Main;

import sutInterface.quic.NonDeterminismTreePruner;
import util.Counter;
import util.Log;
import util.ObservationTree;
import util.exceptions.CorruptedLearningException;
import util.exceptions.CacheInconsistencyException;
import util.exceptions.NonDeterminismException;
import util.learnlib.WordConverter;
import de.ls5.jlearn.abstractclasses.LearningException;
import de.ls5.jlearn.interfaces.Oracle;
import de.ls5.jlearn.interfaces.Symbol;
import de.ls5.jlearn.interfaces.Word;

public class NonDeterminismValidatorWrapper extends NonDeterminismTreePruner {
    private static final long serialVersionUID = 1L;
    private int numberTries;
    private final Oracle afterCache;

    public NonDeterminismValidatorWrapper(int numberTries, ObservationTree tree, Oracle oracle, Oracle afterCache) {
    	super(tree, oracle);
        this.numberTries = numberTries;
        this.afterCache = afterCache;
    }

    @Override
    public Word processQuery(Word word) throws LearningException {
        try {
            return oracle.processQuery(word);
        } catch (NonDeterminismException nonDet) {
        	return recover(word, nonDet);
    	}
    }

    private Word recover(Word word, NonDeterminismException nonDet) throws LearningException {
    	if (numberTries <= 0) {
    		throw nonDet;
    	}
        Log.err("Rerunning word which caused non-determinism: \n" + word);
    	Counter<Word> outputs = new Counter<>();
    	for (int i = 0; i < numberTries; i++) {
    		Word output2;
			output2 = afterCache.processQuery(word);
    		outputs.count(output2);
    	}
    	Word recoveredOutput = concludeRecoveryTrace(outputs);
    	if (recoveredOutput != null) {
    		Log.err("Recovered from non-determinism");
    		if (nonDet instanceof CacheInconsistencyException) {
	    		List<Symbol> oldOutput = WordConverter.toSymbolList(((CacheInconsistencyException)nonDet).getOldOutput());
	    		// if the old and recovered observation are not consistent, you have to abort learning (wrong info was
	    		// then previously passed to the learner with the old observation)
	    		if (!WordConverter.toSymbolList(recoveredOutput).subList(0, oldOutput.size()).equals(oldOutput)) {
	    			Log.err("Traces for checking non-determinism are consistent with eachother:\n" + outputs +
	    					"Recovered output is inconsistent with the old output, so a wrong output was passed to the learner earlier\n" +
	    					"Aborting learning, but written the new observation to the tree");
	                Main.writeCacheTree(tree, false);
	                tree.addObservation(word, recoveredOutput, true);
	    			throw new CorruptedLearningException();
	    		}
    		}
    		tree.addObservation(word, recoveredOutput);
    		return recoveredOutput;
    	} else {
    		if (nonDet instanceof CacheInconsistencyException) {
    			this.prune((CacheInconsistencyException) nonDet);
    		}
    		Log.err("Could not recover from persistent non-determinism for input:\n" + word + "\noutputs:\n" + outputs);
    		throw nonDet;
    	}
    }

    /**
     * When recovering and trying out some traces, which trace to conclude from the given observations.
     * Concludes a trace when that was the only trace observed, null otherwise
     * @param recoveryObservations
     * @return the single observed trace, or null otherwise
     */
    protected Word concludeRecoveryTrace(Counter<Word> recoveryObservations) {
    	if (recoveryObservations.getObjectsCounted() == 1) {
    		return recoveryObservations.getMostFrequent();
    	} else {
    		return null;
    	}
    }
}
