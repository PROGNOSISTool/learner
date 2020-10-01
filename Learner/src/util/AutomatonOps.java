package util;

import net.automatalib.automata.fsa.impl.compact.CompactDFA;
import net.automatalib.automata.graphs.TransitionEdge;
import net.automatalib.automata.transducers.impl.compact.CompactMealy;
import net.automatalib.graphs.Graph;
import net.automatalib.graphs.UniversalGraph;
import net.automatalib.serialization.dot.GraphDOT;
import net.automatalib.util.automata.fsa.DFAs;
import net.automatalib.util.graphs.Path;
import net.automatalib.util.graphs.ShortestPaths;
import net.automatalib.words.Word;
import net.automatalib.words.WordBuilder;
import net.automatalib.words.impl.Alphabets;
import net.automatalib.words.impl.GrowingMapAlphabet;
import util.learnlib.DotDo;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

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

		CompactDFA<Tuple2<String, String>> newAutomaton1 = new CompactDFA<>(automaton1);
		CompactDFA<Tuple2<String, String>> newAutomaton2 = new CompactDFA<>(automaton2);
		for (Tuple2<String, String> symbol : alphabet) {
			newAutomaton1.addAlphabetSymbol(symbol);
			newAutomaton2.addAlphabetSymbol(symbol);
		}

		CompactDFA<Tuple2<String, String>> or = DFAs.or(newAutomaton1, newAutomaton2, alphabet);
		return or;
	}

	public static CompactDFA<Tuple2<String, String>> union(Collection<CompactDFA<Tuple2<String, String>>> automata) {
		CompactDFA<Tuple2<String, String>> dfa = new CompactDFA<>(automata.iterator().next());
		for (CompactDFA<Tuple2<String, String>> automaton : automata) {
			dfa = union(dfa, automaton);
		}

		return dfa;
	}

	public static CompactDFA<Tuple2<String, String>> intersection(CompactDFA<Tuple2<String, String>> automaton1, CompactDFA<Tuple2<String, String>> automaton2) {
		GrowingMapAlphabet<Tuple2<String, String>> alphabet = new GrowingMapAlphabet<>();
		alphabet.addAll(automaton1.getInputAlphabet());
		alphabet.addAll(automaton2.getInputAlphabet());

		CompactDFA<Tuple2<String, String>> newAutomaton1 = new CompactDFA<>(automaton1);
		CompactDFA<Tuple2<String, String>> newAutomaton2 = new CompactDFA<>(automaton2);
		for (Tuple2<String, String> symbol : alphabet) {
			newAutomaton1.addAlphabetSymbol(symbol);
			newAutomaton2.addAlphabetSymbol(symbol);
		}

		return DFAs.and(newAutomaton1, newAutomaton2, alphabet);
	}

	public static CompactDFA<Tuple2<String, String>> intersection(Collection<CompactDFA<Tuple2<String, String>>> automata) {
		CompactDFA<Tuple2<String, String>> dfa = new CompactDFA<>(automata.iterator().next());
		for (CompactDFA<Tuple2<String, String>> automaton : automata) {
			dfa = intersection(dfa, automaton);
		}

		return dfa;
	}

	public static CompactDFA<Tuple2<String, String>> difference(CompactDFA<Tuple2<String, String>> automaton1, CompactDFA<Tuple2<String, String>> automaton2) {
		return intersection(automaton1, negation(automaton2));
	}

	public static CompactDFA<Tuple2<String, String>> deltaStar(Collection<CompactDFA<Tuple2<String, String>>> automata) {
		return DFAs.minimize(difference(union(automata), intersection(automata)));
	}

	private static void saveDFA(String filename, CompactDFA<Tuple2<String, String>> model) {
		try (BufferedWriter out = new BufferedWriter(new FileWriter(filename))) {
			GraphDOT.write(model, model.getInputAlphabet(), out);
		} catch (IOException exception) {
			exception.printStackTrace();
		}
	}

	public static Set<Word<String>> getPaths(CompactDFA<Tuple2<String, String>> automaton) {
		Set<Word<String>> words = new HashSet<>();

		Collection<Integer> acceptingStates = new HashSet<>();
		for (Integer state : automaton.getStates()) {
			if (automaton.isAccepting(state)) {
				acceptingStates.add(state);
			}
		}

		UniversalGraph<Integer, TransitionEdge<Tuple2<String, String>, Integer>, Boolean, TransitionEdge.Property<Tuple2<String, String>, Void>> graph = automaton.transitionGraphView();
		Iterable<Path<Integer, TransitionEdge<Tuple2<String, String>, Integer>>> paths = ShortestPaths.shortestPaths(graph, automaton.getInitialState(), 100000000, acceptingStates);
		for (Path<Integer, TransitionEdge<Tuple2<String, String>, Integer>> path : paths) {
			WordBuilder<String> word = new WordBuilder<>();
			for (TransitionEdge<Tuple2<String, String>, Integer> edge : path.edgeList()) {
				word.add(edge.getInput().tuple0);
			}
			words.add(word.toWord());
		}

		return words;
	}

	public static void main(String[] args) throws IOException {
		LinkedList<CompactMealy<String, String>> mealyMachines = new LinkedList<>();
		for (String file : args) {
			mealyMachines.add(DotDo.readFile(file));
		}

		LinkedList<CompactDFA<Tuple2<String, String>>> tupleDFAs = new LinkedList<>();
		for (CompactMealy<String, String> mealyMachine : mealyMachines) {
			assert mealyMachine != null;
			tupleDFAs.add(convertToTupleDFA(mealyMachine));
		}

		CompactDFA<Tuple2<String, String>> deltaStarDFA = deltaStar(tupleDFAs);
		saveDFA("deltaStarAutomaton.dot", deltaStarDFA);

		Set<Word<String>> queries = getPaths(deltaStarDFA);
		FileManager.writeQueriesToFile("deltaStarQueries.txt", queries);
	}
}
