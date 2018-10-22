package metric.correlation.analysis.calculation;

import java.util.Collection;
import java.util.HashMap;

import org.eclipse.jdt.core.IJavaProject;

public interface IMetricCalculator {

	public boolean calculateMetric(IJavaProject project, String productName, String vendorName, String version);
	
	public HashMap<String, Double> getResults();

	public Collection<? extends String> getMetricKeys();
}
