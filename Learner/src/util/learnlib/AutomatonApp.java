package util.learnlib;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Collection;
import java.util.Deque;
import java.util.List;
import java.util.ListIterator;

import de.ls5.jlearn.interfaces.State;
import de.ls5.jlearn.interfaces.Symbol;
import net.automatalib.automata.FiniteAlphabetAutomaton;
import net.automatalib.automata.transducers.MealyMachine;
import net.automatalib.automata.transducers.impl.compact.CompactMealy;

public class AutomatonApp {
	private final BufferedReader in;
	private final PrintStream out;
	private final Deque<String> commands;

	public AutomatonApp(BufferedReader in, PrintStream out) {
		this.in = in;
		this.out = out;
		this.commands = new ArrayDeque<String>();
	}

	public AutomatonApp() {
		this(new BufferedReader(new InputStreamReader(System.in)), System.out);
	}

	public void bufferCommands(Collection<String> commands) {
		this.commands.addAll(commands);
	}

	private String ask(String msg) throws IOException{
		out.println(msg);
		if (!commands.isEmpty()) {
			return commands.remove();
		}
		return in.readLine().trim();
	}

	public List<String> readTrace(String PATH) throws IOException {
		List<String> trace;
		trace = Files.readAllLines(Paths.get(PATH), StandardCharsets.US_ASCII);
		ListIterator<String> it = trace.listIterator();
		while(it.hasNext()) {
			String line = it.next();
			if (line.startsWith("#") || line.startsWith("!")) {
				it.remove();
			} else {
				 if ( line.isEmpty()) {
					 it.remove();
					 while (it.hasNext()) {
						 it.next();
						 it.remove();
					 }
				 } else {
					 System.out.println();
				 }
			}
		}
		return trace;
	}

	public Collection<String> getRoute(CompactMealy<String, String> automaton, Integer startingState, List<String> inputSeq) throws AppException{
		Deque<String> strings = new ArrayDeque<String>();

		Integer currentState = startingState;
		for (String input : inputSeq) {
			String transitionOutput = automaton.getTransitionOutput(automaton.getTransition(currentState, input));
			if (transitionOutput == null) {
				throw new AppException("Input " + input + " not defined for state s" + automaton.getState(currentState));
			}
			strings.add(input +"\\"+ transitionOutput + " (" + AutomatonUtils.indexOf(automaton, automaton.getSuccessor(currentState, input)) + ") " );
			if (inputSeq.size() > 5) {
				strings.add("\n");
			}
			currentState = automaton.getSuccessor(currentState, input);
		}
		return strings;
	}

	static class AppException extends Exception{
		public AppException(String cause) {
			super(cause);
		}
	}

	public void play() throws IOException {
		CompactMealy<String, String> loadedHyp = null;
		while (true) {
			try {
			out.println("Welcome to the hyp assistent. Today you can: " +
					"\n 1. Load a new hypothesis \n 2. Get trace to state \n " +
					"3. Get distinguishing seq between two states \n 4. Run trace \n " +
					"5. Relabel state machine based on access sequence mapping and dump it \n" +
					"6. Quit");

			String command = ask("Command:");
			switch(command) {
			case "1":
				String hyp = ask("Hypothesis:");
				loadedHyp = DotDo.readDotFile(hyp);
				if (loadedHyp != null) {
					out.println("Loaded successfully");
				}
				break;
			case "2":
				if (loadedHyp == null) {
					out.println("Load a hyp first!");
				} else {
					int stateId = Integer.parseInt(ask("State ID:"));
					List<String> traceToState = AutomatonUtils.traceToState(loadedHyp, loadedHyp.getInputAlphabet(), stateId);
					Collection<String> strings = getRoute(loadedHyp, loadedHyp.getInitialState(), traceToState);
					out.println("Trace to state: " + traceToState);
					out.println("Trace to state(full):  " + strings);
				}
				break;

			case "3":
				if (loadedHyp == null) {
					out.println("Load a hyp first!");
				} else {
					int stateId1 = Integer.parseInt(ask("State ID1:"));
					int stateId2 = Integer.parseInt(ask("State ID2:"));
					List<String> distSeq = AutomatonUtils.distinguishingSeq(loadedHyp, loadedHyp.getInputAlphabet(), stateId1, stateId2);
					out.println("Distinguishing trace: " + distSeq);
					Collection<String> strings1 = getRoute(loadedHyp, AutomatonUtils.get(loadedHyp, stateId1), distSeq);
					Collection<String> strings2 = getRoute(loadedHyp, AutomatonUtils.get(loadedHyp, stateId2), distSeq);
					out.println("Trace from state1: " + strings1);
					out.println("Trace from state2: " + strings2);
				}
				break;

			case "4":
				if (loadedHyp == null) {
					out.println("Load a hyp first!");
				} else {
					Collection<String> trace = readTrace("testtrace.txt");
					List<String> traceSymbols = AutomatonUtils.buildSymbols(trace);
					Collection<String> strings = getRoute(loadedHyp, loadedHyp.getInitialState(), traceSymbols);
					out.println("Trace run:" + strings);

				}
				break;

			case "5":
				out.println("Labelling hypothesis and dumping it");
				if (loadedHyp == null) {
					out.println("Load a hyp first!");
				} else {
					DotDo.writeDotFile(loadedHyp, loadedHyp.getInputAlphabet(), "tcp.dot");
				}
				break;
			case "6":
				out.println("Byee");
				return ;
			}
			} catch(AppException exc) {
				out.print("Non terminal exception: " + exc.getMessage());
			}
		}
	}

	public static void main(String[] args) throws IOException {
		AutomatonApp app = new AutomatonApp();
		if (args.length > 0) {
			app.bufferCommands(Arrays.asList(args));
		}
		app.play();
	}

}
