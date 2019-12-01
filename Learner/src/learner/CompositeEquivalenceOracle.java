package learner;
import de.ls5.jlearn.interfaces.Automaton;
import de.ls5.jlearn.interfaces.EquivalenceOracle;
import de.ls5.jlearn.interfaces.EquivalenceOracleOutput;
import de.ls5.jlearn.interfaces.Oracle;

public class CompositeEquivalenceOracle implements EquivalenceOracle{

	private EquivalenceOracle[] eqOracles;

	public CompositeEquivalenceOracle(EquivalenceOracle ... oracles) {
		this.eqOracles = oracles;
	}
	
	@Override
	public EquivalenceOracleOutput findCounterExample(Automaton hyp) {
		EquivalenceOracleOutput eqOutput = null;
		for (EquivalenceOracle eqOracle : eqOracles) { 
			eqOutput = eqOracle.findCounterExample(hyp);
			if (eqOutput != null) {
				break;
			}
		}
		return eqOutput;
	}

	@Override
	public void setOracle(Oracle testOracle) {
		for (EquivalenceOracle eqOracle : eqOracles) {
			eqOracle.setOracle(testOracle);
		}
	}

}
