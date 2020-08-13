package util.learnlib;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.Queue;

import net.automatalib.automata.transducers.MealyMachine;
import net.automatalib.automata.concepts.StateIDs;
import net.automatalib.words.Alphabet;
import net.automatalib.words.Word;
import util.Tuple2;

public class AutomatonUtils {

	public static <S, I, T, O> List<S> getStatesInBFSOrder(MealyMachine<S, I, T, O> automaton, Alphabet<I> alphabet) {
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

	public static <S, I, T, O> List<I> traceToState(MealyMachine<S, I, T, O> automaton, Alphabet<I> alphabet, int stateId) {
		S state = get(automaton, stateId);
		return traceToState(automaton, alphabet, state);
	}

	/**
	 * Gives a minimal length path from the start state to the given state. We build this path based
	 * on the random value generator. Note, there can be several minimal paths to the given state. We only
	 * return one. The length is the first order. Traces of equal length are order by the position of the last input
	 * in the alphabet.
	 */
	public static <S, I, T, O> List<I> traceToState(MealyMachine<S, I, T, O> automaton, Alphabet<I> alphabet, S state) {
		StateIDs<S> stateIDs = automaton.stateIDs();
		List<I> selectedMiddlePart = new ArrayList<I>();
		List<List<I>> middleParts = new ArrayList<List<I>>();
		middleParts.add(selectedMiddlePart);
		List<S> reachedStates = new ArrayList<S>();
		while(true) {
			List<I> traceToState = new ArrayList<I>(selectedMiddlePart);
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

	public static <S, I, T, O> List<I> distinguishingSeq(MealyMachine<S, I, T, O> automaton, Alphabet<I> alphabet, int stateId1, int stateId2) {
		S state1 = get(automaton, stateId1);
		S state2 = get(automaton, stateId2);
		return getDistinguishingSeq(automaton, alphabet, state1, state2);
	}

	public static <I> Word<I> createWordFromSymbols(List<I> symbols) {
		return Word.fromList(symbols);
	}

	public static <S, I, T, O> int indexOf(MealyMachine<S, I, T, O> automaton, S state) {
		StateIDs<S> stateIds = automaton.stateIDs();
		return stateIds.getStateId(state);
	}

	public static <S, I, T, O> S get(MealyMachine<S, I, T, O> automaton, int stateId) {
		StateIDs<S> stateIds = automaton.stateIDs();
		return stateIds.getState(stateId);
	}

	public static <I> List<I> buildSymbols(Collection<I> trace) {
		return new ArrayList<I>(trace);
	}

	private static <S, I, T, O> List<I> getDistinguishingSeq(MealyMachine<S, I, T, O> automaton,
		Alphabet<I> alphabet, S state1, S state2) {
		List<List<I>> middleParts = new ArrayList<List<I>>();
		List<I> traceToState1 = traceToState(automaton, alphabet, state1);
		List<I> traceToState2 = traceToState(automaton, alphabet, state2);
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
			for (I input : alphabet) {
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
			for (I input : alphabet) {
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

	public static <S, I, T, O> S getState(MealyMachine<S, I, T, O> automaton, List<I> alphabet) {
		S currentState = automaton.getInitialStates().iterator().next();
		for (I symbol : alphabet) {
			currentState = automaton.getSuccessors(currentState, symbol).iterator().next();
		}

		return currentState;
	}
}
