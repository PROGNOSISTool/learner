package util.learnlib;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import util.Tuple2;
import de.ls5.jlearn.interfaces.Alphabet;
import de.ls5.jlearn.interfaces.Automaton;
import de.ls5.jlearn.interfaces.State;
import de.ls5.jlearn.interfaces.Symbol;
import de.ls5.jlearn.interfaces.Word;
import de.ls5.jlearn.shared.SymbolImpl;
import de.ls5.jlearn.shared.WordImpl;

public class AutomatonUtils {

	public static List<State> getStatesInBFSOrder(Automaton automaton) {
		List<Symbol> inputs = automaton.getAlphabet().getSymbolList();
		java.util.Collections.sort(inputs);

		Queue<State> statestovisit = new LinkedList<State>();
		List<State> result = new ArrayList<State>();
		HashSet<State> states = new HashSet<State>(); // to check if state is
														// not seen already by
														// other transition

		statestovisit.offer(automaton.getStart());
		result.add(automaton.getStart());
		states.add(automaton.getStart());

		State current = (State) statestovisit.poll();
		while (current != null) {
			for (Symbol input : inputs) {
				State s = current.getTransitionState(input);
				if ((s != null) && (!states.contains(s))) {
					statestovisit.offer(s);
					result.add(s);
					states.add(s);
				}
			}

			if (statestovisit.isEmpty()) {
				break;
			}
			current = (State) statestovisit.poll();
		}

		return result;
	}
	
	public static List<Symbol> traceToState(Automaton automaton, int stateId) {
		State state = AutomatonUtils.get(automaton, stateId);
		return traceToState(automaton, state);
	}
	
	/**
	 * Gives a minimal length path from the start state to the given state. We build this path based 
	 * on the random value generator. Note, there can be several minimal paths to the given state. We only 
	 * return one. The length is the first order. Traces of equal length are order by the position of the last input
	 * in the alphabet.
	 */
	public static List<Symbol> traceToState(Automaton automaton, State state) {
		List<Symbol> alphabet = automaton.getAlphabet().getSymbolList();
		List<Symbol> selectedMiddlePart = new ArrayList<Symbol>();
		List<List<Symbol>> middleParts = new ArrayList<List<Symbol>>();
		middleParts.add(selectedMiddlePart);
		List<State> reachedStates = new ArrayList<State>();
		while(true) {
			List<Symbol> traceToState = new ArrayList<Symbol>();
			traceToState.addAll(selectedMiddlePart);
			State reachedState = getState(automaton, traceToState);
			if (reachedState.getId() == state.getId()) {
				break;
			}
			reachedStates.add(reachedState);
			middleParts.remove(0);
			for (Symbol input : alphabet) {
				// we only add middle parts which are relevant (those that lead to new states)
				if (!reachedStates.contains(reachedState.getTransitionState(input))) {
					List<Symbol> newMiddlePart = new ArrayList<Symbol>(selectedMiddlePart);
					newMiddlePart.add(input);
					middleParts.add(newMiddlePart);
				}
			}
			
			selectedMiddlePart = middleParts.get(0);
		}
		
		return selectedMiddlePart;
	}

	public static List<Symbol> distinguishingSeq(Automaton automaton, int stateId1, int stateId2) {
		Alphabet alpha = automaton.getAlphabet();
		List<Symbol> symbols = alpha.getSymbolList();
		State state1 = get(automaton, stateId1);
		State state2 = get(automaton, stateId2);
		List<Symbol> distSeq = getDistinguishingSeq(automaton, symbols, state1, state2);
		return distSeq;
	}
	
	public static Word createWordFromSymbols(Collection<Symbol> symbols) {
		Word word = new WordImpl();
		for (Symbol symbol : symbols) {
			word.addSymbol(symbol);
		}
		return word;
	}
	
	public static int indexOf(Automaton automaton, State state) {
		//return getStatesInBFSOrder(automaton).indexOf(state);
		return state.getId();
	}
	
