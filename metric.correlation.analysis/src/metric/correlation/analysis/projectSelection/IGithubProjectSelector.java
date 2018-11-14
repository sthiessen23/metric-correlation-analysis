package metric.correlation.analysis.projectSelection;

public interface IGithubProjectSelector {
	boolean accept(String projectname, String oAuthToken);
}
