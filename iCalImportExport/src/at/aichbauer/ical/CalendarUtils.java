package at.aichbauer.ical;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public abstract class CalendarUtils {
	public static List<File> searchFiles(File root, String... extension) {
		List<File> files = new ArrayList<File>();
		searchFiles(root, files, extension);
		return files;
	}

	private static void searchFiles(File root, List<File> files, String... extension) {
		if (root.isFile()) {
			for (String string : extension) {
				if (root.toString().endsWith(string)) {
					files.add(root);
				}
			}
		} else {
			File[] children = root.listFiles();
			if (children != null) {
				for (File file : children) {
					searchFiles(file, files, extension);
				}
			}
		}
	}
}
