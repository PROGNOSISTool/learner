package learner;

import sutInterface.SutWrapper;
import util.Container;
import util.InputAction;
import util.OutputAction;
import de.learnlib.api.query.Query;
import net.automatalib.words.Word;
import net.automatalib.words.impl.Symbol;

import java.util.Collection;

public class MembershipOracle<I, D> implements de.learnlib.api.oracle.MembershipOracle<I, D> {
	private static final long serialVersionUID = -1374892499287788040L;
	private SutWrapper sutWrapper;
	private final Container<Integer> membershipCounter;

	public MembershipOracle(SutWrapper sutWrapper, Container<Integer> membershipCounter) {
		this.sutWrapper = sutWrapper;
		this.membershipCounter = membershipCounter;
	}

	@Override
	public void processQuery(Query query) {
		Word result = Word.epsilon();

		sutWrapper.sendReset();

		System.out.println("Membership query number: " + this.membershipCounter.value);
		System.out.println("Query: " + query);

		for (Object currentSymbol : query.getInput().asList()) {
			String outputString = sendInput(currentSymbol.toString());
			result.append(new Symbol(outputString));
		}

		System.out.println("Returning to LearnLib: " + result);
		query.answer(result);
	}

	@Override
	public void processQueries(Collection<? extends Query<I, D>> queries) {
		for (Query query : queries) {
			processQuery(query);
		}
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
