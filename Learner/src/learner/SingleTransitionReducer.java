package learner;

import java.util.ArrayList;
import java.util.List;

import util.LearnlibUtils;

import de.ls5.jlearn.abstractclasses.LearningException;
import de.ls5.jlearn.equivalenceoracles.EquivalenceOracleOutputImpl;
import de.ls5.jlearn.interfaces.Automaton;
import de.ls5.jlearn.interfaces.EquivalenceOracleOutput;
import de.ls5.jlearn.interfaces.Oracle;
import de.ls5.jlearn.interfaces.Symbol;
import de.ls5.jlearn.interfaces.Word;

public class SingleTransitionReducer {
	private Oracle testOracle;
	public SingleTransitionReducer (Oracle queryOracle) {
		testOracle = queryOracle;
	}
	
	public EquivalenceOracleOutput reducedCounterexample( EquivalenceOracleOutput ce, Automaton hyp) throws LearningException {
		List<Symbol> ceInputs = ce.getCounterExample().getSymbolList();
		EquivalenceOracleOutput oracleOutput = null;
		
		for (int i = 0; i < ceInputs.size(); i++) {
			Symbol symbol = ceInputs.get(i);
			List<Symbol> ceTest = new ArrayList<Symbol>(ceInputs);
			ceTest.remove(symbol);
			oracleOutput = isStillCe(ceTest, hyp);
			if (oracleOutput != null) {
				ceInputs = ceTest;
			}
		}
		
		return oracleOutput != null? oracleOutput : ce;
	}

	private EquivalenceOracleOutput isStillCe(List<Symbol> ceTest, Automaton hyp) throws LearningException{
		EquivalenceOracleOutput newCe = null;
		Word testWord = LearnlibUtils.symbolsToWord(ceTest);
		Word sutOutput = testOracle.processQuery(testWord);
		Word hypOutput = hyp.getTraceOutput(testWord);
		if (!sutOutput.equals(hypOutput)) {
			newCe = getMinimalCe(ceTest, hypOutput, sutOutput);
		}
		return newCe;
	}

	private EquivalenceOracleOutput getMinimalCe(List<Symbol> ceTest,
			Word hypOutput, Word sutOutput) {
		List<Symbol> newCe = new ArrayList<Symbol>();
		List<Symbol> sutOutputSyms = sutOutput.getSymbolList();
		List<Symbol> hypOutputSyms = hypOutput.getSymbolList();
		for (int i = 0; i < hypOutput.getSymbolArray().length; i ++) {
			newCe.add(ceTest.get(i));
			if (!hypOutputSyms.get(i).equals(sutOutputSyms.get(i))) {
				break;
			}
		}
		List<Symbol> newOracleOutputs = sutOutputSyms.subList(0, newCe.size());
		
		return new EquivalenceOracleOutputImpl(LearnlibUtils.symbolsToWord(newCe), LearnlibUtils.symbolsToWord(newOracleOutputs));
	}
	
}
