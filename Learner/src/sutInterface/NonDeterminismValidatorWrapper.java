package sutInterface;

import de.learnlib.api.exception.SULException;
import de.learnlib.api.oracle.MembershipOracle;
import learner.Main;

import net.automatalib.words.Word;
import sutInterface.quic.NonDeterminismTreePruner;
import util.Counter;
import util.Log;
import util.ObservationTree;
import util.exceptions.CorruptedLearningException;
import util.exceptions.CacheInconsistencyException;
import util.exceptions.NonDeterminismException;

public class NonDeterminismValidatorWrapper extends NonDeterminismTreePruner {
    private final int numberTries;
    private final MembershipOracle<String, Word<String>> afterCache;

    public NonDeterminismValidatorWrapper(int numberTries, ObservationTree tree, MembershipOracle<String, Word<String>> oracle, MembershipOracle<String, Word<String>> afterCache) {
    	super(tree, oracle);
        this.numberTries = numberTries;
        this.afterCache = afterCache;
    }

    @Override
    public Word<String> answerQuery(Word<String> word) {
		return oracle.answerQuery(word);
    }

    private Word<String> recover(Word<String> word, NonDeterminismException nonDet) throws SULException {
    	if (numberTries <= 0) {
    		throw nonDet;
    	}
        Log.err("Rerunning word which caused non-determinism: \n" + word);
    	Counter<Word<String>> outputs = new Counter<>();
    	for (int i = 0; i < numberTries; i++) {
    		Word<String> output2;
			output2 = afterCache.answerQuery(word);
    		outputs.count(output2);
    	}
    	Word<String> recoveredOutput = concludeRecoveryTrace(outputs);
    	if (recoveredOutput != null) {
    		Log.err("Recovered from non-determinism");
    		if (nonDet instanceof CacheInconsistencyException) {
	    		Word<String> oldOutput = ((CacheInconsistencyException)nonDet).getOldOutput();
	    		// if the old and recovered observation are not consistent, you have to abort learning (wrong info was
	    		// then previously passed to the learner with the old observation)
	    		if  (!recoveredOutput.subWord(0, oldOutput.size()).equals(oldOutput)) {
	    			Log.err("Traces for checking non-determinism are consistent with each other:\n" + outputs +
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
    protected Word<String> concludeRecoveryTrace(Counter<Word<String>> recoveryObservations) {
    	if (recoveryObservations.getObjectsCounted() == 1) {
    		return recoveryObservations.getMostFrequent();
    	} else {
    		return null;
    	}
    }
}
