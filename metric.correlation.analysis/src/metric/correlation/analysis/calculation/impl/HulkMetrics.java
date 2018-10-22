package metric.correlation.analysis.calculation.impl;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.IJavaProject;

import org.gravity.hulk.HulkAPI;
import org.gravity.hulk.HulkAPI.AntiPatternNames;
import org.gravity.hulk.antipatterngraph.HAnnotation;
import org.gravity.hulk.antipatterngraph.HMetric;
import org.gravity.hulk.antipatterngraph.antipattern.HBlobAntiPattern;
import org.gravity.hulk.antipatterngraph.metrics.HIGAMMetric;
import org.gravity.hulk.antipatterngraph.metrics.HIGATMetric;
import org.gravity.hulk.exceptions.DetectionFailedException;
import org.gravity.tgg.modisco.MoDiscoTGGActivator;
import org.gravity.typegraph.basic.TypeGraph;

import metric.correlation.analysis.calculation.IMetricCalculator;

import static metric.correlation.analysis.calculation.impl.HulkMetrics.MetricKeysImpl.*;

public class HulkMetrics implements IMetricCalculator {

	private static final Logger LOGGER = Logger.getLogger(HulkMetrics.class);

	private List<HAnnotation> results = null;
	private boolean ok = false;

	/**
	 * A constructor initializing the dependencies
	 */
	public HulkMetrics() {
		MoDiscoTGGActivator.getDefault();
	}

	@Override
	public boolean calculateMetric(IJavaProject project, String productName, String vendorName, String version) {

		try {
			results = HulkAPI.detect(project, new NullProgressMonitor(), AntiPatternNames.Blob, AntiPatternNames.IGAM,
					AntiPatternNames.IGAT);
		} catch (DetectionFailedException e) {
			LOGGER.log(Level.ERROR, e.getMessage(), e);
			return false;
		}
		ok = true;
		return true;
	}

	@Override
	public LinkedHashMap<String, Double> getResults() {

		LinkedHashMap<String, Double> metrics = new LinkedHashMap<String, Double>();
		double igam = 0.0;
		double igat = 0.0;

		if (!ok) {
			throw new IllegalStateException("The metrics haven't been calculated successfully!");
		}
		double blob = 0.0;

		for (HAnnotation annoatation : results) {

			if (annoatation instanceof HBlobAntiPattern) {
				blob++;
			} else if (annoatation instanceof HIGAMMetric) {
				if (annoatation.getTAnnotated() instanceof TypeGraph) {
					igam = ((HMetric) annoatation).getValue();
					LOGGER.log(Level.INFO, "IGAM = " + igam);
				}
			} else if (annoatation instanceof HIGATMetric) {
				if (annoatation.getTAnnotated() instanceof TypeGraph) {
					igat = ((HMetric) annoatation).getValue();
					LOGGER.log(Level.INFO, "IGAT = " + igat);
				}
			}

		}
		metrics.put(BLOB.toString(), blob);
		metrics.put(IGAM.toString(), roundDouble(igam));
		metrics.put(IGAT.toString(), roundDouble(igat));

		return metrics;
	}

	private double roundDouble(double d) {
		DecimalFormatSymbols dfs = DecimalFormatSymbols.getInstance();
		dfs.setDecimalSeparator('.');
		DecimalFormat dFormat = new DecimalFormat("0.00", dfs);

		return Double.parseDouble(dFormat.format(d));
	}

	@Override
	public Collection<? extends String> getMetricKeys() {
		return Arrays.asList(BLOB.toString(), IGAM.toString(), IGAT.toString());
	}

	static enum MetricKeysImpl {
		BLOB("BLOB-Antipattern"), IGAM("IGAM"), IGAT("IGAT");

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
