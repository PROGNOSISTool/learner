package sutInterface;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.ls5.jlearn.abstractclasses.LearningException;
import de.ls5.jlearn.interfaces.Oracle;
import de.ls5.jlearn.interfaces.Symbol;
import de.ls5.jlearn.interfaces.Word;
import de.ls5.jlearn.shared.SymbolImpl;
import de.ls5.jlearn.shared.WordImpl;

import util.LearnlibUtils;

/**
 * Oracle wrapper to check for non-determinism, and aborts the learner upon detection.
 * @author Ramon
 */
public class DeterminismCheckerOracleWrapper implements Oracle {
	private static final long serialVersionUID = 1L;
	private final Oracle oracle;
	private final Map<List<Symbol>, List<Symbol>> inputToOutput = new HashMap<>();
	
	public DeterminismCheckerOracleWrapper(Oracle oracle) {
		this.oracle = oracle;
	}

	@Override
	public Word processQuery(Word input) throws LearningException {
		Word output = this.oracle.processQuery(input);
		List<Symbol> inputList = input.getSymbolList();
		List<Symbol> outputList = output.getSymbolList();
		for (int length = 1; length <= inputList.size(); length++) {
			checkDeterminism(inputList.subList(0, length), outputList.subList(0, length));
		}
		return output;
	}
	
	private void checkDeterminism(List<Symbol> input, List<Symbol> output) throws LearningException {
		List<Symbol> previousOutput = inputToOutput.get(input);
		
		if (previousOutput == null) {
			inputToOutput.put(input, output);
		} else {
			if (!output.equals(previousOutput)) {
				throw new LearningException("Non-determinism detected:\ninput\n" + input
						+ "\npreviously encountered\n" + previousOutput +
						"\nnow encountering\n" + output) {
							private static final long serialVersionUID = 1L;};
			}
		}
	}
	
	public static void main(String[] args) {
		Word word = LearnlibUtils.symbolsToWords("aap", "noot", "mies");
		Word subWord = LearnlibUtils.symbolsToWords("aap", "noot", "wim");
		Oracle dummy = new Oracle() {
			private static final long serialVersionUID = 1L;
			int i = 0;
			@Override
			public Word processQuery(Word arg0) throws LearningException {
				Word result;
				if (i == 0) {
					result = LearnlibUtils.symbolsToWords("1", "2", "3");
				} else if (i == 1) {
					result = LearnlibUtils.symbolsToWords("1", "2", "3");
				} else {
					result = LearnlibUtils.symbolsToWords("1", "4", "3");
				}
				i++;
				return result;
			}
		};
		Oracle testOracle = new DeterminismCheckerOracleWrapper(dummy);
		try {
			testOracle.processQuery(word);
			testOracle.processQuery(word);
			try {
				testOracle.processQuery(subWord);
				System.out.println("failed, non-determinism not detected");
			} catch (LearningException e) {
				System.out.println("success, non-determinism detected");
				e.printStackTrace();
			}
		} catch (LearningException e) {
			System.out.println("failed too early on non-determinism-check:");
			e.printStackTrace();
		}
	}
}
