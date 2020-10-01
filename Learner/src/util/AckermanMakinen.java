package util;

import net.automatalib.automata.fsa.impl.compact.CompactNFA;
import net.automatalib.words.Alphabet;
import net.automatalib.words.Word;
import net.automatalib.words.impl.ListAlphabet;

import java.text.Collator;
import java.util.*;

// The Ackerman-Mäkinen Boolean Algorithms
// From: Three New Algorithms for Regular Language Enumeration (2009)

public class AckermanMakinen {

	public static LinkedList<Word<String>> enumerate(int max, CompactNFA<String> nfa) {
		if (max < 1) {
			return new LinkedList<>();
		}

		int i = 0;
		int numCEC = 0;
		int len = 0;
		int numberOfStates = nfa.getStates().size();
		LinkedList<Word<String>> words = new LinkedList<>();
		while (i < max && numCEC < numberOfStates) {
			LinkedList<HashSet<Integer>> stateStack = new LinkedList<>();
			for (int x = 0; x <= max; x++) {
				stateStack.push(null);
			}
			stateStack.set(0, new HashSet<>(nfa.getInitialStates()));
			Tuple2<LinkedList<HashSet<Integer>>, Word<String>> wordResult = minWordLM(stateStack, len, nfa);
			stateStack = wordResult.tuple0;
			Word<String> word = wordResult.tuple1;

			if (word == null) {
				numCEC++;
			} else {
				numCEC = 0;
				while (word != null && i < max) {
					words.add(word);
					Tuple2<LinkedList<HashSet<Integer>>, Word<String>> nextWordResult = nextWord(stateStack, word, nfa);
					stateStack = nextWordResult.tuple0;
					word = nextWordResult.tuple1;
					i++;
				}
			}
			len++;
		}
		return words;
	}

	public static Tuple2<LinkedList<HashSet<Integer>>, Word<String>>  nextWord(LinkedList<HashSet<Integer>> stateStack, Word<String> word, CompactNFA<String> nfa) {
		LinkedList<String> sortedAlphabet = getSortedAlphabet(nfa);
		HashMap<Tuple2<Integer, Integer>, Boolean> iCompletenessTable = preprocessingAMBoolean(word.size(), nfa);

		for (int i = word.size(); i > 0; i--) {
			stateStack.set(i - 1, stateStack.peek());

			HashSet<Integer> RStates = new HashSet<>();
			HashSet<Integer> successorStates = new HashSet<>();
			for (Integer state : stateStack.get(i - 1)) {
				for (String symbol : sortedAlphabet) {
					successorStates.addAll(nfa.getSuccessors(state, symbol));
				}
			}

			for (Integer state : successorStates) {
				if (iCompletenessTable.containsKey(new Tuple2<>(state, word.size() - i))) {
					if (iCompletenessTable.get(new Tuple2<>(state, word.size() - i))) {
						RStates.add(state);
					}
				}
			}


			HashSet<String> ASymbols = new HashSet<>();
			for (String symbol : sortedAlphabet) {
				HashSet<Integer> stateSet = new HashSet<>();
				for (Integer state : stateStack.get(i - 1)) {
					stateSet.addAll(nfa.getSuccessors(state, symbol));
				}
				stateSet.retainAll(RStates);
				if (!stateSet.isEmpty()) {
					ASymbols.add(symbol);
				}
			}

			boolean shouldPop = true;
			String iSymbol = word.getSymbol(i - 1);
			for (String symbol : ASymbols) {
				if (sortedAlphabet.indexOf(symbol) > sortedAlphabet.indexOf(iSymbol)) {
					shouldPop = false;
					break;
				}
			}

			if (shouldPop) {
				stateStack.pop();
			} else {
				String bSymbol = null;
				LinkedList<String> BAlphabet = new LinkedList<>();
				for (String symbol : ASymbols) {
					if (sortedAlphabet.indexOf(symbol) > sortedAlphabet.indexOf(iSymbol)) {
						BAlphabet.add(symbol);
					}
				}
				BAlphabet.sort((o1, o2) -> Collator.getInstance().compare(o1,o2));
				bSymbol = BAlphabet.getFirst();

				HashSet<Integer> newSi = new HashSet<>();
				for (Integer state : stateStack.get(i - 1)) {
					for (Integer successorState : nfa.getSuccessors(state, bSymbol)) {
						if (iCompletenessTable.containsKey(new Tuple2<>(successorState, word.size() - i))) {
							if (iCompletenessTable.get(new Tuple2<>(successorState, word.size() - i))) {
								newSi.add(state);
							}
						}
					}
				}

				stateStack.set(i, newSi);

				if (i != word.size()) {
					stateStack.push(newSi);
				}

				Word<String> wPrime = word.subWord(0, i - 1);
				wPrime = wPrime.append(bSymbol);
				Tuple2<LinkedList<HashSet<Integer>>, Word<String>> minWordResult = minWordLM(stateStack, word.size() - i, nfa);
				stateStack = minWordResult.tuple0;
				Word<String> minWord = minWordResult.tuple1;

				if (minWord != null) {
					wPrime = wPrime.concat(minWord);
				}
				return new Tuple2<>(stateStack, wPrime);
			}
		}
		return new Tuple2<>(stateStack, null);
	}

