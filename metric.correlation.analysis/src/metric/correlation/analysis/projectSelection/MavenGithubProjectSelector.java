package metric.correlation.analysis.projectSelection;

public class MavenGithubProjectSelector extends FileBasedGithubprojectSelector {

	public MavenGithubProjectSelector() {
		super("pom.xml");
	}

}
