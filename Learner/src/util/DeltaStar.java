package util;

import net.automatalib.words.Word;

import java.io.File;
import java.util.*;

public class DeltaStar {
	public static void computeCex(Collection<String> eqOracleFilenames, Collection<String> memOracleFilenames, String outputFilename) {
		Set<Word<String>> counterExamples = computeDeltaStar(eqOracleFilenames);
		Set<Word<String>> queries = computeDeltaStar(memOracleFilenames);
		Set<Word<String>> finalSet = SetOps.union(counterExamples, queries);
		File file = FileManager.createFile(outputFilename, true);
		FileManager.appendToFile(file, finalSet);
	}

	private static Set<Word<String>> computeDeltaStar(Collection<String> filenames) {
		Collection<Set<Word<String>>> sets = new HashSet<>();
		for (String filename : filenames) {
			sets.add(FileManager.readQueriesFromFile(filename));
		}
		return SetOps.deltaStar(sets);
	}

	public static void main(String[] args) {
		List<String> eqOracleFilenames = Arrays.asList(args[0].split(","));
		List<String> memOracleFilenames = Arrays.asList(args[1].split(","));
		String outputFilename = args[2];
		computeCex(eqOracleFilenames, memOracleFilenames, outputFilename);
	}
}