	// O((s+d)n + f) . s = |Q| ^ d = number of transitions ^ n = |w| ^ f = |F|
	public static HashMap<Tuple2<Integer, Integer>, Boolean> preprocessingAMBoolean(Integer n, CompactNFA<String> nfa) {
		HashMap<Tuple2<Integer, Integer>, Boolean> iCompletenessTable = new HashMap<>();
		if (n < 0) {
			return null;
		}

		HashSet<Integer> finalStates = getFinalStates(nfa);
		for (Integer state : finalStates) {
			iCompletenessTable.put(new Tuple2<>(state, 0), true);
		}

		for (Integer state : nfa.getStates()) {
			iCompletenessTable.put(new Tuple2<>(state, 1), false);
			for (String symbol : nfa.getInputAlphabet()) {
				Set<Integer> finalSuccessorStates = nfa.getSuccessors(state, symbol);
				finalSuccessorStates.retainAll(finalStates);

				if (!finalSuccessorStates.isEmpty()) {
					iCompletenessTable.put(new Tuple2<>(state, 1), true);
					break;
				}
			}
		}

		for (int i = 2; i <= n; i++) {
			for (Integer state : nfa.getStates()) {
				iCompletenessTable.put(new Tuple2<>(state, i), false);
				for (String symbol : nfa.getInputAlphabet()) {
					for (Integer successorState : nfa.getSuccessors(state, symbol)) {
						if (iCompletenessTable.get(new Tuple2<>(successorState, i - 1))) {
							iCompletenessTable.put(new Tuple2<>(state, i), true);
						}
					}
				}
			}
		}

		return iCompletenessTable;
	}

	public static Tuple2<LinkedList<HashSet<Integer>>, Word<String>> minWordLM(LinkedList<HashSet<Integer>> stateStack, Integer n, CompactNFA<String> nfa) {
		if (n <= 0) {
			return new Tuple2<>(stateStack, null);
		}

		LinkedList<Matrix> adjacencyMatrices = computeAdjacencyMatrices(n, nfa);
		HashSet<Integer> finalStates = getFinalStates(nfa);
		LinkedList<String> sortedAlphabet = getSortedAlphabet(nfa);

		boolean shouldReturn = true;
		for (Integer initialState : nfa.getInitialStates()) {
			for (Integer finalState : finalStates) {
				if (adjacencyMatrices.get(n).get(initialState, finalState)) {
					shouldReturn = false;
					break;
				}
			}
		}
		if (shouldReturn) {
			return new Tuple2<>(stateStack, null);
		}

		Word<String> word = Word.epsilon();
		for (int i = 0; i < n; i++) {
			String nextSymbol = null;
			alphabetLoop:
			for (String symbol : sortedAlphabet) {
				for (Integer state : stateStack.get(i)) {
					for (Integer finalState : finalStates) {
						for(Integer successorState : nfa.getSuccessors(state, symbol)) {
							if (adjacencyMatrices.get(n - 1 - i).get(successorState, finalState)) {
								nextSymbol = symbol;
								break alphabetLoop;
							}
						}
					}
				}
			}

			word = word.append(nextSymbol);

			if (i != n - 1) {
				HashSet<Integer> nextStateSet = new HashSet<>();
				for (Integer state : stateStack.get(i)) {
					for (Integer successorState : nfa.getSuccessors(state, nextSymbol)) {
						for (Integer finalState : finalStates) {
							if (adjacencyMatrices.get(n - 1 - i).get(successorState, finalState)) {
								nextStateSet.add(successorState);
							}
						}
					}
				}

				stateStack.set(i + 1, nextStateSet);
			}
		}

		return new Tuple2<>(stateStack, word.flatten());
	}

