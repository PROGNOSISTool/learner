package util;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermission;
import java.util.HashSet;
import java.util.Objects;

public class Chmod {
	public static void set(int owner, int group, int others, boolean recursive, String pathName) throws IOException {
		Path path = Paths.get(pathName);
		HashSet<PosixFilePermission> perms = new HashSet<>();
		if ((owner & 0b100) != 0)
			perms.add(PosixFilePermission.OWNER_READ);
		if ((owner & 0b10) != 0)
			perms.add(PosixFilePermission.OWNER_WRITE);
		if ((owner & 0b1) != 0)
			perms.add(PosixFilePermission.OWNER_EXECUTE);
		if ((group & 0b100) != 0)
			perms.add(PosixFilePermission.GROUP_READ);
		if ((group & 0b10) != 0)
			perms.add(PosixFilePermission.GROUP_WRITE);
		if ((group & 0b1) != 0)
			perms.add(PosixFilePermission.GROUP_EXECUTE);
		if ((others & 0b100) != 0)
			perms.add(PosixFilePermission.OTHERS_READ);
		if ((others & 0b10) != 0)
			perms.add(PosixFilePermission.OTHERS_WRITE);
		if ((others & 0b1) != 0)
			perms.add(PosixFilePermission.OTHERS_EXECUTE);
		Files.setPosixFilePermissions(path, perms);
		File file = path.toFile();
		if (recursive && file.isDirectory()) {
			for (String child : Objects.requireNonNull(file.list())) {
				set(owner, group, others, true, child);
			}
		}
	}
}
