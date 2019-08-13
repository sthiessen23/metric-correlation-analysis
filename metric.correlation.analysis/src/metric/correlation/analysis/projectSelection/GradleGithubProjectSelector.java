package metric.correlation.analysis.projectSelection;

public class GradleGithubProjectSelector extends FileBasedGithubprojectSelector {

	public GradleGithubProjectSelector() {
		super("build.gradle");
	}

}
