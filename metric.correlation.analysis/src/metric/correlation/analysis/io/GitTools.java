package metric.correlation.analysis.io;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Stack;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.gravity.eclipse.os.OperationSystem;
import org.gravity.eclipse.os.UnsupportedOperationSystemException;

import metric.correlation.analysis.GradleBuild;

public class GitTools {

	private static final Logger LOGGER = Logger.getLogger(GitTools.class);

	protected static boolean gitClone(String url, File destination)
			throws UnsupportedOperationSystemException, GitCloneException {
		return gitClone(url, destination, false);
	}

	public static boolean gitClone(String url, File destination, boolean replace)
			throws UnsupportedOperationSystemException, GitCloneException {
		if (!destination.exists()) {
			destination.mkdirs();
		}
		String productName = url.substring(url.lastIndexOf('/') + 1, url.length() - 4);
		File repository = new File(destination, productName);
		if (repository.exists()) {
			if (replace) {
				Stack<File> files = new Stack<>();
				files.add(repository);
				Stack<File> delete = new Stack<>();
				delete.add(repository);
				while (!files.isEmpty()) {
					File next = files.pop();
					for (File f : next.listFiles()) {
						if (f.isDirectory()) {
							files.add(f);
							delete.add(f);
						} else {
							f.delete();
						}
					}
				}
				while (!delete.isEmpty()) {
					delete.pop().delete();
				}
			} else {
				throw new GitCloneException("There is already a repository with the name \"" + productName + "\".");
			}
		}
		String cmd = "cd " + destination.getAbsolutePath() + " && " + "git clone --recursive " + url;
		Runtime run = Runtime.getRuntime();
		try {
			Process process;
			switch (OperationSystem.getCurrentOS()) {
			case WINDOWS:
				process = run.exec("cmd /c \"" + cmd);
				break;
			case LINUX:
				cmd = "git clone --recursive " + url;
				process = run.exec(cmd, null, destination);
				break;
			default:
				throw new UnsupportedOperationSystemException("Program is not compatibel with the Operating System");
			}

			try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
				String line;
				while ((line = reader.readLine()) != null) {
					LOGGER.log(Level.ERROR, "> " + line); //$NON-NLS-1$
				}
			} catch (IOException e) {
				LOGGER.log(Level.ERROR, e.getMessage(), e);
			}
			process.waitFor();
			process.destroy();
			return process.exitValue() == 0;
		} catch (IOException e) {
			LOGGER.log(Level.ERROR, e.getMessage(), e);
			return false;
		} catch (InterruptedException e) {
			LOGGER.log(Level.ERROR, e.getMessage(), e);
			Thread.currentThread().interrupt();
			return false;
		}
	}

	public static boolean changeVersion(File src_code, String id) throws UnsupportedOperationSystemException {

		LOGGER.log(Level.INFO, "Change version to commit: " + id);
		File buildDir = new File(src_code, "build");
		if (buildDir.exists()) {
			GradleBuild.cleanBuild(src_code);
			FileUtils.recursiveDelete(buildDir);
		}
		String cmd = "cd " + src_code.getPath() + " && " + "git checkout " + id + " .";
		Runtime run = Runtime.getRuntime();
		try {
			Process process;
			switch (OperationSystem.getCurrentOS()) {
			case WINDOWS:
				process = run.exec("cmd /c \"" + cmd + " && exit\"");
				break;
			case LINUX:
				cmd = "git checkout " + id + " .";
				process = run.exec(cmd, null, src_code);
				break;
			default:
				throw new UnsupportedOperationSystemException("Program is not compatibel with the Operating System");
			}

			try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
				String line;
				while ((line = reader.readLine()) != null) {
					LOGGER.log(Level.INFO, "> " + line); //$NON-NLS-1$
				}
				process.waitFor();
				process.destroy();
			}
			return true;

		} catch (IOException e) {
			LOGGER.log(Level.ERROR, e.getMessage(), e);
		} catch (InterruptedException e) {
			LOGGER.log(Level.ERROR, e.getMessage(), e);
			Thread.currentThread().interrupt();
		}

		return false;
	}
}
