package metric.correlation.analysis;

import java.io.BufferedReader;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStreamReader;
import org.gravity.eclipse.os.OperationSystem;
import org.gravity.eclipse.os.UnsupportedOperationSystemException;

public class GradleBuild {

	/**
	 * @param src_code
	 *            - The downloaded apk src
	 * @return the apk for this application
	 * @throws UnsupportedOperationSystemException
	 *             when not on Windows/Linux
	 */
	public static File buildApk(File src_code) throws UnsupportedOperationSystemException {

		if (new File(src_code, "build").exists()) {
			System.out.println("Build already exists!");
			return getApk(src_code);
		}

		String cmd = "cd " + src_code.getPath() + " && gradlew assembleDebug";
		Runtime run = Runtime.getRuntime();
		Process process;

		try {
			switch (OperationSystem.getCurrentOS()) {

			case WINDOWS:
				process = run.exec("cmd /c \"" + cmd + " && exit\"");
				break;

			case LINUX:
				process = run.exec("./gradlew assembleDebug", null, src_code);
				break;

			default:
				throw new UnsupportedOperationSystemException();
			}

			BufferedReader stream_reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
			String line;

			while ((line = stream_reader.readLine()) != null) {
				System.out.println("> " + line); //$NON-NLS-1$
			}

			process.waitFor();
			process.destroy();

			return getApk(src_code);

		} catch (InterruptedException | IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	/**
	 * @param src_code the directory of the application
	 * @return a compiled apk file
	 */
	private static File getApk(File src_code) {
		File[] list = src_code.listFiles();
		
		if (list == null) {
			throw new RuntimeException("Directory is empty!");
		}
		
		File compiled_apk = null;
		for (File file : list) {
			
			if (file.isDirectory()) {
				return getApk(file);
				
			} else {
				File[] apklist = file.getParentFile().listFiles(new FilenameFilter() {
					@Override
					public boolean accept(File dir, String name) {
						return name.endsWith(".apk");
					}
				});
				
				if (apklist.length > 0) {
					return compiled_apk = apklist[0];
				}
			}
		}
		return compiled_apk;
	}

	/**
	 * UnsupportedOperationSystemException
	 * @param src_code the src to clean
	 * @return true if it worked, else false
	 * @throws UnsupportedOperationSystemException
	 */
	public static boolean cleanBuild(File src_code) throws UnsupportedOperationSystemException {
		String cmd = "cd " + src_code.getPath() + " && gradlew clean";
		Runtime run = Runtime.getRuntime();
		
		try {
			Process process;
			switch (OperationSystem.getCurrentOS()) {
			case WINDOWS:
				process = run.exec("cmd /c \"" + cmd + " && exit\"");
				break;
			case LINUX:
				process = run.exec("./gradlew clean", null, src_code);
				break;
			default:
				throw new UnsupportedOperationSystemException();
			}
			
			BufferedReader stream_reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
			String line;
			while ((line = stream_reader.readLine()) != null) {
				System.out.println("> " + line); //$NON-NLS-1$
			}
			
			process.waitFor();
			process.destroy();
			stream_reader.close();
			return true;
			
		} catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		return false;
	}

}
