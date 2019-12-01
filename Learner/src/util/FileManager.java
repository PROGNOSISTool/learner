package util;

import java.nio.file.FileSystems;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public class FileManager {
	
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
}
