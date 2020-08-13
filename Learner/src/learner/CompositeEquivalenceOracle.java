package learner;
import de.learnlib.api.query.DefaultQuery;
import de.learnlib.api.oracle.EquivalenceOracle;
import net.automatalib.automata.transducers.MealyMachine;
import net.automatalib.words.Word;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Collection;
import java.util.List;


public class CompositeEquivalenceOracle implements EquivalenceOracle<MealyMachine<?, String, ?, String>, String, Word<String>> {

	private final List<EquivalenceOracle<MealyMachine<?, String, ?, String>, String, Word<String>>> eqOracles;

	public CompositeEquivalenceOracle(List<EquivalenceOracle<MealyMachine<?, String, ?, String>, String, Word<String>>> oracles) {
		this.eqOracles = oracles;
	}

	@Override
	public @Nullable DefaultQuery<String, Word<String>> findCounterExample(MealyMachine<?, String, ?, String> hyp, Collection<? extends String> alphabet) {
		DefaultQuery<String, Word<String>> eqOutput = null;
		for (EquivalenceOracle<MealyMachine<?, String, ?, String>, String, Word<String>> eqOracle : eqOracles) {
			eqOutput = eqOracle.findCounterExample(hyp, alphabet);
			if (eqOutput != null) {
				break;
			}
		}
		return eqOutput;
	}
}
