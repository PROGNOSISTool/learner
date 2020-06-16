package util.learnlib;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.Queue;

import util.Tuple2;

import net.automatalib.automata.FiniteAlphabetAutomaton;
import net.automatalib.automata.concepts.StateIDs;
import net.automatalib.words.Alphabet;
import net.automatalib.words.Word;

public class AutomatonUtils<S, I, T> {

	public List<S> getStatesInBFSOrder(FiniteAlphabetAutomaton<S, I, T> automaton) {
		Alphabet<I> alphabet = automaton.getInputAlphabet();

		Queue<S> statestovisit = new LinkedList<S>();
		List<S> result = new ArrayList<S>();
		HashSet<S> states = new HashSet<S>(); // to check if state is not seen already by other transition

		for (S initialState : automaton.getInitialStates()) {
			statestovisit.offer(initialState);
			result.add(initialState);
			states.add(initialState);
		}

		S current = statestovisit.poll();
		while (current != null) {
			for (I input : alphabet) {
				Set<S> destStates = automaton.getSuccessors(current, input);
				for (S destState : destStates) {
					if ((destState != null) && (!states.contains(destState))) {
						statestovisit.offer(destState);
						result.add(destState);
						states.add(destState);
					}
				}
			}

			if (statestovisit.isEmpty()) {
				break;
			}
			current = statestovisit.poll();
		}

		return result;
	}

	public List<I> traceToState(FiniteAlphabetAutomaton<S, I, T> automaton, int stateId) {
		S state = get(automaton, stateId);
		return traceToState(automaton, state);
	}

	/**
	 * Gives a minimal length path from the start state to the given state. We build this path based
	 * on the random value generator. Note, there can be several minimal paths to the given state. We only
	 * return one. The length is the first order. Traces of equal length are order by the position of the last input
	 * in the alphabet.
	 */
	public List<I> traceToState(FiniteAlphabetAutomaton<S, I, T> automaton, S state) {
		Alphabet<I> alphabet = automaton.getInputAlphabet();
		StateIDs<S> stateIDs = automaton.stateIDs();
		List<I> selectedMiddlePart = new ArrayList<I>();
		List<List<I>> middleParts = new ArrayList<List<I>>();
		middleParts.add(selectedMiddlePart);
		List<S> reachedStates = new ArrayList<S>();
		while(true) {
			List<I> traceToState = new ArrayList<I>();
			traceToState.addAll(selectedMiddlePart);
			S reachedState = getState(automaton, traceToState);
			if (stateIDs.getStateId(reachedState) == stateIDs.getStateId(state)) {
				break;
			}
			reachedStates.add(reachedState);
			middleParts.remove(0);
			for (I input : alphabet) {
				// we only add middle parts which are relevant (those that lead to new states)
				S successor = automaton.getSuccessors(reachedState, input).iterator().next();
				if (!reachedStates.contains(successor)) {
					List<I> newMiddlePart = new ArrayList<I>(selectedMiddlePart);
					newMiddlePart.add(input);
					middleParts.add(newMiddlePart);
				}
			}

			selectedMiddlePart = middleParts.get(0);
		}

		return selectedMiddlePart;
	}

	public List<I> distinguishingSeq(FiniteAlphabetAutomaton<S, I, T> automaton, int stateId1, int stateId2) {
		Alphabet<I> alphabet = automaton.getInputAlphabet();
		S state1 = get(automaton, stateId1);
		S state2 = get(automaton, stateId2);
		List<I> distSeq = getDistinguishingSeq(automaton, alphabet, state1, state2);
		return distSeq;
	}

	public Word<I> createWordFromSymbols(List<I> symbols) {
		return Word.fromList(symbols);
	}

	public int indexOf(FiniteAlphabetAutomaton<S, I, T> automaton, S state) {
		StateIDs<S> stateIds = automaton.stateIDs();
		return stateIds.getStateId(state);
	}

	public S get(FiniteAlphabetAutomaton<S, I, T> automaton, int stateId) {
		StateIDs<S> stateIds = automaton.stateIDs();
		return stateIds.getState(stateId);
	}

	public List<I> buildSymbols(Collection<I> trace) {
		return new ArrayList<I>(trace);
	}

	private List<I> getDistinguishingSeq(FiniteAlphabetAutomaton<S, I, T> automaton,
		Collection<I> symbols, S state1, S state2) {
		List<List<I>> middleParts = new ArrayList<List<I>>();
		List<I> traceToState1 = traceToState(automaton, state1);
		List<I> traceToState2 = traceToState(automaton, state2);
		List<Tuple2<S,S>> reachedSatePairs = new ArrayList<>();

		List<I> selectedMiddlePart = new ArrayList<I>();
		middleParts.add(selectedMiddlePart);
		while(true) {
			List<I> traceFromState1 = new ArrayList<I>();
			traceFromState1.addAll(traceToState1);
			traceFromState1.addAll(selectedMiddlePart);
			List<I> traceFromState2 = new ArrayList<I>();
			traceFromState2.addAll(traceToState2);
			traceFromState2.addAll(selectedMiddlePart);
			S reachedStateFrom1 = getState(automaton, traceFromState1);
			S reachedStateFrom2 = getState(automaton, traceFromState2);

			boolean diffFound = false;
			for (I input : symbols) {
				S successorOfInputAndState1 = automaton.getSuccessors(reachedStateFrom1, input).iterator().next();
				S successorOfInputAndState2 = automaton.getSuccessors(reachedStateFrom2, input).iterator().next();
				if (!successorOfInputAndState1.equals(successorOfInputAndState2)) {
					System.out.println(
							"(s" + indexOf(automaton, reachedStateFrom1) + ") " + input + "/" + successorOfInputAndState1
							+  " (s" + indexOf(automaton, successorOfInputAndState1) + ") " +
							" != (s" + indexOf(automaton, reachedStateFrom2) + ") " + input  + "/" + successorOfInputAndState2
							+  " (s" + indexOf(automaton, successorOfInputAndState2) + ") ");
					selectedMiddlePart.add(input);
					diffFound = true;
					break;
				}
			}
			if (diffFound) {
				break;
			}

			Tuple2<S, S> reachedStatePair = new Tuple2<>(reachedStateFrom1, reachedStateFrom2);
			reachedSatePairs.add(reachedStatePair);

			middleParts.remove(0);
			for (I input : symbols) {
				S successorOfInputAndState1 = automaton.getSuccessors(reachedStateFrom1, input).iterator().next();
				S successorOfInputAndState2 = automaton.getSuccessors(reachedStateFrom2, input).iterator().next();
				Tuple2<S, S> statePairAfterSymbol = new Tuple2<S, S>(successorOfInputAndState1, successorOfInputAndState2);
				if (!reachedSatePairs.contains(statePairAfterSymbol)) {
					List<I> newMiddlePart = new ArrayList<I>(selectedMiddlePart);
					newMiddlePart.add(input);
					middleParts.add(newMiddlePart);
				}
			}
			selectedMiddlePart = middleParts.get(0);
		}
		return selectedMiddlePart;
	}
	
	public S getState(FiniteAlphabetAutomaton<S, I, T> automaton, List<I> symbols) {
		S startState = automaton.getInitialStates().iterator().next();
		S currentState = startState;
		for (I symbol : symbols) {
			currentState = automaton.getSuccessors(currentState, symbol).iterator().next();
		}

		return currentState;
	}
}
