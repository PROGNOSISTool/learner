package util;

import de.learnlib.api.logging.LearnLogger;
import net.automatalib.words.Word;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;

import java.nio.charset.StandardCharsets;
import com.thoughtworks.xstream.XStream;


public class FileManager {
	private static final LearnLogger logger = LearnLogger.getLogger("Learner");

	public static void copyFromTo(Path fromPath, Path toPath) throws Exception{
		File fromFile = fromPath.toFile();
		if(fromFile.exists() && fromFile.isFile()) {
			Files.copy(fromPath, toPath, StandardCopyOption.REPLACE_EXISTING);
		}
		else {
			if(!Files.exists(toPath)) {
				Files.createDirectories(toPath);
			}
			for(String subFileName : Objects.requireNonNull(fromFile.list())) {
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

	public static <T> void writeStateToFile(T state, String filename) {
		if (state == null) {
			logger.error("Could not write uninitialized cache state.");
			return;
		}

		try {
			File file = new File(filename);
			Files.createDirectories(file.getParentFile().toPath());
		} catch (IOException e) {
			logger.error("Failed to create parent directories: " + e);
		}

		try (OutputStream stream = new FileOutputStream(filename);
			 OutputStream buffer = new BufferedOutputStream(stream);
			 ObjectOutput output = new ObjectOutputStream(buffer)) {
			XStream X_STREAM = new XStream();
			X_STREAM.allowTypesByRegExp(new String[] {"net.automatalib.*", "de.learnlib.*"});
			final byte[] cacheData = X_STREAM.toXML(state).getBytes(StandardCharsets.UTF_8);
			output.writeObject(cacheData);
		} catch (IOException ex){
			System.err.println("Could not write cache state: (" + ex + ")");
		}
	}

	public static <T> T readStateFromFile(String fileName) {
		try {
			Path path = Paths.get(fileName);
			byte[] data = Files.readAllBytes(path);
			String dataString = new String(data, StandardCharsets.UTF_8);
			XStream X_STREAM = new XStream();
			X_STREAM.allowTypesByRegExp(new String[] {"net.automatalib.*", "de.learnlib.*"});
			return (T) X_STREAM.fromXML(dataString);
		} catch (IOException e) {
			logger.error("Failed to read state.");
		}
		return null;
	}
}
