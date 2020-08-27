package util;

import de.learnlib.api.logging.LearnLogger;
import net.automatalib.words.Word;

import java.io.*;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;

public class FileManager {
	private static final LearnLogger logger = LearnLogger.getLogger("Learner");

	public static void copyFromTo(String from, String to) throws Exception{
		Path fromPath = FileSystems.getDefault().getPath(from);
		Path toPath = FileSystems.getDefault().getPath(to);
		if(fromPath.toFile().exists()) {
			copyFromTo(fromPath, toPath);
		}
	}

	public static void copyFromTo(Path fromPath, Path toPath) throws Exception{
		File fromFile = fromPath.toFile();
		if(fromFile.exists() && fromFile.isFile()) {
			Files.copy(fromPath, toPath, StandardCopyOption.REPLACE_EXISTING);
		}
		else {
			if(!Files.exists(toPath)) {
				Files.createDirectories(toPath);
			}
			for(String subFileName : fromFile.list()) {
				copyFromTo(fromPath.resolve(subFileName), toPath.resolve(subFileName));
			}
		}
	}

	public static void appendToFile(File file, List<String> lines) {
		try {
			FileWriter fileWriter = new FileWriter(file, true);
			for (String line : lines) {
				fileWriter.write(line + "\n");
			}
			fileWriter.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void appendToFile(File file, Collection<Word<String>> queries) {
		List<String> queryStrings = new LinkedList<>();
		for (Word<String> query : queries) {
			queryStrings.add(query.toString());
		}
		appendToFile(file, queryStrings);
	}


	public static File createFile(String filename, Boolean recreate) {
		File file = new File(filename);
		try {
			if (recreate) {
				file.delete();
			}
			file.createNewFile();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return file;
	}


	public static Set<Word<String>> readQueriesFromFile(String filename)  {
		Set<Word<String>> queries = new HashSet<>();
		try {
			File file = new File(filename);
			FileReader fr = new FileReader(file);
			BufferedReader br = new BufferedReader(fr);
			String line;
			while((line = br.readLine()) != null) {
				line = line.replace("\n", "");
				LinkedList<String> symbolList = new  LinkedList<>(Arrays.asList(line.split(" ")));
				int separator = symbolList.indexOf("/");
				if (separator != -1) {
					symbolList.subList(separator, symbolList.size()).clear();
				}
				queries.add(Word.fromList(symbolList));
			}
			fr.close();
		} catch(FileNotFoundException e) {
			logger.info("External CounterExamples not found, creating pass-throw oracle.");
		} catch(IOException e) {
			e.printStackTrace();
		}
		return queries;
	}

	public static <T> void writeStateToFile(T state, String filename) {
		if (state == null) {
			logger.error("Could not write uninitialized cache state.");
			return;
		}

		try {
			File file = new File(filename);
			Files.createDirectories(file.getParentFile().toPath());
		} catch (IOException e) {
			logger.error("Failed to create parent directories: " + e.toString());
		}

		try (OutputStream stream = new FileOutputStream(filename);
			 OutputStream buffer = new BufferedOutputStream(stream);
			 ObjectOutput output = new ObjectOutputStream(buffer)) {
			output.writeObject(state);
		} catch (IOException ex){
			System.err.println("Could not write cache state: (" + ex.toString() + ")");
		}
	}

	public static <T> T readStateFromFile(String fileName) {
		try (InputStream file = new FileInputStream(fileName);
			 InputStream buffer = new BufferedInputStream(file);
			 ObjectInput input = new ObjectInputStream (buffer)) {
			@SuppressWarnings("unchecked")
			T state = (T) input.readObject();
			return state;
		} catch(ClassNotFoundException ex) {
			logger.error("Cache state file corrupt");
			return null;
		} catch(IOException ex) {
			logger.error("Could not read cache file");
			return null;
		}
	}
}
