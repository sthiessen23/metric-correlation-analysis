package metric.correlation.analysis.calculation.impl;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.stream.Collectors;

import org.eclipse.jdt.core.IJavaProject;
import metric.correlation.analysis.calculation.IMetricCalculator;
import metric.correlation.analysis.vulnerabilities.VulnerabilityDataQueryHandler;

public class CVEMetrics implements IMetricCalculator {

	private HashMap<String, Double> results;
	
	@Override
	public boolean calculateMetric(IJavaProject project, String productName, String vendorName, String version) {
		VulnerabilityDataQueryHandler VDQH = new VulnerabilityDataQueryHandler();
		results = VDQH.getMetrics(VDQH.getVulnerabilities(productName, vendorName, version, "TWO"));
		return !results.isEmpty();
	}

	@Override
	public HashMap<String, Double> getResults() {
		return results;
	}
	
	@Override
	public Collection<String> getMetricKeys() {
		return Arrays.asList(MetricKeysImpl.values()).stream().map(Object::toString).collect(Collectors.toList());
	}
	
	/**
	 * The keys of the CVE metrics
	 * 
	 * @author speldszus
	 *
	 */
	public static enum MetricKeysImpl {
		AVERAGE_CVSS3("AverageCVSS3"),
		AVERAGE_CVSS2("AverageCVSS2"),
		NUMBER_OF_VULNERABILITIES("NumberOfVulnerabilities");
		
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