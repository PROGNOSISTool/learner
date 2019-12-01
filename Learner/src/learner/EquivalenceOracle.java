package learner;

import sutInterface.SutWrapper;
import util.Container;
import util.InputAction;
import util.OutputAction;
import de.ls5.jlearn.abstractclasses.LearningException;
import de.ls5.jlearn.interfaces.Symbol;
import de.ls5.jlearn.interfaces.Word;
import de.ls5.jlearn.shared.SymbolImpl;
import de.ls5.jlearn.shared.WordImpl;

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

		
		for (Symbol currentSymbol : query.getSymbolList()) {
			String outputString = sendInput(currentSymbol.toString());
			result.addSymbol(new SymbolImpl(outputString));
		}

		System.out.println("Returning to LearnLib: " + result);
		return result;
	}
	
	public String sendInput(String inputString) {
		InputAction input = new InputAction(inputString);
		System.out.println("Sending: " + inputString);

		OutputAction output = sutWrapper.sendInput(input);
		if (output != null) {
			String outputString = output.getValuesAsString();
			System.out.println("Received: " + outputString);
			return outputString;
		} else {
			return null;
		}
		
	}
}
