package metric.correlation.analysis.io;

import java.io.File;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.gravity.eclipse.os.UnsupportedOperationSystemException;

import metric.correlation.analysis.GradleBuild;
import metric.correlation.analysis.commands.CommandExecuter;

public class GitTools {

	public static final Logger LOGGER = Logger.getLogger(GitTools.class);

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
				if (!FileUtils.recursiveDelete(repository)) {
					throw new GitCloneException("There is already a repository with the name \"" + productName
							+ "\" which couldn't be deleted.");
				}
			} else {
				throw new GitCloneException("There is already a repository with the name \"" + productName + "\".");
			}
		}
		return CommandExecuter.executeCommand(destination, "git clone --recursive " + url);
	}

	public static boolean changeVersion(File location, String id) throws UnsupportedOperationSystemException {
		LOGGER.log(Level.INFO, "Change version to commit: " + id);
		File buildDir = new File(location, "build");
		if (buildDir.exists()) {
			GradleBuild.cleanBuild(location);
			FileUtils.recursiveDelete(buildDir);
		}
		return CommandExecuter.executeCommand(location, "git checkout " + id + " .");
	}
}
