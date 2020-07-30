package learner;

import sutInterface.SutWrapper;
import util.Container;
import de.ls5.jlearn.abstractclasses.LearningException;
import de.ls5.jlearn.interfaces.Symbol;
import de.ls5.jlearn.interfaces.Word;
import de.ls5.jlearn.shared.SymbolImpl;
import de.ls5.jlearn.shared.WordImpl;

import java.util.LinkedList;
import java.util.List;

public class EquivalenceOracle implements ExtendedOracle {
	private static final long serialVersionUID = -5409624854115451929L;
	private SutWrapper sutWrapper;
	private final Container<Integer> equivCounter;
	private final Container<Integer> uniqueEquivCounter;

	public EquivalenceOracle(SutWrapper sutWrapper, Container<Integer> equivCounter, Container<Integer> uniqueEquivCounter) {
		this.sutWrapper = sutWrapper;
		this.equivCounter = equivCounter;
		this.uniqueEquivCounter = uniqueEquivCounter;
	}

	public EquivalenceOracle(SutWrapper sutWrapper, Container<Integer> equivCounter) {
		this(sutWrapper, equivCounter, null);
	}

	public Word processQuery(Word query) throws LearningException {
		Word result = new WordImpl();
		System.out.println("LearnLib Query: " + query);

		sutWrapper.sendReset();

		System.out.println("Equivalence query number: " + equivCounter.value +
				(uniqueEquivCounter == null ? "" : " (" + uniqueEquivCounter.value + ")"));


		result = sendQuery(query);

		System.out.println("Returning to LearnLib: " + result);
		return result;
	}

	public Word sendQuery(Word inputQuery) {
		StringBuilder inputQueryString = new StringBuilder();
		for (Symbol currentSymbol : inputQuery.getSymbolList()) {
			inputQueryString.append(currentSymbol.toString());
		}
		String resultString = sendInput(inputQueryString.toString());
		Word result = new WordImpl();
		for (String symbol : resultString.split(" ")) {
			result.addSymbol(new SymbolImpl(symbol));
		}
		return result;
	}

	public String sendInput(String input) {
		System.out.println("Sending: " + input);
		String output = sutWrapper.sendInput(input);

		if (output != null) {
			System.out.println("Received: " + output);
			return output;
		} else {
			return null;
		}

	}
}