	public static State get(Automaton automaton, int index) {
		//return getStatesInBFSOrder(automaton).get(index);
		return automaton.getAllStates().stream().filter(state -> state.getId() == index)
		.findAny().orElse(null);
	}
	
	public static List<Symbol> buildSymbols(Collection<String> trace) {
		List<Symbol> traceSymbols = new ArrayList<Symbol>();
		for (String str : trace) {
			traceSymbols.add(new SymbolImpl(str));
		}
		return traceSymbols;
	}

	private static List<Symbol> getDistinguishingSeq(Automaton automaton,
		List<Symbol> symbols, State state1, State state2) {
		List<List<Symbol>> middleParts = new ArrayList<List<Symbol>>();
		getState(automaton, Arrays.asList(new Symbol[]{new SymbolImpl("LISTEN")}));
		List<Symbol> traceToState1 = traceToState(automaton, state1);
		List<Symbol> traceToState2 = traceToState(automaton, state2);
		List<Tuple2<State,State>> reachedSatePairs =new ArrayList<>();

		List<Symbol> selectedMiddlePart = new ArrayList<Symbol>();
		middleParts.add(selectedMiddlePart);
		while(true) {
			List<Symbol> traceFromState1 = new ArrayList<Symbol>();
			traceFromState1.addAll(traceToState1);
			traceFromState1.addAll(selectedMiddlePart);
			List<Symbol> traceFromState2 = new ArrayList<Symbol>();
			traceFromState2.addAll(traceToState2);
			traceFromState2.addAll(selectedMiddlePart);
			State reachedStateFrom1 = getState(automaton, traceFromState1);
			State reachedStateFrom2 = getState(automaton, traceFromState2);
			
			boolean diffFound = false;
			for (Symbol input : symbols) {
				if (!reachedStateFrom1.getTransitionOutput(input).equals(reachedStateFrom2
						.getTransitionOutput(input))) {
					System.out.println(
							"(s" + indexOf(automaton,reachedStateFrom1) + ") " + input + "/" + reachedStateFrom1.getTransitionOutput(input) 
							+  " (s" + indexOf(automaton,reachedStateFrom1.getTransitionState(input)) + ") " +
							" != (s" + indexOf(automaton,reachedStateFrom2) + ") " + input  + "/" + reachedStateFrom2.getTransitionOutput(input) 
							+  " (s" + indexOf(automaton,reachedStateFrom2.getTransitionState(input)) + ") ");
					selectedMiddlePart.add(input);
					diffFound = true;
					break;
				}
			}
			if (diffFound) {
				break;
			}
			
			Tuple2<State,State> reachedStatePair = new Tuple2<>(reachedStateFrom1, reachedStateFrom2);
			reachedSatePairs.add(reachedStatePair);
			
			middleParts.remove(0);
			for (Symbol input : symbols) {
				State stateReachedAfterSymFromState1 = reachedStateFrom1.getTransitionState(input);
				State stateReachedAfterSymFromState2 = reachedStateFrom2.getTransitionState(input);
				Tuple2<State, State> statePairAfterSymbol = new Tuple2<State,State>(stateReachedAfterSymFromState1, stateReachedAfterSymFromState2);
				if (!reachedSatePairs.contains(statePairAfterSymbol)) {
					List<Symbol> newMiddlePart = new ArrayList<Symbol>(selectedMiddlePart);
					newMiddlePart.add(input);
					middleParts.add(newMiddlePart);
				}
			}
			
			selectedMiddlePart = middleParts.get(0);
		}
		
		return selectedMiddlePart;
	}
	
	
	
	public static State getState(Automaton automaton, List<Symbol> symbols ) {
		State currentState = automaton.getStart();
		for(Symbol symbol : symbols) {
			currentState = currentState.getTransitionState(symbol);
		}
		return currentState;
	}
}
