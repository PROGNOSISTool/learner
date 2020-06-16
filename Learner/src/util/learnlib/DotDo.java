package util.learnlib;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import net.automatalib.automata.FiniteAlphabetAutomaton;
import net.automatalib.words.Alphabet;
import util.ExceptionAdapter;

import net.automatalib.serialization.dot.DOTParsers;
import net.automatalib.serialization.InputModelDeserializer;
import net.automatalib.serialization.dot.GraphDOT;

public class DotDo {
	// policy : convert into method throwing unchecked exception
	public static FiniteAlphabetAutomaton readDotFile(String filepath)  {
		try {
			return readFile(filepath);
		} catch (IOException ex) {
			throw new ExceptionAdapter(ex);
		}
	}

	public static FiniteAlphabetAutomaton readFile(String filename) throws IOException {
		File file = new File(filename);
		InputModelDeserializer parser = DOTParsers.mealy();
		FiniteAlphabetAutomaton machine = (FiniteAlphabetAutomaton) parser.readModel(file);
		return machine;
	}

    // policy : convert into method throwing unchecked exception
	static public void writeDotFile(FiniteAlphabetAutomaton automaton, Alphabet alphabet, String filepath) throws IOException {
		writeFile(automaton, alphabet, filepath);
	}

	//policy:
	//  write dotfile with red double circeled start state
	public static void writeFile(FiniteAlphabetAutomaton automaton, Alphabet alphabet, String filepath) throws IOException {
		BufferedWriter outstream = new BufferedWriter(new FileWriter(filepath));
		write(automaton, alphabet, outstream);
		outstream.close();
	}

	/* write
	 *   same as writeFile but then to Appendable instead of filepath
	 *
	 */
	public static void write(FiniteAlphabetAutomaton automaton, Alphabet alphabet, Appendable out) {
		try {
			GraphDOT.write(automaton, alphabet, out);
		} catch (IOException e) {
			e.printStackTrace();
			throw new ExceptionAdapter(e);
		}
	}
}
