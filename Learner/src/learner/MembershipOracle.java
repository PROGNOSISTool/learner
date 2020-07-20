package learner;

import sutInterface.SutWrapper;
import util.Container;
import de.ls5.jlearn.abstractclasses.LearningException;
import de.ls5.jlearn.interfaces.Symbol;
import de.ls5.jlearn.interfaces.Word;
import de.ls5.jlearn.shared.SymbolImpl;
import de.ls5.jlearn.shared.WordImpl;

public class MembershipOracle implements ExtendedOracle {
	private static final long serialVersionUID = -1374892499287788040L;
	private SutWrapper sutWrapper;
	private final Container<Integer> membershipCounter;

	public MembershipOracle(SutWrapper sutWrapper, Container<Integer> membershipCounter) {
		this.sutWrapper = sutWrapper;
		this.membershipCounter = membershipCounter;
	}

	//@Override
	public Word processQuery(Word query) throws LearningException {
		Word result = new WordImpl();

		sutWrapper.sendReset();

		System.out.println("Membership query number: " + this.membershipCounter.value);
		System.out.println("Query: " + query);

		for (Symbol currentSymbol : query.getSymbolList()) {
			String outputString = sendInput(currentSymbol.toString());
			result.addSymbol(new SymbolImpl(outputString));
		}

		System.out.println("Returning to LearnLib: " + result);

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
