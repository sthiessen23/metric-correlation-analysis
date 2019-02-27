package metric.correlation.analysis.io;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.errors.NoWorkTreeException;

public class GitTools implements Closeable {

	public static final Logger LOGGER = Logger.getLogger(GitTools.class);
	
	private Git git;

	protected GitTools(String url, File destination)
			throws GitCloneException {
		this(url, destination, false);
	}

	public GitTools(String url, File destination, boolean replace) throws GitCloneException {
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
		try {
			git = Git.cloneRepository()
					.setDirectory(repository)
					.setURI(url)
					.setCloneSubmodules(true)
					.call();
		} catch (GitAPIException e) {
			throw new GitCloneException(e);
		}
	}

	public boolean changeVersion(String id) {
		try {
			git.clean().setForce(true).setCleanDirectories(true).call();
			git.checkout().setCreateBranch(false).setName(id).call();
		} catch (NoWorkTreeException | GitAPIException e) {
			LOGGER.log(Level.ERROR, e.getLocalizedMessage(), e);
			return false;
		}
		return true;
	}

	@Override
	public void close() throws IOException {
		git.getRepository().close();
	}
}