	private static HashSet<Integer> getFinalStates(CompactNFA<String> nfa) {
		HashSet<Integer> finalStates = new HashSet<>();
		for (Integer state : nfa.getStates()) {
			if (nfa.isAccepting(state)) {
				finalStates.add(state);
			}
		}
		return finalStates;
	}

	private static LinkedList<String> getSortedAlphabet(CompactNFA<String> nfa) {
		LinkedList<String> sortedAlphabet = new LinkedList<>(nfa.getInputAlphabet());
		sortedAlphabet.sort((o1, o2) -> Collator.getInstance().compare(o1,o2));
		return sortedAlphabet;
	}

	public static LinkedList<Matrix> computeAdjacencyMatrices(int upto, CompactNFA<String> nfa) {
		LinkedList<Matrix> adjacencyMatrices = new LinkedList<>();
		adjacencyMatrices.add(Matrix.identity(nfa.getStates().size()));

		Matrix M1 = new Matrix(nfa.getStates().size(), nfa.getStates().size());
		for (Integer stateFrom : nfa.getStates()) {
			for (String symbol : nfa.getLocalInputs(stateFrom)) {
				for(Integer stateTo : nfa.getSuccessors(stateFrom, symbol)) {
					M1.set(stateFrom, stateTo, true);
				}
			}
		}
		adjacencyMatrices.add(M1);

		for (int i = 2; i <= upto; i++) {
			adjacencyMatrices.add(adjacencyMatrices.get(i-1).multiply(M1));
		}

		return adjacencyMatrices;
	}

	public static void main(String[] args) {
		LinkedList<String> alphaList = new LinkedList<>();
		alphaList.add("a");
		alphaList.add("b");
		alphaList.add("c");
		Alphabet<String> alphabet = new ListAlphabet<>(alphaList);
		CompactNFA<String> nfa = new CompactNFA<>(alphabet);

		Integer init0 = nfa.addInitialState();
		Integer acc2 = nfa.addState(true);
		Integer acc3 = nfa.addState(true);

		nfa.addTransition(init0, "a", acc2);
		nfa.addTransition(acc2, "b", init0);
		nfa.addTransition(init0, "c", acc3);
		nfa.addTransition(acc3, "b", init0);

//		Integer state1 = nfa.addState(false);
//		Integer state2 = nfa.addState(false);
//		Integer state3 = nfa.addState(true);
//
//		nfa.addTransition(init0, "a", state1);
//		nfa.addTransition(state1, "b", state2);
//		nfa.addTransition(state1, "a", state2);
//		nfa.addTransition(state2, "c", state3);

//		LinkedList<HashSet<Integer>> stateStack = new LinkedList<>();
//		for (int i = 0; i < 3; i++) {
//			stateStack.push(new HashSet<>());
//		}
//		HashSet<Integer> init = new HashSet<>();
//		init.add(init0);
//		stateStack.set(0, init);

//		Tuple2<LinkedList<HashSet<Integer>>, Word<String>> result = minWordLM(stateStack, 3, nfa);
//		System.out.println(result.tuple1.toString());

		LinkedList<Word<String>> enumerated = enumerate(50, nfa);
		for (Word<String> word : enumerated) {
			System.out.println(word.toString());
		}
	}
}
