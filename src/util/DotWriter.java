package util;

import net.automatalib.automata.transducers.MealyMachine;
import net.automatalib.serialization.dot.GraphDOT;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collection;

public class DotWriter {

    // policy : convert into method throwing unchecked exception
	public static <O> void writeDotFile(MealyMachine<?, String, ?, O> automaton, Collection<? extends String> inputAlphabet, String filepath) throws IOException {
		writeFile(automaton, inputAlphabet, filepath);
	}

	//policy:
	//  write dotfile with red double circeled start state
	public static <O> void writeFile(MealyMachine<?, String, ?, O> automaton, Collection<? extends String> inputAlphabet, String filepath) throws IOException {
		BufferedWriter outstream = new BufferedWriter(new FileWriter(filepath));
		write(automaton, inputAlphabet, outstream);
		outstream.close();
	}

	/* write
	 *   same as writeFile but then to Appendable instead of filepath
	 *
	 */
	public static <O> void write(MealyMachine<?, String, ?, O> automaton, Collection<? extends String> inputAlphabet, Appendable out) {
		try {
			GraphDOT.write(automaton, inputAlphabet, out);
		} catch (IOException e) {
			e.printStackTrace();
			throw new ExceptionAdapter(e);
		}
	}
}
