package metricTool;

import java.util.HashMap;

import org.eclipse.jdt.core.IJavaProject;

public interface IMetricCalculator {

	public boolean calculateMetric(IJavaProject in);
	
	public HashMap<String, Double> getResults();
	
}
