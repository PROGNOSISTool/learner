package learner;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import de.learnlib.api.exception.SULException;
import de.learnlib.api.oracle.EquivalenceOracle.MealyEquivalenceOracle;
import de.learnlib.api.query.DefaultQuery;
import net.automatalib.automata.transducers.MealyMachine;
import org.checkerframework.checker.nullness.qual.Nullable;
import util.LearnlibUtils;
import util.Log;

import net.automatalib.words.Word;

public class WordCheckingEquivalenceOracle<O> implements MealyEquivalenceOracle<String, O> {

	private List<Word<String>> wordsToCheck = new ArrayList<Word<String>>();
	private MembershipOracle<String, Word<O>> oracle;

	public WordCheckingEquivalenceOracle(MembershipOracle<String, Word<O>> oracle, List<List<String>> inputTracesToCheck) {
		this.wordsToCheck = parseTestWords(inputTracesToCheck);
		this.oracle = oracle;
	}

	private List<Word<String>> parseTestWords(List<List<String>> inputTracesToCheck) {
		List<Word<String>> testWords = new ArrayList<Word<String>>();
		for(List<String> traceToCheck : inputTracesToCheck) {
			Word<String> testWord = LearnlibUtils.symbolsToWords(traceToCheck);
			testWords.add(testWord);
		}
		return testWords;
	}

	@Override
	public @Nullable DefaultQuery<String, Word<O>> findCounterExample(MealyMachine<?, String, ?, O> hyp, Collection<? extends String> inputs) {
		DefaultQuery<String, Word<O>> defaultQuery = null;
		TEST: for (Word<String> wordInput : wordsToCheck) {
			Log.err("Executing the test query: "+ wordInput);
			for (String s : wordInput.asList()) {
				if (!inputs.contains(s)) {
					Log.err("Alphabet " + inputs + " does not contain symbol: " + s);
					Log.err("Skipping test");
					continue TEST;
				}
			}
			Word<O> hypOutput = hyp.computeOutput(wordInput);
			Word<O> sutOutput;
			try {
				sutOutput = oracle.answerQuery(wordInput);
				if (!hypOutput.equals(sutOutput)) {
					Log.err("Selected word counterexample \n" +
							"for input: " + wordInput + "\n" +
							"expected: " + sutOutput + "\n" +
							"received: " + hypOutput);
					defaultQuery = new DefaultQuery<String, Word<O>>(wordInput, sutOutput);
					break;
				}
			} catch (SULException e) {
				e.printStackTrace();
				Log.err("Error executing the test query: " + wordInput);
				System.exit(0);
			}
		}
		return defaultQuery;
	}

	public void setOracle(MembershipOracle<String, Word<O>> arg0) {
		this.oracle = arg0;

	}


}
