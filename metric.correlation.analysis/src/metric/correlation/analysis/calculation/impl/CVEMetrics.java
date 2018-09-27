package metric.correlation.analysis.calculation.impl;

import java.util.HashMap;
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

}