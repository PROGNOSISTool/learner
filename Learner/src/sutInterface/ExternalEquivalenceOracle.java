package sutInterface;

import de.learnlib.api.oracle.EquivalenceOracle;
import de.learnlib.api.oracle.MembershipOracle;
import de.learnlib.api.query.DefaultQuery;
import net.automatalib.automata.transducers.MealyMachine;
import net.automatalib.words.Word;
import org.checkerframework.checker.nullness.qual.Nullable;
import util.FileManager;

import java.util.*;

public class ExternalEquivalenceOracle implements EquivalenceOracle<MealyMachine<?, String, ?, String>, String, Word<String>> {
	private final MembershipOracle<String, Word<String>> queryOracle;
	private final Set<Word<String>> counterExamples;

	public ExternalEquivalenceOracle(MembershipOracle<String, Word<String>> queryOracle, String cexFilename) {
		this.queryOracle = queryOracle;
		this.counterExamples = FileManager.readQueriesFromFile(cexFilename);
	}

	@Override
	public @Nullable DefaultQuery<String, Word<String>> findCounterExample(MealyMachine<?, String, ?, String> model, Collection<? extends String> alphabet) {
		for (Word<String> counterExample : counterExamples) {
			Set<String> cexSymbols = new HashSet<>(counterExample.asList());
			cexSymbols.removeAll(alphabet);
			if (cexSymbols.size() == 0) {
				DefaultQuery<String, Word<String>> query = new DefaultQuery<>(counterExample);
				Word<String> modelOutput = model.computeOutput(counterExample);
				Word<String> sulOutput = queryOracle.answerQuery(counterExample);
				if (!sulOutput.equals(modelOutput)) {
					query.answer(sulOutput);
					return query;
				}
			}
		}
		return null;
	}
}
