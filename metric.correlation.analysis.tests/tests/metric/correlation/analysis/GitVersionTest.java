package metric.correlation.analysis;

import static org.junit.Assert.assertFalse;

import java.io.File;
import java.io.IOException;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ResetCommand.ResetType;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.InvalidRemoteException;
import org.eclipse.jgit.api.errors.TransportException;
import org.junit.Test;

public class GitVersionTest {

	@Test
	public void testChange() throws InvalidRemoteException, TransportException, GitAPIException, IOException {
		String path = "../metric.correlation.analysis/repositories/junit4";
		String id = "b51fa17fc6a750a17436f9f38c139a7b5228171f";
		File f = new File(path);
		Git git = Git.open(f);
		git.reset().setMode(ResetType.HARD).setRef(id).call();
		assertFalse(git.status().call().hasUncommittedChanges());
				
	}
}
