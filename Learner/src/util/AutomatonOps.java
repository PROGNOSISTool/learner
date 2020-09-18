package util;

import net.automatalib.automata.fsa.impl.compact.CompactDFA;
import net.automatalib.automata.transducers.impl.compact.CompactMealy;
import net.automatalib.serialization.dot.GraphDOT;
import net.automatalib.util.automata.fsa.DFAs;
import net.automatalib.words.impl.GrowingMapAlphabet;
import util.learnlib.DotDo;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;

public class AutomatonOps {
	public static CompactDFA<Tuple2<String, String>> convertToTupleDFA(CompactMealy<String, String> mealyMachine) {
		GrowingMapAlphabet<Tuple2<String, String>> alphabet = new GrowingMapAlphabet<>();
		CompactDFA<Tuple2<String, String>> dfa = new CompactDFA<>(alphabet);

		HashMap<Integer, Boolean> visited = new HashMap<>();
		for (Integer state : mealyMachine.getStates()) {
			visited.put(state, false);
			dfa.addState(true);
		}

		LinkedList<Integer> queue = new LinkedList<>();
		Integer current = mealyMachine.getInitialState();

		visited.replace(current, true);
		queue.add(current);
		dfa.setInitialState(current);

		while (queue.size() != 0) {
			current = queue.poll();

			for (String input : mealyMachine.getLocalInputs(current)) {
				Integer next = mealyMachine.getSuccessor(current, input);
				assert next != null;
				Tuple2<String, String> symbol = new Tuple2<>(input, mealyMachine.getOutput(current, input));
				dfa.addAlphabetSymbol(symbol);
				dfa.addTransition(current, symbol, next);

				if (!visited.get(next)) {
					visited.replace(next, true);
					queue.add(next);
				}
			}
		}

		return DFAs.complete(dfa, alphabet);
	}

	public static CompactDFA<Tuple2<String, String>> negation(CompactDFA<Tuple2<String, String>> automaton) {
		return DFAs.complement(automaton, automaton.getInputAlphabet());
	}

	public static CompactDFA<Tuple2<String, String>> union(CompactDFA<Tuple2<String, String>> automaton1, CompactDFA<Tuple2<String, String>> automaton2) {
		GrowingMapAlphabet<Tuple2<String, String>> alphabet = new GrowingMapAlphabet<>();
		alphabet.addAll(automaton1.getInputAlphabet());
		alphabet.addAll(automaton2.getInputAlphabet());

		return DFAs.or(automaton1, automaton2, alphabet);
	}

	public static CompactDFA<Tuple2<String, String>> union(Collection<CompactDFA<Tuple2<String, String>>> automata) {
		CompactDFA<Tuple2<String, String>> dfa = automata.iterator().next();
		for (CompactDFA<Tuple2<String, String>> automaton : automata) {
			dfa = union(dfa, automaton);
		}

		return dfa;
	}

	public static CompactDFA<Tuple2<String, String>> intersection(CompactDFA<Tuple2<String, String>> automaton1, CompactDFA<Tuple2<String, String>> automaton2) {
		GrowingMapAlphabet<Tuple2<String, String>> alphabet = new GrowingMapAlphabet<>();
		alphabet.addAll(automaton1.getInputAlphabet());
		alphabet.addAll(automaton2.getInputAlphabet());

		return DFAs.and(automaton1, automaton2, alphabet);
	}

	public static CompactDFA<Tuple2<String, String>> intersection(Collection<CompactDFA<Tuple2<String, String>>> automata) {
		CompactDFA<Tuple2<String, String>> dfa = automata.iterator().next();
		for (CompactDFA<Tuple2<String, String>> automaton : automata) {
			dfa = intersection(dfa, automaton);
		}

		return dfa;
	}

	public static CompactDFA<Tuple2<String, String>> difference(CompactDFA<Tuple2<String, String>> automaton1, CompactDFA<Tuple2<String, String>> automaton2) {
		return intersection(automaton1, negation(automaton2));
	}

	public static CompactDFA<Tuple2<String, String>> deltaStar(Collection<CompactDFA<Tuple2<String, String>>> automata) {
		return difference(union(automata), intersection(automata));
	}

	private static <I> void saveDFA(String filename, CompactDFA<Tuple2<String, String>> model) {
		try (BufferedWriter out = new BufferedWriter(new FileWriter(filename))) {
			GraphDOT.write(model, model.getInputAlphabet(), out);
		} catch (IOException exception) {
			exception.printStackTrace();
		}
	}

	public static void main(String[] args) throws IOException {
		CompactMealy<String, String> mealyMachine = DotDo.readFile(args[0]);
		assert mealyMachine != null;
		CompactDFA<Tuple2<String, String>> dfa = convertToTupleDFA(mealyMachine);
		saveDFA("dfaOut.dot", dfa);
	}

}
