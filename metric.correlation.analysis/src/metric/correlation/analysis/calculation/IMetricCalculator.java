package metric.correlation.analysis.calculation;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

import org.eclipse.jdt.core.IJavaProject;

public interface IMetricCalculator extends Comparable<IMetricCalculator> {

	public boolean calculateMetric(IJavaProject project, String productName, String vendorName, String version, final Map<String, String> map);
	
	public Map<String, String> getResults();

	public Collection<String> getMetricKeys();
	
	public Set<Class<? extends IMetricCalculator>> getDependencies();
	
	@Override
	public default int compareTo(IMetricCalculator other) {
		if(other.getClass().equals(getClass())) {
			return 0;
		}
		if(getDependencies().contains(other.getClass())) {
			if(other.getDependencies().contains(getClass())) {
				throw new IllegalStateException("There is a cycle in the dependencies of metric calculators");
			}
			return 1;
		}
		return -1;
	}
}
