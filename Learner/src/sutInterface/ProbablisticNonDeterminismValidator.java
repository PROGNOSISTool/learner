package sutInterface;

import util.Counter;
import util.ObservationTree;
import de.ls5.jlearn.interfaces.Oracle;
import de.ls5.jlearn.interfaces.Word;

public class ProbablisticNonDeterminismValidator extends NonDeterminismValidatorWrapper {
	private final double minimumFraction;
	
	public ProbablisticNonDeterminismValidator(int numberTries, double minimumFraction,
			ObservationTree tree, Oracle oracle, Oracle afterCache) {
		super(numberTries, tree, oracle, afterCache);
		this.minimumFraction = minimumFraction;
		if (this.minimumFraction <= 0.5) {
			throw new RuntimeException("probablistic oracle should check for a >50% match, two results may be obtained otherwise");
		}
	}
	
	/**
     * When recovering and trying out some traces, which trace to conclude from the given observations.
     * Concludes a trace that meets the , null otherwise
     * @param recoveryObservations
     * @return the single observed trace, or null otherwise
     */
	@Override
    protected Word concludeRecoveryTrace(Counter<Word> recoveryObservations) {
    	if (recoveryObservations.getObjectsCounted() == 1 || recoveryObservations.getHighestFrequencyFraction() >= minimumFraction) {
    		return recoveryObservations.getMostFrequent();
    	} else {
    		return null;
    	}
    }
}
