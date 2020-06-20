package learner;

import java.io.IOException;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import de.learnlib.api.exception.SULException;
import de.learnlib.api.oracle.MembershipOracle;
import de.learnlib.api.oracle.EquivalenceOracle.MealyEquivalenceOracle;
import de.learnlib.api.query.DefaultQuery;
import net.automatalib.words.Word;
import net.automatalib.automata.transducers.MealyMachine;

import util.Container;
import util.LearnlibUtils;
import util.Log;
import util.Tuple2;
import util.learnlib.YannakakisTest;

public class IOEquivalenceOracle<O> implements MealyEquivalenceOracle<String, O>  {

	private MembershipOracle<String, Word<O>> oracle;
	private final int numberOfTests;
	private final boolean uniqueOnly;
	private final Container<Integer> uniqueCounter;
	private String ioCommand;
	private int hypTestNumber = 0;

	public IOEquivalenceOracle (MembershipOracle<String, Word<O>> oracle, int numberOfTests, String ioCommand) {
		this(oracle, numberOfTests, ioCommand, null);
	}

	public IOEquivalenceOracle (MembershipOracle<String, Word<O>> oracle, int numberOfTests, String ioCommand, Container<Integer> uniqueCounter) {
		this.oracle = oracle;
		this.ioCommand = ioCommand;
		if (numberOfTests <= 0) {
			this.numberOfTests = Integer.MAX_VALUE - 1;
		} else {
			this.numberOfTests = numberOfTests;
		}
		this.uniqueCounter = uniqueCounter;
		this.uniqueOnly = uniqueCounter != null;
	}

	@Override
	public DefaultQuery<String, Word<O>> findCounterExample(MealyMachine<?, String, ?, O> hyp, Collection<? extends String> inputs) {
		List<String> testQuery = null;
		TestGenerator wrapper = null;
		if (ioCommand.contains("fixed")) {
		    try {
		        Tuple2<List<LinkedList<String>>, Integer> tuple = YannakakisTest.getMinimumalTestSuite(hyp, inputs, Main.getTree(), ioCommand, 0, 100);
		        List<LinkedList<String>> testSuite = tuple.tuple0;
		        int expectedNumTests = tuple.tuple1;
                Log.err("Changed seed to: " + YannakakisTest.seed);
                Log.err("Number of generated tests: " + testSuite.size());
                Log.err("Number of actual tests: " + expectedNumTests);
                wrapper = new PredefinedTestGenerator(testSuite);
            } catch (IOException e) {
                e.printStackTrace();
                System.exit(0);
            }
		} else {
		    wrapper = new YannakakisWrapper<O>(hyp, inputs, ioCommand);
		}
		wrapper.initialize();
		int uniqueValueStart = uniqueOnly ? uniqueCounter.value : 0;
		try {
			for (hypTestNumber = 0;	hypTestNumber < numberOfTests; hypTestNumber = uniqueOnly ? uniqueCounter.value - uniqueValueStart : hypTestNumber + 1) {
				Log.info("Equivalence test " + hypTestNumber + " for this hypothesis");
				testQuery = wrapper.nextTest();

				if ( testQuery != null) {
					Word<String> wordInput = LearnlibUtils.symbolsToWords(testQuery);
					Word<O> hypOutput = hyp.computeOutput(wordInput);
					Word<O> sutOutput;
					try {
						sutOutput = oracle.answerQuery(wordInput);
						if (!hypOutput.equals(sutOutput)) {
						    if (ioCommand.contains("fixed")) {
						    Main.getTree().remove(wordInput.asList());
						    }
						    sutOutput = oracle.answerQuery(wordInput);
						    if (!hypOutput.equals(sutOutput)) {
							Log.err("Yannakakis counterexample \n" +
									"for input: " + wordInput + "\n" +
									"expected: " + sutOutput + "\n" +
									"received: " + hypOutput);

							wrapper.terminate();
							Log.err("Counterexample found after " + hypTestNumber + " attempts");
							return new DefaultQuery<String, Word<O>>(wordInput, sutOutput);
						    }
						}
					} catch (SULException e) {
						e.printStackTrace();
						throw new RuntimeException("Error executing the test query: " + wordInput);
					}
				} else {
					Log.err("Yannakakis did not produce enough equivalence queries");
					break;
				}
			}
		} catch (IOException e) {
			throw new RuntimeException("Generated IO Exception while generating tests from stdin");
		}
		wrapper.terminate();
		Log.err("No counterexample found after " + hypTestNumber + " attempts");
		return null;
	}


	public void setOracle(MembershipOracle<String, Word<O>> arg0) {
		this.oracle = arg0;
	}

	/**
	 * Gets the number of tests done for the current/most recent hypothesis
	 * @return
	 */
	public int getNrHypthesisTests() {
		return this.hypTestNumber;
	}

	public void clearNrHypTests() {
	    this.hypTestNumber = 0;
	}
}
