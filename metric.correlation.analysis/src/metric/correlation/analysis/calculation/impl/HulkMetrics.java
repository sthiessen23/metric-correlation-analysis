package metric.correlation.analysis.calculation.impl;

import java.io.IOException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.emf.ecore.resource.Resource;
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
	public Set<Class<? extends IMetricCalculator>> getDependencies() {
		return Collections.emptySet();
	}

	@Override
	public boolean calculateMetric(IJavaProject project, String productName, String vendorName, String version,
			final Map<String, String> map) {
		try {
			cleanResults();
		} catch (IOException e) {
			LOGGER.log(Level.WARN, "Cleaning previous results failed: "+e.getMessage(), e);
		}
		try {
			results = HulkAPI.detect(project, new NullProgressMonitor(), AntiPatternNames.BLOB, AntiPatternNames.IGAM,
					AntiPatternNames.IGAT);
		} catch (DetectionFailedException e) {
			LOGGER.log(Level.ERROR, e.getMessage(), e);
			return false;
		}
		ok = true;
		return true;
	}

	private void cleanResults() throws IOException {
		if(results == null) {
			return;
		}
		Set<Resource> resources = new HashSet<>();
		for(HAnnotation metric : results) {
			resources.add(metric.eResource());
		}
		results.clear();
		results = null;
		for(Resource resource : resources) {
			resource.delete(Collections.EMPTY_MAP);
		}
		resources.clear();
	}

	@Override
	public LinkedHashMap<String, String> getResults() {

		LinkedHashMap<String, String> metrics = new LinkedHashMap<>();
		double igam = 0.0;
		double igat = 0.0;

		if (!ok) {
			throw new IllegalStateException("The metrics haven't been calculated successfully!");
		}
		double blob = 0.0;

		for (HAnnotation annoatation : results) {

			if (annoatation instanceof HBlobAntiPattern) {
				// We count all blobs
				blob++;
			} else if (annoatation.getTAnnotated() instanceof TypeGraph) {
				/*
				 * For all metrics that are not blobs we are only interested in the values for
				 * the whole program model
				 */
				if (annoatation instanceof HIGAMMetric) {
					igam = ((HMetric) annoatation).getValue();
					LOGGER.log(Level.INFO, "IGAM = " + igam);
				} else if (annoatation instanceof HIGATMetric) {
					igat = ((HMetric) annoatation).getValue();
					LOGGER.log(Level.INFO, "IGAT = " + igat);
				}
			}

		}
		metrics.put(BLOB.toString(), Double.toString(blob));
		metrics.put(IGAM.toString(), roundDouble(igam));
		metrics.put(IGAT.toString(), roundDouble(igat));

		return metrics;
	}

	private String roundDouble(double d) {
		DecimalFormatSymbols dfs = DecimalFormatSymbols.getInstance();
		dfs.setDecimalSeparator('.');
		DecimalFormat dFormat = new DecimalFormat("0.00", dfs);

		return dFormat.format(d);
	}

	@Override
	public Collection<String> getMetricKeys() {
		return Arrays.asList(MetricKeysImpl.values()).stream().map(Object::toString).collect(Collectors.toList());
	}

	/**
	 * The keys of the Hulk metrics
	 * 
	 * @author speldszus
	 *
	 */
	public enum MetricKeysImpl {
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
