package metric.correlation.analysis.calculation.impl;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.LinkedHashMap;
import java.util.List;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.IJavaProject;

import org.gravity.eclipse.exceptions.NoConverterRegisteredException;
import org.gravity.hulk.HulkAPI;
import org.gravity.hulk.HulkAPI.AntiPatternNames;
import org.gravity.hulk.antipatterngraph.HAnnotation;
import org.gravity.hulk.antipatterngraph.HMetric;
import org.gravity.hulk.antipatterngraph.antipattern.HBlobAntiPattern;
import org.gravity.hulk.antipatterngraph.metrics.HIGAMMetric;
import org.gravity.hulk.antipatterngraph.metrics.HIGATMetric;
import org.gravity.tgg.modisco.MoDiscoTGGActivator;
import org.gravity.typegraph.basic.TypeGraph;

import metric.correlation.analysis.calculation.IMetricCalculator;

public class HulkMetrics implements IMetricCalculator {

	private List<HAnnotation> hulk_results = null;
	private boolean hulk_ok = false;
	
	/**
	 * A constructor initializing the dependencies
	 */
	public HulkMetrics() {
		MoDiscoTGGActivator.getDefault();
	}
	
	@Override
	public boolean calculateMetric(IJavaProject project, String productName, String vendorName, String version) {

		try {
			hulk_results = HulkAPI.detect(project, new NullProgressMonitor(), AntiPatternNames.Blob, AntiPatternNames.IGAM,
					AntiPatternNames.IGAT);
		} catch (NoConverterRegisteredException e) {
			e.printStackTrace();
			return false;
		}
		hulk_ok = true;
		return true;
	}

	@Override
	public LinkedHashMap<String, Double> getResults() {

		LinkedHashMap<String, Double> metric_results = new LinkedHashMap<String, Double>();
		double igam = 0.0;
		double igat = 0.0;

		if (!hulk_ok) {
			metric_results.put("BLOB-Antipattern", -1.0);
			metric_results.put("IGAM", -1.0);
			metric_results.put("IGAT", -1.0);
			return metric_results;
		}
		double blob = 0.0;

		for (HAnnotation ha : hulk_results) {

			if (ha instanceof HBlobAntiPattern)
				blob++;

			if (ha instanceof HIGAMMetric) {
				if (ha.getTAnnotated() instanceof TypeGraph) {
					igam = ((HMetric) ha).getValue();
					System.out.println(igam);
				}
			}
			if (ha instanceof HIGATMetric) {
				if (ha.getTAnnotated() instanceof TypeGraph) {
					igat = ((HMetric) ha).getValue();
					System.out.println(igat);
				}
			}

		}
		metric_results.put("BLOB-Antipattern", blob);
		metric_results.put("IGAM", roundDouble(igam));
		metric_results.put("IGAT", roundDouble(igat));

		return metric_results;
	}

	private double roundDouble(double d) {
		DecimalFormatSymbols dfs = DecimalFormatSymbols.getInstance();
		dfs.setDecimalSeparator('.');
		DecimalFormat dFormat = new DecimalFormat("0.00", dfs);

		return Double.parseDouble(dFormat.format(d));
	}
}
