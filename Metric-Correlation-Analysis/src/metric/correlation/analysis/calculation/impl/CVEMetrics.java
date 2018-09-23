package metric.correlation.analysis.calculation.impl;

import java.util.HashMap;
import org.eclipse.jdt.core.IJavaProject;
import metric.correlation.analysis.calculation.IMetricCalculator;
import metric.correlation.analysis.vulnerabilities.VulnerabilityDataQueryHandler;

public class CVEMetrics implements IMetricCalculator {

	@Override
	public boolean calculateMetric(IJavaProject project, String productName, String vendorName, String version) {
		// TODO: Sven, how do I catch the exception that elasticsearch is not running?
		VulnerabilityDataQueryHandler VDQH = new VulnerabilityDataQueryHandler();
		HashMap<String, Double> results = VDQH
				.getMetrics(VDQH.getVulnerabilities(productName, vendorName, version, "TWO"));
		return !results.isEmpty();
	}

	private HashMap<String, Double> getResults(String productName, String vendorName, String version) {
		VulnerabilityDataQueryHandler VDQH = new VulnerabilityDataQueryHandler();
		HashMap<String, Double> results = VDQH
				.getMetrics(VDQH.getVulnerabilities(productName, vendorName, version, "TWO"));
		return results;
	}
	
	//TODO: Sven, how do I implement this without the input?
	@Override
	public HashMap<String, Double> getResults() {
		// TODO Auto-generated method stub
		return null;
	}

}
