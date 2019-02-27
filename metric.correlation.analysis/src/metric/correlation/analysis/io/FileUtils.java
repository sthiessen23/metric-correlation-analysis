package metric.correlation.analysis.io;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

/**
 * @author Antoniya Ivanova Offers some standard file-modifying abilities.
 */
public class FileUtils {

	private static final Logger LOGGER = Logger.getLogger(FileUtils.class);

	/**
	 * Creates a directory
	 * 
	 * @param directoryPath - The path where the file will be created
	 * @return - The new File with the given path
	 */
	public File createDirectory(String directoryPath) {
		try {
			File dir = new File(directoryPath);
			if (dir.exists())
				return dir;

			if (dir.mkdirs())
				return dir;

		} catch (Exception e) {
			LOGGER.log(Level.ERROR, "Could not create directory " + directoryPath);
			LOGGER.log(Level.ERROR, e.getMessage(), e);
		}

		return null;
	}

	/**
	 * Unzips a zip file to a given location
	 * 
	 * @param zipFilePath   - The path of the ZIP file
	 * @param unzipLocation - The location to be unzipped
	 */
	public void unzip(final String zipFilePath, final String unzipLocation) {
		if (!(Files.exists(Paths.get(unzipLocation)))) {
			try {
				Files.createDirectories(Paths.get(unzipLocation));
			} catch (IOException e) {

				LOGGER.log(Level.ERROR, e.getMessage(), e);
			}
		}
		try (ZipInputStream zipInputStream = new ZipInputStream(new FileInputStream(zipFilePath))) {
			ZipEntry entry = zipInputStream.getNextEntry();
			while (entry != null) {
				Path filePath = Paths.get(unzipLocation, entry.getName());
				if (!entry.isDirectory()) {
					unzipFiles(zipInputStream, filePath);
				} else {
					Files.createDirectories(filePath);
				}

				zipInputStream.closeEntry();
				entry = zipInputStream.getNextEntry();
			}
		} catch (Exception e) {
			LOGGER.log(Level.ERROR, e.getMessage(), e);
		}
	}

	public void unzipFiles(final ZipInputStream zipInputStream, final Path unzipFilePath) {
		try (BufferedOutputStream bos = new BufferedOutputStream(
				new FileOutputStream(unzipFilePath.toAbsolutePath().toString()))) {
			byte[] bytesIn = new byte[1024];
			int read = 0;
			while ((read = zipInputStream.read(bytesIn)) != -1) {
				bos.write(bytesIn, 0, read);
			}
		} catch (Exception e) {
			LOGGER.log(Level.ERROR, e.getMessage(), e);
		}
	}

	/**
	 * This method recursively deletes a file
	 * 
	 * @param file The file
	 * @return true, iff the file has been deleted successfully
	 */
	public static boolean recursiveDelete(File file) {
		boolean success = true;
		if (file.exists()) {
			if (file.isDirectory()) {
				for (File f : file.listFiles()) {
					if (f.isDirectory()) {
						success &= recursiveDelete(f);
						success &= f.delete();
					} else {
						success &= f.delete();
					}
				}
			}
			success = file.delete();
		}
		return success;
	}

}
