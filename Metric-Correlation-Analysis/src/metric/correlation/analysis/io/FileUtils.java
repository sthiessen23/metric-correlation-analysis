package metric.correlation.analysis.io;

import java.io.File;

public class FileUtils {

	/**
	 * This method recursively deletes a file
	 * 
	 * @param file The file
	 */
	public static void recursiveDelete(File file) {
		if (file.exists()) {
			for (File f : file.listFiles()) {
				if (f.isDirectory()) {
					recursiveDelete(f);
					f.delete();
				} else {
					f.delete();
				}
			}
		}
	}
}
