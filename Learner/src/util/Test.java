package util;

import net.automatalib.automata.transducers.impl.compact.CompactMealy;
import net.automatalib.serialization.InputModelData;
import net.automatalib.serialization.InputModelDeserializer;
import net.automatalib.serialization.dot.DOTParsers;
import net.automatalib.serialization.dot.GraphDOT;

import java.io.*;

public class Test {
	private static String DOT_IN = "input.dot";
	private static String DOT_OUT = "output.dot";

	private static CompactMealy<String, String> readFile(String filename) throws IOException {
		File file = new File(filename);

		// Let's print the content just to be sure.
		FileInputStream fis = new FileInputStream(file);
		int oneByte;
		while ((oneByte = fis.read()) != -1) {
			System.out.write(oneByte);
		}
		System.out.flush();

		InputModelDeserializer<String, CompactMealy<String,String>> parser = DOTParsers.mealy();
		InputModelData<String, CompactMealy<String, String>> machine = parser.readModel(file);
		return machine.model;
	}

	private static <I> void saveToFile(String filename, CompactMealy<String, String> model) {
		try (BufferedWriter out = new BufferedWriter(new FileWriter(filename))) {
			GraphDOT.write(model, model.getInputAlphabet(), System.out);
			GraphDOT.write(model, model.getInputAlphabet(), out);
			System.out.flush();
		} catch (IOException exception) {
			exception.printStackTrace();
		}
	}

	public static void main(String[] args) throws IOException {
		CompactMealy<String, String> mealyMachine = readFile(DOT_IN);
		saveToFile(DOT_OUT, mealyMachine);
	}
}
