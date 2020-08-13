package debug;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Map.Entry;

import learner.*;
import util.Log;
import util.Tuple2;
import de.ls5.jlearn.interfaces.Symbol;
import de.ls5.jlearn.interfaces.Word;

public class TraceRunner {
	private static final String PATH = "testtrace.txt";

	public static final String START = 		"\n****** INPUTS  ******\n";
	public static final String SEPARATOR = 	"\n****** OUTPUTS ******\n";
	public static final String END = 		"\n*********************\n";

	private final Map<List<String>, Integer> outcomes = new HashMap<List<String>, Integer>();
	private final SocketSUL socketSul;
	private final List<String> inputTrace;
	//private final CacheInputValidator validator;

	public static Tuple2<List<String>,Integer> readTraceAndIterations() {
	    List<String> trace;
	    try {
            trace = Files.readAllLines(Paths.get(PATH), StandardCharsets.US_ASCII);
        } catch (IOException e) {
            System.out.println("usage of java tracerunner: create a file '" + PATH + "' with the input on each line', optionally preceded by the number of times the input should be repeated");
            return null;
        }
        ListIterator<String> it = trace.listIterator();
        System.out.println("TRACE FILE: ");
        int i = 1;
        while(it.hasNext()) {
            String line = it.next().trim();
            System.out.print((i++) + ": " + line);
            if (line.startsWith("#") || line.startsWith("!")) {
                it.remove();
                System.out.println(" (skipped)");
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
        int iterations;
        try {
            iterations = Integer.parseInt(trace.get(0));
            trace.remove(0);
        } catch (NumberFormatException e) {
            iterations = 1;
        }
        return new Tuple2<List<String>,Integer>(trace, iterations);
	}

	public static void main(String[] args) throws IOException {
		Main.handleArgs(args);
		Tuple2<List<String>, Integer> traceAndIncrement = readTraceAndIterations();
		List<String> trace = traceAndIncrement.tuple0;
		Integer iterations = traceAndIncrement.tuple1;

		Log.fatal("Start running trace " + iterations + " times");

		Main.setupOutput("trace runner output.txt");
		Config config = Main.createConfig();

		SutInterface sutInterface = Main.createSutInterface(config);

		SULConfig sulConfig = Main.readConfig(config, sutInterface);
		sulConfig.exitIfInvalid = false;

		SocketSUL sul = new SocketSUL(sulConfig);
		TraceRunner traceRunner = new TraceRunner(trace, sul);
		traceRunner.testTrace(iterations);
		sul.stop();
		System.out.println(traceRunner.getResults());
	}

	public String getResults() {
		StringBuilder sb = new StringBuilder();
		sb.append(START);
		sb.append("input:").append(this.inputTrace);
		sb.append(SEPARATOR);
		List<Entry<List<String>, Integer>> orderedEntries = new ArrayList<>(this.outcomes.size());
		orderedEntries.addAll(this.outcomes.entrySet());
		orderedEntries.sort(new Comparator<Entry<List<String>, Integer>>() {
			@Override
			public int compare(Entry<List<String>, Integer> arg0,
							   Entry<List<String>, Integer> arg1) {
				return Integer.compare(arg0.getValue(), arg1.getValue());
			}
		});
		for (Entry<List<String>, Integer> entry : orderedEntries) {
			sb.append(entry.getValue().toString()).append(": ").append(entry.getKey()).append("\n");
		}
		sb.append(END);
		return sb.toString();
	}

	public TraceRunner(Word word, SocketSUL socketSul) {
		List<String> inputString = new ArrayList<>(word.size());
		for (Symbol symbol : word.getSymbolList()) {
			inputString.add(symbol.toString());
		}
		this.inputTrace = inputString;
		this.socketSul = socketSul;
	}

	public TraceRunner(List<String> inputTrace, SocketSUL socketSul) {
		this.inputTrace = inputTrace;
		this.socketSul = socketSul;
	}

	public void testTrace(int iterations) {
		for (int i = 0; i < iterations; i++) {
			boolean check = runTrace((i+1));
			if (!check) {
			    break;
			}

		}
		socketSul.pre();
	}

	protected boolean runTrace(int printNumber) {
		List<String> outcome = new LinkedList<String>();
		boolean checkResult = true;
		socketSul.pre();
		System.out.println("# " + printNumber);
		//System.out.println("# " + number + " @@@ " + this.sutWrapper.toString());
		for (String input : inputTrace) {
			String output;
			if (input.equals("RESET")) {
				this.socketSul.pre();
				output = "RESET";
			} else {
				output = this.socketSul.step(input);
			}
			//System.out.println("# " + number + " >>> " + input + " >>> " + output);
			//System.out.println("# " + number + " @@@ " + this.sutWrapper.toString());
			outcome.add(output);
		}
		Integer currentCounter = outcomes.get(outcome);
		if (currentCounter == null) {
			currentCounter = 0;
		}
		outcomes.put(outcome, currentCounter + 1);
		return checkResult;
	}
}
