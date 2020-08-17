package learner;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import de.learnlib.api.exception.SULException;
import de.learnlib.api.oracle.EquivalenceOracle;
import de.learnlib.api.oracle.MembershipOracle;
import de.learnlib.api.query.DefaultQuery;
import net.automatalib.automata.transducers.MealyMachine;
import org.checkerframework.checker.nullness.qual.Nullable;
import util.Log;

import net.automatalib.words.Word;

public class WordCheckingEquivalenceOracle implements EquivalenceOracle<MealyMachine<?, String, ?, String>, String, Word<String>> {

	private List<Word<String>> wordsToCheck = new ArrayList<Word<String>>();
	private MembershipOracle<String, Word<String>> oracle;

	public WordCheckingEquivalenceOracle(MembershipOracle<String, Word<String>> oracle, List<List<String>> inputTracesToCheck) {
		this.wordsToCheck = parseTestWords(inputTracesToCheck);
		this.oracle = oracle;
	}

	private List<Word<String>> parseTestWords(List<List<String>> inputTracesToCheck) {
		List<Word<String>> testWords = new ArrayList<Word<String>>();
		for(List<String> traceToCheck : inputTracesToCheck) {
			Word<String> testWord = Word.fromList(traceToCheck);
			testWords.add(testWord);
		}
		return testWords;
	}

	@Override
	public @Nullable DefaultQuery<String, Word<String>> findCounterExample(MealyMachine<?, String, ?, String> hyp, Collection<? extends String> alphabet) {
		DefaultQuery<String, Word<String>> defaultQuery = null;
		TEST: for (Word<String> wordInput : wordsToCheck) {
			Log.err("Executing the test query: "+ wordInput);
			for (String s : wordInput.asList()) {
				if (!alphabet.contains(s)) {
					Log.err("Alphabet " + alphabet + " does not contain symbol: " + s);
					Log.err("Skipping test");
					continue TEST;
				}
			}
			Word<String> hypOutput = hyp.computeOutput(wordInput);
			Word<String> sutOutput;
			try {
				sutOutput = oracle.answerQuery(wordInput);
				if (!hypOutput.equals(sutOutput)) {
					Log.err("Selected word counterexample \n" +
							"for input: " + wordInput + "\n" +
							"expected: " + sutOutput + "\n" +
							"received: " + hypOutput);
					// "the output field in the DefaultQuery contains the SUL output for the respective query."[1]
					//  and "The reaction of the target system consists of the output word produced while executing the suffix."[2]
					// Thus in this case, the suffix must be entirely responsible for the output
					// [1] http://learnlib.github.io/learnlib/maven-site/0.15.0/apidocs/de/learnlib/api/oracle/EquivalenceOracle.html#findCounterExample-A-java.util.Collection-
					// [2] http://learnlib.github.io/learnlib/maven-site/0.15.0/apidocs/de/learnlib/api/query/Query.html
					defaultQuery = new DefaultQuery<String, Word<String>>(Word.epsilon(), wordInput, sutOutput);
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

	public void setOracle(MembershipOracle<String, Word<String>> arg0) {
		this.oracle = arg0;

	}
}
