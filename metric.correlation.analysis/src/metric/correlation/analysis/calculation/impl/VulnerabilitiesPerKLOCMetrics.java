package metric.correlation.analysis.calculation.impl;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static metric.correlation.analysis.calculation.impl.VulnerabilitiesPerKLOCMetrics.MetricKeysImpl.*;

import org.eclipse.jdt.core.IJavaProject;

import metric.correlation.analysis.calculation.IMetricCalculator;

public class VulnerabilitiesPerKLOCMetrics implements IMetricCalculator {

	private Map<String, String> results;

	@Override
	public boolean calculateMetric(IJavaProject project, String productName, String vendorName, String version,
			final Map<String, String> map) {
		String llocKey = SourceMeterMetrics.MetricKeysImpl.LLOC.toString();
		String numberOfVulnerabilitiesKey = CVEMetrics.MetricKeysImpl.NUMBER_OF_VULNERABILITIES.toString();
		if (!map.containsKey(llocKey) || !map.containsKey(numberOfVulnerabilitiesKey)) {
			return false;
		}
		double lloc = Double.valueOf(map.get(llocKey));
		double numberOfVulnerabilities = Double.valueOf(map.get(numberOfVulnerabilitiesKey));
		
		results = Collections.singletonMap(VULNERABIITIES_PER_KLOC.toString(), Double.toString(numberOfVulnerabilities / lloc * 1000));
		return true;
		
	}

	@Override
	public Map<String, String> getResults() {
		return results;
	}

	@Override
	public Collection<String> getMetricKeys() {
		return Collections.singleton(VULNERABIITIES_PER_KLOC.toString());
	}

	@Override
	public Set<Class<? extends IMetricCalculator>> getDependencies() {
		Set<Class<? extends IMetricCalculator>> dependencies = new HashSet<Class<? extends IMetricCalculator>>();
		dependencies.add(CVEMetrics.class);
		dependencies.add(SourceMeterMetrics.class);
		return dependencies;
	}

	/**
	 * The keys of the relative metric calculator metrics
	 * 
	 * @author speldszus
	 *
	 */
	public enum MetricKeysImpl {
		VULNERABIITIES_PER_KLOC("VulnerabiitiesPerKLOC");

		private String value;

		private MetricKeysImpl(String value) {
			this.value = value;
		}

		@Override
		public String toString() {
			return value;
		}
	}
}
